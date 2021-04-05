package me.gravitinos.perms.core.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * This is a utility lock that will let you lock loading per-id and returning already created CompletableFutures if a load on an id is already
 * happening. It will also let you lock saving per-id. During loading of some id loadLock calls will return the future provided with the first call to loadLock.
 * During saving both loadLock and other saveLock calls will block until the saving lock is given up. Load locking is not reentrant
 *
 * @param <T> Identifying type
 * @param <K> Future type
 */
public class SaveLoadLock<T, K> {

    @Getter
    @Setter
    @AllArgsConstructor
    public static class FutureCounterPair<T> {
        private int count;
        private CompletableFuture<T> future;
        private Thread owningThread;
    }

    public static class CounterThreadPair {
        public final AtomicInteger count = new AtomicInteger();
        public final AtomicInteger blockForeign = new AtomicInteger();
        public volatile Thread owningThread = null;

        public synchronized boolean shouldWait(boolean foreign) {
            return Thread.currentThread() != owningThread && count.get() != 0;
        }
    }

    private final Map<T, CompletableFuture<K>> results = new HashMap<>();
    private final Map<T, FutureCounterPair<Void>> saving = new HashMap<>();
    private final ReentrantLock resultLock = new ReentrantLock(true);
    private final Condition condition = resultLock.newCondition();
    private final PrioritizingSync operationAll = new PrioritizingSync(resultLock);

    /**
     * Lock loading for a specified id.
     * This is not reentrant.
     *
     * @param id           The id to lock
     * @param futureResult The future to store if locked successfully
     * @return null if locked successfully, a future else
     */
    public CompletableFuture<K> loadLock(T id, CompletableFuture<K> futureResult) {
        resultLock.lock();
        try {
            if (results.containsKey(id))
                return results.get(id);
            if (loadAllSupplier.requestFuture(id) != null)
                return loadAllSupplier.requestFuture(id);
            while (saving.containsKey(id) || operationAll.shouldWait()) {
                if (operationAll.shouldWait()) {
                    try {
                        operationAll.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    CompletableFuture<?> future = saving.get(id).getFuture();
                    resultLock.unlock();
                    future.join();
                    resultLock.lock();
                }
            }

            if (results.containsKey(id)) //Check again
                return results.get(id);
            if (loadAllSupplier.requestFuture(id) != null) //Pretty sure this will always be false but good to have it here anyways
                return loadAllSupplier.requestFuture(id);


            results.put(id, futureResult);
        } finally {

            if (resultLock.isHeldByCurrentThread())
                resultLock.unlock();
        }
        return null;
    }

    /**
     * Unlock loading for a specified id NOTE: this can be called by any thread and I'm too lazy to change that
     * This should not be used in a finally clause instead use exception handling
     *
     * @param id The id to unlock
     */
    public void loadUnlock(T id, K result) {
        CompletableFuture<K> future;
        resultLock.lock();
        try {
            future = results.remove(id);
        } finally {
            resultLock.unlock();
        }
        future.complete(result);
    }

    /**
     * Lock saving for a specified id. During a save lock any other calls to loadLock(id) or saveLock(id) will block until saveUnlock(id) is called
     * This method will block until both the load lock and save lock for this id are un-occupied
     *
     * @param id The id to lock
     */
    public void saveLock(T id) {
        resultLock.lock();
        try {
            while ((saving.containsKey(id) && !saving.get(id).getOwningThread().equals(Thread.currentThread())) || results.containsKey(id) || operationAll.shouldWait()) {
                if (operationAll.shouldWait()) {
                    try {
                        operationAll.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    CompletableFuture<?> future;
                    if (saving.containsKey(id)) future = saving.get(id).getFuture();
                    else future = results.get(id);
                    resultLock.unlock();
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    resultLock.lock();
                }
            }
            if (saving.containsKey(id)) {
                saving.get(id).setCount(saving.get(id).getCount() + 1);
            } else {
                saving.put(id, new FutureCounterPair<>(1, new CompletableFuture<>(), Thread.currentThread()));
            }
        } finally {
            if (resultLock.isHeldByCurrentThread())
                resultLock.unlock();
        }
    }

    /**
     * @param id The id to lock
     */
    public void saveUnlock(T id) {
        resultLock.lock();
        try {
            if (Thread.currentThread() == saving.get(id).getOwningThread()) {
                saving.get(id).setCount(saving.get(id).getCount() - 1);
                if (saving.get(id).getCount() <= 0) {
                    saving.remove(id).getFuture().complete(null);
                }
            }
        } finally {
            resultLock.unlock();
        }
    }

    public void saveAllLock() {
        resultLock.lock();
        try {
            PrioritizingSync.Waiter waiter = operationAll.reserve(0); //Reserve our spot

            while (saving.size() != 0 || results.size() != 0) { //Wait for saving/results to finish (no new ones can be created because of reservation)
                CompletableFuture<?> future;
                if (saving.size() != 0) {
                    future = saving.values().iterator().next().getFuture();
                } else {
                    future = results.values().iterator().next();
                }
                resultLock.unlock();
                future.join();
                resultLock.lock();
            }

            try {
                waiter.acquire(); //Acquire the lock, blocking until acquired
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            if (resultLock.isHeldByCurrentThread())
                resultLock.unlock();
        }
    }

    private class LoadAllSupplier {
        public Map<T, CompletableFuture<K>> storedFutures = new HashMap<>();
        public AtomicBoolean supplying = new AtomicBoolean(false);

        public CompletableFuture<K> requestFuture(T id) {
            if (!supplying.get())
                return null;
            if (storedFutures.containsKey(id))
                return storedFutures.get(id);
            CompletableFuture<K> future = new CompletableFuture<>();
            storedFutures.put(id, future);
            return future;
        }
    }

    private final LoadAllSupplier loadAllSupplier = new LoadAllSupplier();

    /**
     * Locks all loads and saves returning loads with the supplied future of the corresponding id
     */
    public void loadAllLock() {
        resultLock.lock();
        try {
            PrioritizingSync.Waiter waiter = operationAll.reserve(0); //Reserve our spot

            while (saving.size() != 0 || results.size() != 0) { //Wait for saving/results to finish (no new ones can be created because of reservation)
                CompletableFuture<?> future;
                if (saving.size() != 0) {
                    future = saving.values().iterator().next().getFuture();
                } else {
                    future = results.values().iterator().next();
                }
                resultLock.unlock();
                future.join();
                resultLock.lock();
            }

            try {
                waiter.acquire(); //Acquire the lock, blocking until acquired
                loadAllSupplier.supplying.set(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            if (resultLock.isHeldByCurrentThread())
                resultLock.unlock();
        }
    }

    public void saveAllUnlock() {
        resultLock.lock();
        try {
            operationAll.release();
        } finally {
            resultLock.unlock();
        }
    }

    public void loadAllUnlock(Function<T, K> resultSupplier) {
        resultLock.lock();
        try {
            loadAllSupplier.supplying.set(false);
            loadAllSupplier.storedFutures.forEach((id, future) -> future.complete(resultSupplier.apply(id)));
            loadAllSupplier.storedFutures.clear();
            operationAll.release();
        } finally {
            resultLock.unlock();
        }
    }

    public static void main(String[] args) {
        SaveLoadLock<String, Void> lock = new SaveLoadLock<>();
        for (int i = 0; i < 20; i++) {
            int finalI = i;
            new Thread(() -> {
                if (finalI % 4 == 0) {
                    CompletableFuture<Void> f = new CompletableFuture<>();
                    Future<Void> future = lock.loadLock("a", f);
                    if (future != null) {
                    } else {
                        lock.loadUnlock("a", null);
                    }
                } else if (finalI % 4 == 1) {
                    lock.saveLock("a");
                    lock.saveUnlock("a");
                } else if (finalI % 4 == 2) {
                    lock.saveAllLock();
                    lock.saveAllUnlock();
                } else {
                    lock.loadAllLock();
                    lock.loadAllUnlock((id) -> null);
                }
                System.out.println("Finished #" + finalI);
            }).start();
        }
    }
}

package me.gravitinos.perms.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public class FutureIDLock<T> {
    private Map<UUID, Future<T>> map = new HashMap<>();
    private Map<UUID, Integer> waitList = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock(true);

    private int getWaitNum(UUID key) {
        lock.lock();
        if (!waitList.containsKey(key)) {
            lock.unlock();
            return 0;
        }
        int res = waitList.get(key);
        lock.unlock();
        return res;
    }

    private void incrementWaitNum(UUID key) {
        lock.lock();
        waitList.put(key, getWaitNum(key) + 1);
        lock.unlock();
    }

    private int decrementWaitNum(UUID key) {
        lock.lock();
        waitList.put(key, getWaitNum(key) - 1);
        int now = getWaitNum(key);
        if (now <= 0)
            waitList.remove(key);
        lock.unlock();
        return now;
    }

    /**
     * Returns null if lock was acquired or if the lock is occupied, returns the associated future
     *
     * @param id     The ID
     * @param future The future to associate with the lock if no current lock is found
     */
    public Future<T> tryLock(UUID id, Future<T> future) {
        lock.lock();
        if (map.containsKey(id)) {
            Future<T> future1 = map.get(id);
            lock.unlock();
            return future1;
        }
        map.put(id, future);
        lock.unlock();
        return null;
    }

    public void lockOrWait(UUID id, Future<T> future) {
        lock.lock();
        if (map.containsKey(id)) {
            Future<T> future1 = map.get(id);
            map.put(id, future);
            incrementWaitNum(id);
            lock.unlock();
            try {
                future1.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return;
        }
        map.put(id, future);
        incrementWaitNum(id);
        lock.unlock();
    }

    public void unlock(UUID id) {
        lock.lock();
        int num = decrementWaitNum(id);
        if (num <= 0) {
            map.remove(id);
        }
        lock.unlock();
    }

    public boolean isLocked(UUID id) {
        lock.lock();
        boolean acquired = map.containsKey(id);
        lock.unlock();
        return acquired;
    }
}

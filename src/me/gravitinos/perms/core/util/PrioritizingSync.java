package me.gravitinos.perms.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Mainly for use in SaveLoadLock
 */
public class PrioritizingSync {

    public class Waiter {
        public final Condition condition;
        public final int priority;
        private final boolean reserver;

        public Waiter(Condition condition, int priority, boolean reserver) {
            this.condition = condition;
            this.priority = priority;
            this.reserver = reserver;
        }

        public void await() throws InterruptedException {
            condition.await();
        }

        public void acquire() throws InterruptedException {
            if (shouldWait()) {
                this.await();
            } else {
                waiterList.remove(this);
                reservedList.remove(this);
            }
            PrioritizingSync.this.acquire();
        }
    }

    private final List<Waiter> waiterList = new ArrayList<>();
    private final List<Waiter> reservedList = new ArrayList<>();
    private final AtomicInteger count = new AtomicInteger();
    private final AtomicReference<Thread> holder = new AtomicReference<>();

    private final ReentrantLock parentLock;

    public PrioritizingSync(ReentrantLock parentLock) {
        this.parentLock = parentLock;
    }

    public boolean shouldWait() {
        return holder.get() != Thread.currentThread() && count.get() != 0 && holder.get() != null || reservedList.size() != 0 && holder.get() != null;
    }


    public void acquire() {
        if (holder.compareAndSet(null, Thread.currentThread()))
            count.getAndIncrement();
    }

    public Waiter reserve(int priority) {
        Waiter waiter = new Waiter(parentLock.newCondition(), priority, true);
        reservedList.add(waiter);
        return waiter;
    }

    public void release() {
        if (holder.get() != Thread.currentThread())
            return;
        if (count.decrementAndGet() == 0) {
            holder.set(null);
            signal();
        }
    }

    private void await(int priority) throws InterruptedException {
        Waiter waiter = new Waiter(parentLock.newCondition(), priority, false);
        waiterList.add(waiter);
        waiter.condition.await();
    }

    public void await() throws InterruptedException {
        this.await(0);
    }

    private void signal() {

        //Find the next reserved spot and signal it
        //Or signal all the waiters
        if (reservedList.size() != 0) {
            reservedList.sort((t1, t2) -> t2.priority - t1.priority);
            reservedList.remove(0).condition.signal();
        } else if (waiterList.size() != 0) {
            waiterList.forEach(w -> w.condition.signalAll());
        }
    }

}

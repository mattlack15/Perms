package me.gravitinos.perms.core.subject;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class  ConcurrentLoggedList<T> {

    public static final int MOD_ADD = 1;
    public static final int MOD_REM = -1;

    @Getter
    private final ReentrantLock lock = new ReentrantLock(true);
    private final List<T> base = new ArrayList<>();
    private final Map<T, Integer> modifications = new HashMap<>();

    public List<T> get() {
        return new ArrayList<>(base);
    }

    public void clear() {
        lock.lock();
        try {
            base.forEach(b -> addModification(b, MOD_REM));
            base.clear();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return base.size();
        } finally {
            lock.unlock();
        }
    }


    public void clearAndReset() {
        lock.lock();
        try {
            clear();
            resetLog();
        } finally {
            lock.unlock();
        }
    }

    public void addAll(List<T> entries) {
        lock.lock();
        try {
            base.addAll(entries);
            entries.forEach(e -> addModification(e, MOD_ADD));
        } finally {
            lock.unlock();
        }
    }

    public void set(List<T> entryList, boolean recordModifications) {
        lock.lock();
        try {
            if(recordModifications)
                base.forEach(b -> addModification(b, MOD_REM));
            base.clear();
            base.addAll(entryList);
            if(recordModifications)
                entryList.forEach(e -> addModification(e, MOD_ADD));
        } finally {
            lock.unlock();
        }
    }

    public boolean weakAdd(T entry) {
        lock.lock();
        try {
            if (contains(entry))
                return false;
            base.add(entry);
            addModification(entry, MOD_ADD);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void add(T entry) {
        lock.lock();
        try {
            base.add(entry);
            addModification(entry, MOD_ADD);
        } finally {
            lock.unlock();
        }
    }

    public void remove(T entry) {
        lock.lock();
        try {
            if (base.remove(entry))
                addModification(entry, MOD_REM);
        } finally {
            lock.unlock();
        }
    }

    public void removeIf(Predicate<T> predicate) {
        lock.lock();
        try {
            this.base.removeIf((e) -> {
                boolean result = predicate.test(e);
                if (result) {
                    addModification(e, MOD_REM);
                }
                return result;
            });
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(T entry) {
        lock.lock();
        try {
            return base.contains(entry);
        } finally {
            lock.unlock();
        }
    }

    public Map<T, Integer> getModifications() {
        lock.lock();
        try {
            return new HashMap<>(modifications);
        } finally {
            lock.unlock();
        }
    }

    public ImmutableListLog<T> getAndResetModifications() {
        lock.lock();
        try {
            ImmutableListLog<T> log = new ImmutableListLog<>(modifications);
            modifications.clear();
            return log;
        } finally {
            lock.unlock();
        }
    }

    public void resetLog() {
        lock.lock();
        try {
            modifications.clear();
        } finally {
            lock.unlock();
        }
    }

    public List<T> getAdded() {
        return getModifications(true);
    }

    public List<T> getRemoved() {
        return getModifications(false);
    }

    public boolean isModified() {
        return this.modifications.size() != 0;
    }

    private List<T> getModifications(boolean added) {
        lock.lock();
        try {
            List<T> mods = new ArrayList<>();
            this.modifications.forEach((m, n) -> {
                if (n == MOD_ADD && added || n == MOD_REM && !added) {
                    mods.add(m);
                }
            });
            return mods;
        } finally {
            lock.unlock();
        }
    }

    //Must be called while locked
    private void addModification(T entry, int mod) {
        this.modifications.merge(entry, mod, (old, n) -> {
            int num = old + n;
            if (num == 0) return null;
            return (int) Math.signum(num);
        });
    }
}

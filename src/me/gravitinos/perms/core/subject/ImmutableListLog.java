package me.gravitinos.perms.core.subject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImmutableListLog<T> {
    private final Map<T, Integer> log;
    public ImmutableListLog (Map<T, Integer> log) {
        this.log = new ConcurrentHashMap<>(log);
    }

    public List<T> getAdded() {
        return getModifications(true);
    }

    public List<T> getRemoved() {
        return getModifications(false);
    }

    private List<T> getModifications(boolean added) {
            List<T> mods = new ArrayList<>();
            this.log.forEach((m, n) -> {
                if (n == ConcurrentLoggedList.MOD_ADD && added || n == ConcurrentLoggedList.MOD_REM && !added) {
                    mods.add(m);
                }
            });
            return mods;
    }
}

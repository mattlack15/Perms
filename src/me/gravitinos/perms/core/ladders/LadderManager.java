package me.gravitinos.perms.core.ladders;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.group.GroupManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LadderManager {
    public static LadderManager instance;

    private DataManager dataManager;
    private List<RankLadder> loadedLadders = new ArrayList<>();

    public LadderManager(@NotNull DataManager dataManager) {
        instance = this;
        this.dataManager = dataManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CompletableFuture<Boolean> loadLadders() {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            synchronized (this) {
                for(CompletableFuture<Boolean> futures : beingLoaded.values()) {
                    try {
                        futures.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                getDataManager().getRankLadders().thenAccept(ladders -> {
                    this.loadedLadders = ladders;
                    out.complete(true);
                });
            }
        });

        return out;
    }

    public boolean isLadderLoaded(UUID id) {
        for (RankLadder ladder : loadedLadders) {
            if (ladder.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public RankLadder getLadder(UUID id) {
        for (RankLadder ladder : loadedLadders) {
            if (ladder.getId().equals(id)) {
                return ladder;
            }
        }
        return null;
    }

    public void unloadLadder(UUID id) {
        loadedLadders.removeIf(ladder -> ladder.getId().equals(id));
    }

    private static Map<UUID, CompletableFuture<Boolean>> beingLoaded = new HashMap<>();

    public CompletableFuture<Boolean> loadLadder(UUID id) {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        getDataManager().getRankLadder(id).thenAccept(ladder -> {
            synchronized (this) {
                if(beingLoaded.containsKey(id)) {
                    try {
                        out.complete(beingLoaded.get(id).get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    beingLoaded.put(id, out);
                }
            }
            loadedLadders.add(ladder);
            out.complete(true);
        });
        return out;
    }
}

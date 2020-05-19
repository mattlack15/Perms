package me.gravitinos.perms.core.ladders;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.group.GroupManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
        List<UUID> successfullyLoaded = new ArrayList<>();
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            synchronized (GroupManager.class) {
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

    public CompletableFuture<Boolean> loadLadder(UUID id) {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        getDataManager().getRankLadder(id).thenAccept(ladder -> {
            loadedLadders.add(ladder);
            out.complete(true);
        });
        return out;
    }
}

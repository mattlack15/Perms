package me.gravitinos.perms.core.ladders;

import lombok.Getter;
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

    public CompletableFuture<Boolean> addLadder(RankLadder ladder){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if(isLadderLoaded(ladder.getId()) || getLadderFromName(ladder.getName()) != null){
            future.complete(false);
            return future;
        }

        this.loadedLadders.add(ladder);

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                dataManager.addRankLadder(ladder).get();
                future.complete(true);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                future.complete(false);
            }
        });
        return future;
    }

    public synchronized CompletableFuture<Void> removeLadder(UUID id){
        this.unloadLadder(id);
        return dataManager.removeRankLadder(id);
    }

    public RankLadder getLadderFromName(String name){
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        for(RankLadder ladder : getLoadedLadders()){
            if(caseSensitive && ladder.getName().equals(name) || !caseSensitive && ladder.getName().equalsIgnoreCase(name))
                return ladder;
        }
        return null;
    }

    public synchronized List<RankLadder> getLoadedLadders() {
        return new ArrayList<>(this.loadedLadders);
    }


    public CompletableFuture<Boolean> loadLadders() {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                try {
                    getDataManager().getRankLadders().thenAccept(ladders -> {
                        ladders.forEach(l -> l.setDataManager(this.dataManager));
                        synchronized (this) {
                            this.loadedLadders = ladders;
                        }
                        out.complete(true);
                    }).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
        });

        return out;
    }

    public boolean isLadderLoaded(UUID id) {
        for (RankLadder ladder : getLoadedLadders()) {
            if (ladder.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public RankLadder getLadder(UUID id) {
        for (RankLadder ladder : getLoadedLadders()) {
            if (ladder.getId().equals(id)) {
                return ladder;
            }
        }
        return null;
    }

    public synchronized void unloadLadder(UUID id) {
        loadedLadders.removeIf(ladder -> ladder.getId().equals(id));
    }

    public CompletableFuture<Boolean> loadLadder(UUID id) {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        getDataManager().getRankLadder(id).thenAccept(ladder -> {
            synchronized (this) {
                loadedLadders.add(ladder);
                out.complete(true);
            }
        });
        return out;
    }
}

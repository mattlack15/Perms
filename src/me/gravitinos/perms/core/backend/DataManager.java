package me.gravitinos.perms.core.backend;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.ladders.RankLadder;
import me.gravitinos.perms.core.subject.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public abstract class DataManager {

    private ThreadLocal<Boolean> keepSync = ThreadLocal.withInitial(() -> false);

    /**
     * Keeps all async operations that are started on the thread this is called from sync
     */
    protected void setKeepSync(boolean bool) {
        keepSync.set(bool);
    }

    protected boolean isKeepSync() {
        return keepSync.get();
    }

    public abstract CompletableFuture<Void> addSubject(Subject<?> subject);

    public abstract CompletableFuture<CachedSubject> getSubject(UUID subjectId);

    public abstract CompletableFuture<Void> updateSubject(Subject<?> subject);

    public abstract CompletableFuture<Void> removeSubject(UUID subjectId);

    public abstract CompletableFuture<Void> updateSubjectData(Subject<?> subject);

    public abstract CompletableFuture<GenericSubjectData> getSubjectData(UUID subjectId);

    public abstract CompletableFuture<Void> addSubjects(List<Subject<?>> subjects);


    //Queue

    private final ReentrantLock queueLock = new ReentrantLock(true);
    private final List<Subject<?>> queue = new ArrayList<>();

    public void queueUpdate(Subject<?> subject) {
        queueLock.lock();
        try {
            if(queue.contains(subject))
                return;
            queue.add(subject);
        } finally {
            queueLock.unlock();
        }
    }

    public CompletableFuture<Void> flushUpdateQueue() {
        queueLock.lock();
        try {

            CompletableFuture<Void> completed = new CompletableFuture<>();
            completed.complete(null);

            if(queue.isEmpty())
                return completed;

            List<Subject<?>> q = new ArrayList<>(queue);
            queue.clear();
            return addOrUpdateSubjects(q);
        } finally {
            queueLock.unlock();
        }
    }

    public abstract CompletableFuture<Void> addOrUpdateSubjects(List<Subject<?>> subjects);

    //Large Operations

    public abstract CompletableFuture<Void> removeSubjects(List<UUID> subjects);

    public abstract CompletableFuture<Void> removeInheritances(Subject<?> subject, List<UUID> parents);

    public abstract CompletableFuture<Void> addInheritances(List<Inheritance> inheritances);

    public abstract CompletableFuture<List<CachedSubject>> getAllSubjectsOfType(String type);

    public abstract CompletableFuture<Void> clearAllData();

    public abstract CompletableFuture<Void> clearSubjectsOfType(String type);

    //Rank Ladders
    public abstract CompletableFuture<List<RankLadder>> getRankLadders();

    public abstract CompletableFuture<RankLadder> getRankLadder(UUID id);

    public abstract CompletableFuture<Void> removeRankLadder(UUID id);

    public abstract CompletableFuture<Void> addRankLadder(RankLadder ladder);

    public abstract CompletableFuture<Void> updateRankLadder(RankLadder ladder);
    //

    public abstract CompletableFuture<Map<Integer, String>> getServerIndex();

    public abstract CompletableFuture<Void> putServerIndex(int serverId, String serverName);

    public abstract CompletableFuture<Void> removeServerIndex(int serverId);

    public abstract CompletableFuture<Boolean> testBackendConnection();

    public abstract boolean isRemote();

    //Shutdown
    public void shutdown(){};

    //Async execution

    protected <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {

        CompletableFuture<T> future = new CompletableFuture<>();

        if (keepSync.get()) {
            future.complete(supplier.get());
        } else {
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                try {
                    try {
                        future.complete(supplier.get());
                    } catch(Throwable throwable){
                        future.complete(null);
                        throwable.printStackTrace();
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
            });
        }

        return future;
    }

    public <T> CompletableFuture<T> performOrderedOpAsync(Supplier<T> op) {
        return runAsync(() -> {
            boolean before = this.isKeepSync();
            this.setKeepSync(true);
            T a = op.get();
            this.setKeepSync(before);
            return a;
        });
    }

}

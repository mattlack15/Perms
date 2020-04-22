package me.gravitinos.perms.core.backend;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

    public abstract CompletableFuture<Void> addSubject(Subject subject);

    public abstract CompletableFuture<CachedSubject> getSubject(UUID subjectId); //Change to SubjectId

    public abstract CompletableFuture<Void> updateSubject(Subject subject);

    public abstract CompletableFuture<Void> removeSubject(UUID subjectId);

    public abstract CompletableFuture<ImmutablePermissionList> getPermissions(UUID subjectId);

    public abstract CompletableFuture<Void> updatePermissions(Subject subject);

    public abstract CompletableFuture<Void> addPermission(Subject subject, PPermission permission);

    public abstract CompletableFuture<Void> removePermission(Subject subject, String permission);

    public abstract CompletableFuture<Void> removePermissionExact(Subject subject, String permission, UUID permIdentifier);

    public abstract CompletableFuture<ArrayList<CachedInheritance>> getInheritances(UUID subjectId);

    public abstract CompletableFuture<Void> updateInheritances(Subject subject);

    public abstract CompletableFuture<Void> addInheritance(Subject subject, Subject inheritance, Context context);

    public abstract CompletableFuture<Void> removeInheritance(Subject subject, UUID parent);

    public abstract CompletableFuture<Void> updateSubjectData(Subject subject);

    public abstract CompletableFuture<GenericSubjectData> getSubjectData(UUID subjectId);

    //Large Operations
    public abstract CompletableFuture<Void> addPermissions(Subject subject, ImmutablePermissionList list);

    public abstract CompletableFuture<Void> removePermissions(Subject subject, ArrayList<String> list);

    public abstract CompletableFuture<Void> removePermissionsExact(Subject subject, ArrayList<PPermission> list);

    public abstract CompletableFuture<Void> addSubjects(ArrayList<Subject> subjects);

    public abstract CompletableFuture<Void> removeSubjects(ArrayList<UUID> subjects);

    public abstract CompletableFuture<Void> removeInheritances(Subject subject, ArrayList<UUID> parents);

    public abstract CompletableFuture<Void> addInheritances(ArrayList<Inheritance> inheritances);

    public abstract CompletableFuture<ArrayList<CachedSubject>> getAllSubjectsOfType(String type);

    public abstract CompletableFuture<Void> clearAllData();

    public abstract CompletableFuture<Void> clearSubjectOfType(String type);

    public abstract CompletableFuture<Map<Integer, String>> getServerIndex();

    public abstract CompletableFuture<Void> putServerIndex(int serverId, String serverName);

    public abstract CompletableFuture<Void> removeServerIndex(int serverId);

    public abstract CompletableFuture<Boolean> testBackendConnection();

    //Async execution

    protected <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {

        CompletableFuture<T> future = new CompletableFuture<>();

        if (keepSync.get()) {
            future.complete(supplier.get());
        } else {
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                try {
                    future.complete(supplier.get());
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

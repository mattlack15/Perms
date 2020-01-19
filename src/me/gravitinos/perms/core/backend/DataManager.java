package me.gravitinos.perms.core.backend;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.subject.*;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class DataManager {

    public abstract CompletableFuture<Void> addSubject(Subject subject);

    public abstract CompletableFuture<CachedSubject> getSubject(String name);

    public abstract CompletableFuture<Void> updateSubject(Subject subject);

    public abstract CompletableFuture<Void> removeSubject(String name);

    public abstract CompletableFuture<ImmutablePermissionList> getPermissions(String name);

    public abstract CompletableFuture<Void> updatePermissions(Subject subject);

    public abstract CompletableFuture<Void> addPermission(Subject subject, PPermission permission);

    public abstract CompletableFuture<Void> removePermission(Subject subject, String permission);

    public abstract CompletableFuture<ArrayList<CachedInheritance>> getInheritances(String name);

    public abstract CompletableFuture<Void> updateInheritances(Subject subject);

    public abstract CompletableFuture<Void> addInheritance(Subject subject, Inheritance inheritance);

    public abstract CompletableFuture<Void> removeInheritance(Subject subject, String parent);

    public abstract CompletableFuture<Void> updateSubjectOption(Subject subject);

    //Large Operations

    public abstract CompletableFuture<Void> addPermissionList(ImmutablePermissionList list);

    public abstract CompletableFuture<Void> addSubjects(ArrayList<Subject> subjects);

    public abstract CompletableFuture<Void> addInheritances(ArrayList<Inheritance> inheritances);

    public abstract CompletableFuture<ArrayList<CachedSubject>> getAllSubjectsOfType(String type);

    //Async execution

    protected <T> CompletableFuture<T> runAsync(Supplier<T> supplier){

        CompletableFuture<T> future = new CompletableFuture<>();

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            future.complete(supplier.get());
        });

        return future;
    }

}

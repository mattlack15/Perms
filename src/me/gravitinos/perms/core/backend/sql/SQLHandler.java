package me.gravitinos.perms.core.backend.sql;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class SQLHandler extends DataManager {
    private DataSource dataSource;
    public SQLHandler(DataSource source){
        this.dataSource = source;
    }

    @Override
    public CompletableFuture<Void> addSubject(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Subject> getSubject(String name) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateSubject(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeSubject(String name) {
        return null;
    }

    @Override
    public CompletableFuture<ImmutablePermissionList> getPermissions(String name) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updatePermissions(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addPermission(Subject subject, PPermission permission) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removePermission(Subject subject, String permission) {
        return null;
    }

    @Override
    public CompletableFuture<ArrayList<Inheritance>> getInheritances(String name) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateInheritances(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addInheritance(Subject subject, Inheritance inheritance) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeInheritance(Subject subject, String parent) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateSubjectOption(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addPermissionList(ImmutablePermissionList list) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addSubjects(ArrayList<Subject> subjects) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addInheritances(ArrayList<Inheritance> inheritances) {
        return null;
    }
}

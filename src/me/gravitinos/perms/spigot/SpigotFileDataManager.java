package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class SpigotFileDataManager extends DataManager {
    File fil
    FileConfiguration groupsConfig = YamlConfiguration.loadConfiguration(file);

    @Override
    public CompletableFuture<Void> addSubject(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<CachedSubject> getSubject(String name) {
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
    public CompletableFuture<ArrayList<CachedInheritance>> getInheritances(String name) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateInheritances(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addInheritance(Subject subject, Subject inheritance, Context context) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeInheritance(String subjectIdentifier, String parent) {
        return null;
    }

    @Override
    public CompletableFuture<Void> updateSubjectData(Subject subject) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addPermissions(Subject subject, ImmutablePermissionList list) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removePermissions(Subject subject, ArrayList<String> list) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addSubjects(ArrayList<Subject> subjects) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeSubjects(ArrayList<String> subjects) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeInheritances(String subjectIdentifier, ArrayList<String> parents) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addInheritances(ArrayList<Inheritance> inheritances) {
        return null;
    }

    @Override
    public CompletableFuture<ArrayList<CachedSubject>> getAllSubjectsOfType(String type) {
        return null;
    }
}

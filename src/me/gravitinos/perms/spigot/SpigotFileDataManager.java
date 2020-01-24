package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.user.UserData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SpigotFileDataManager extends DataManager {
    private FileConfiguration groupsConfig = YamlConfiguration.loadConfiguration(Files.GROUPS_FILE);
    private FileConfiguration usersConfig = YamlConfiguration.loadConfiguration(Files.USERS_FILE);

    private static final String GROUP_SECTION = "groups";
    private static final String USER_SECTION = "users";

    private static final String USER_DATA_PREFIX = "prefix";
    private static final String USER_DATA_SUFFIX = "suffix";
    private static final String USER_DATA_DISPLAYGROUP = "display_group";
    private static final String USER_DATA_NOTES = "notes";
    private static final String USER_DATA_USERNAME = "username";
    private static final String USER_PERMISSIONS = "permissions";
    private static final String USER_INHERITANCE = "inheritances";

    private boolean saveGroupsConfig(){
        try {
            groupsConfig.save(Files.GROUPS_FILE);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public CompletableFuture<Void> addSubject(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            if(this.subjectExists(subject.getIdentifier())){
                try {
                    this.removeSubject(subject.getIdentifier()).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            if(subject.getType().equals(Subject.USER)){
                //User Data
                UserData data = new UserData(subject.getData());
                ConfigurationSection section = usersConfig.createSection(USER_SECTION + "." + subject.getIdentifier());
                section.set(USER_DATA_USERNAME, data.getName());
                section.set(USER_DATA_DISPLAYGROUP, data.getDisplayGroup());
                //TODO
            } else if(subject.getType().equals(Subject.GROUP)){

            } else {
                SpigotPerms.instance.getManager().getImplementation().addToLog("Tried to add subject of unknown type " + subject.getType());
            }
            return null;
        });
        return future;
    }

    private boolean subjectExists(String identifier){
        return this.groupsConfig.isConfigurationSection(GROUP_SECTION + "." + identifier) ||
                this.usersConfig.isConfigurationSection(USER_SECTION + "." + identifier);
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

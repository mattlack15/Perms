package me.gravitinos.perms.spigot;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.subject.*;
import me.gravitinos.perms.core.user.UserData;
import me.gravitinos.perms.core.util.MapUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SpigotFileDataManager extends DataManager {
    private FileConfiguration groupsConfig = YamlConfiguration.loadConfiguration(Files.GROUPS_FILE);
    private FileConfiguration usersConfig = YamlConfiguration.loadConfiguration(Files.USERS_FILE);

    private static final String GROUP_SECTION = "groups";
    private static final String USER_SECTION = "users";

    private static final String SUBJECT_PERMISSIONS = "permissions";
    private static final String SUBJECT_INHERITANCES = "inheritances";

    private static final String USER_DATA_PREFIX = "prefix";
    private static final String USER_DATA_SUFFIX = "suffix";
    private static final String USER_DATA_DISPLAYGROUP = "display_group";
    private static final String USER_DATA_NOTES = "notes";
    private static final String USER_DATA_USERNAME = "username";

    private static final String GROUP_DATA_PREFIX = "prefix";
    private static final String GROUP_DATA_SUFFIX = "suffix";
    private static final String GROUP_DATA_CHATCOLOUR = "chatcolour";
    private static final String GROUP_DATA_DESCRIPTION = "description";

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
                section.set(USER_DATA_NOTES, data.getNotes());
                section.set(USER_DATA_PREFIX, data.getPrefix());
                section.set(USER_DATA_SUFFIX, data.getSuffix());

                //Other Data
                this.addPermissions(subject, Subject.getPermissions(subject));
                this.addInheritances(Subject.getInheritances(subject));

            } else if(subject.getType().equals(Subject.GROUP)){
                //Group Data
                GroupData data = new GroupData(subject.getData());
                ConfigurationSection section = groupsConfig.createSection(GROUP_SECTION + "." + subject.getIdentifier());
                section.set(GROUP_DATA_PREFIX, data.getPrefix());
                section.set(GROUP_DATA_SUFFIX, data.getSuffix());
                section.set(GROUP_DATA_CHATCOLOUR, data.getChatColour());
                section.set(GROUP_DATA_DESCRIPTION, data.getDescription());

                //Other Data
                this.addPermissions(subject, Subject.getPermissions(subject));
                this.addInheritances(Subject.getInheritances(subject));
            } else {
                SpigotPerms.instance.getManager().getImplementation().addToLog("Tried to add subject of unknown type " + subject.getType());
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    private boolean subjectExists(String identifier){
        return this.groupsConfig.isConfigurationSection(GROUP_SECTION + "." + identifier) ||
                this.usersConfig.isConfigurationSection(USER_SECTION + "." + identifier);
    }

    private GenericSubjectData getSubjectData(String identifier){
        if(this.getSubjectType(identifier).equals(Subject.GROUP)){
            GroupData data = new GroupData();
            ConfigurationSection section = groupsConfig.getConfigurationSection(GROUP_SECTION + "." + identifier);
            if(section == null){
                return new GenericSubjectData();
            }
            data.setChatColour(section.getString(GROUP_DATA_CHATCOLOUR));
            data.setDescription(section.getString(GROUP_DATA_DESCRIPTION));
            data.setPrefix(section.getString(GROUP_DATA_PREFIX));
            data.setSuffix(section.getString(GROUP_DATA_SUFFIX));

            return new GenericSubjectData(data);
        } else {
            UserData data = new UserData();
            ConfigurationSection section = usersConfig.getConfigurationSection(USER_SECTION + "." + identifier);
            if(section == null){
                return new GenericSubjectData();
            }
            data.setDisplayGroup(section.getString(USER_DATA_DISPLAYGROUP));
            data.setName(section.getString(USER_DATA_USERNAME));
            data.setNotes(section.getString(USER_DATA_NOTES));
            data.setPrefix(section.getString(USER_DATA_PREFIX));
            data.setSuffix(section.getString(USER_DATA_SUFFIX));

            return new GenericSubjectData(data);
        }
    }

    private String getSubjectType(String identifier){
        if(this.groupsConfig.isConfigurationSection(GROUP_SECTION + "." + identifier)){
            return Subject.GROUP;
        } else if(this.usersConfig.isConfigurationSection(USER_SECTION + "." + identifier)){
            return Subject.USER;
        } else {
            return "GENERIC";
        }
    }

    @Override
    public CompletableFuture<CachedSubject> getSubject(String name) {
        CompletableFuture<CachedSubject> future = new CompletableFuture<>();
        runAsync(() -> {
            try {
                CachedSubject subject = new CachedSubject(name, this.getSubjectType(name), , this.getPermissions(name).get().getPermissions(), this.getInheritances(name).get());
                future.complete(subject);
                return null;
            }catch (Exception e){
                future.complete(null);
                return null;
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateSubject(Subject subject) {
        return this.addSubject(subject);
    }

    @Override
    public CompletableFuture<Void> removeSubject(String name) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            if(this.getSubjectType(name).equals(Subject.USER)){
                usersConfig.set(USER_SECTION + "." + name, null);
            } else {
                groupsConfig.set(GROUP_SECTION + "." + name, null);
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<ImmutablePermissionList> getPermissions(String name) {
        CompletableFuture<ImmutablePermissionList> future = new CompletableFuture<>();
        runAsync(() -> {
            ConfigurationSection section;
            if(this.getSubjectType(name).equals(Subject.USER)){
                section = groupsConfig.getConfigurationSection(USER_SECTION + "." + name);
            } else {
                section = groupsConfig.getConfigurationSection(GROUP_SECTION + "." + name);
            }
            if(section == null){
                future.complete(null);
                return null;
            }
            ArrayList<String> permStrings = Lists.newArrayList(section.getStringList(SUBJECT_PERMISSIONS));
            ArrayList<PPermission> perms = new ArrayList<>();

            for(String permString : permStrings){
                if(!permString.contains(" ")){
                    perms.add(new PPermission(permString));
                }
                Map<String, String> deserialized = MapUtil.stringToMap(permString.substring(permString.indexOf(" ")+1));
                Context context = Context.CONTEXT_ALL;
                Long expiration = 0L;
                if(deserialized.get("context") != null){
                    context = Context.fromString(deserialized.get("context"));
                }
                if(deserialized.get("expiration") != null){
                    try {
                        expiration = Long.parseLong(deserialized.get("expiration"));
                    }catch (Exception ignored){}
                }
                perms.add(new PPermission(permString.substring(0, permString.indexOf(" ")), context, expiration));
            }

            future.complete(new ImmutablePermissionList(perms));
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updatePermissions(Subject subject) {
        CompletableFuture future = new CompletableFuture<>();
        runAsync(() -> {

        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addPermission(Subject subject, PPermission permission) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            ConfigurationSection section;
            if(subject.getType().equals(Subject.USER)){
                section = groupsConfig.getConfigurationSection(USER_SECTION + "." + subject.getIdentifier());
            } else {
                section = groupsConfig.getConfigurationSection(GROUP_SECTION + "." + subject.getIdentifier());
            }
            if(section == null){
                future.complete(null);
                return null;
            }
            Map<String, String> contextExpirationMap = new HashMap<>();
            if(!Context.CONTEXT_ALL.equals(permission.getContext())) {
                contextExpirationMap.put("context", permission.getContext().toString());
            }
            if(permission.getExpiry() != 0) {
                contextExpirationMap.put("expiration", Long.toString(permission.getExpiry()));
            }
            String dataStr = contextExpirationMap.size() > 0 ? " " + MapUtil.mapToString(contextExpirationMap) : "";
            String permString = permission.getPermission() + dataStr;

            ArrayList<String> current = Lists.newArrayList(section.getStringList(SUBJECT_PERMISSIONS));
            current.add(permString);
            section.set(SUBJECT_PERMISSIONS, current);
            future.complete(null);
            return null;
        });
        return future;
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

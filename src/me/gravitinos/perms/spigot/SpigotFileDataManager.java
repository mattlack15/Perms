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
import sun.net.www.content.text.Generic;

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

    public CompletableFuture<GenericSubjectData> getSubjectData(String identifier){
        CompletableFuture<GenericSubjectData> future = new CompletableFuture<>();
        runAsync(() -> {
            if (this.getSubjectType(identifier).equals(Subject.GROUP)) {
                GroupData data = new GroupData();
                ConfigurationSection section = groupsConfig.getConfigurationSection(GROUP_SECTION + "." + identifier);
                if (section == null) {
                    return new GenericSubjectData();
                }
                data.setChatColour(section.getString(GROUP_DATA_CHATCOLOUR));
                data.setDescription(section.getString(GROUP_DATA_DESCRIPTION));
                data.setPrefix(section.getString(GROUP_DATA_PREFIX));
                data.setSuffix(section.getString(GROUP_DATA_SUFFIX));

                future.complete(new GenericSubjectData(data));
            } else {
                UserData data = new UserData();
                ConfigurationSection section = usersConfig.getConfigurationSection(USER_SECTION + "." + identifier);
                if (section == null) {
                    return new GenericSubjectData();
                }
                data.setDisplayGroup(section.getString(USER_DATA_DISPLAYGROUP));
                data.setName(section.getString(USER_DATA_USERNAME));
                data.setNotes(section.getString(USER_DATA_NOTES));
                data.setPrefix(section.getString(USER_DATA_PREFIX));
                data.setSuffix(section.getString(USER_DATA_SUFFIX));

                future.complete(new GenericSubjectData(data));
            }
            return null;
        });
        return future;
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
                CachedSubject subject = new CachedSubject(name, this.getSubjectType(name), this.getSubjectData(name).get(), this.getPermissions(name).get().getPermissions(), this.getInheritances(name).get());
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
            section.set(SUBJECT_PERMISSIONS, new ArrayList<String>());
            try {
                this.addPermissions(subject, Subject.getPermissions(subject)).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            future.complete(null);
            return null;
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
            ArrayList<String> current = Lists.newArrayList(section.getString(SUBJECT_PERMISSIONS));
            current.stream().forEach(s -> {
                if(s.equals(" ") && permission.equals(" ")){
                    current.remove(s);
                }
                String[] split = s.split(" ");
                if(split.length == 0){
                    return;
                }
                if(split[0].equals(permission)){
                    current.remove(s);
                }
            });
            section.set(SUBJECT_PERMISSIONS, current);
            future.complete(null);
            return null;
        });
        return future;
    }

    private ConfigurationSection getSection(String identifier){
        if(this.getSubjectType(identifier).equals(Subject.USER)){
            return usersConfig.getConfigurationSection(USER_SECTION + "." + identifier);
        } else {
            return groupsConfig.getConfigurationSection(GROUP_SECTION + "." + identifier);
        }
    }

    @Override
    public CompletableFuture<ArrayList<CachedInheritance>> getInheritances(String name) {
        CompletableFuture<ArrayList<CachedInheritance>> future = new CompletableFuture<>();
        runAsync(() -> {
            ConfigurationSection section = getSection(name);
            if(section == null){
                future.complete(null);
                return null;
            }

            String type = this.getSubjectType(name);

            String inheritanceType = Subject.GROUP;

            ArrayList<CachedInheritance> out = new ArrayList<>();

            for(String inheritanceString : section.getStringList(SUBJECT_INHERITANCES)){
                String inheritance;
                Context context = Context.CONTEXT_ALL;
                if(!inheritanceString.contains(" ")){
                    inheritance = inheritanceString;
                } else {
                    Map<String, String> deserialized = MapUtil.stringToMap(inheritanceString.substring(inheritanceString.indexOf(" ")+1));

                    if(deserialized.containsKey("context")){
                        context = Context.fromString(deserialized.get("context"));
                    }
                    inheritance = inheritanceString.substring(0, inheritanceString.indexOf(" "));
                }
                out.add(new CachedInheritance(name, inheritance, type, inheritanceType, context));
            }

            future.complete(out);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateInheritances(Subject subject) {
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
            section.set(SUBJECT_INHERITANCES, new ArrayList<String>());
            try {
                this.addInheritances(Subject.getInheritances(subject)).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addInheritance(Subject subject, Subject inheritance, Context context) {
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

            ArrayList<String> current = Lists.newArrayList(section.getString(SUBJECT_INHERITANCES));
            Map<String, String> contextMap = new HashMap<>();

            if(!Context.CONTEXT_ALL.equals(context)) {
                contextMap.put("context", context.toString());
            }

            String contextString = contextMap.size() > 0 ? " " + MapUtil.mapToString(contextMap) : "";
            String inheritanceString = inheritance.getIdentifier() + contextString;

            current.add(inheritanceString);

            section.set(SUBJECT_INHERITANCES, current);

            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeInheritance(Subject subject, String parent) {
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
            ArrayList<String> current = Lists.newArrayList(section.getString(SUBJECT_INHERITANCES));
            current.stream().forEach(s -> {
                if(s.equals(" ") && parent.equals(" ")){
                    current.remove(s);
                }
                String[] split = s.split(" ");
                if(split.length == 0){
                    return;
                }
                if(split[0].equals(parent)){
                    current.remove(s);
                }
            });
            section.set(SUBJECT_INHERITANCES, current);
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> updateSubjectData(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            if(subject.getType().equals(Subject.USER)){
                //User Data
                UserData data = new UserData(subject.getData());
                ConfigurationSection section = getSection(subject.getIdentifier());
                if(section == null){
                    future.complete(null);
                    return null;
                }
                section.set(USER_DATA_USERNAME, data.getName());
                section.set(USER_DATA_DISPLAYGROUP, data.getDisplayGroup());
                section.set(USER_DATA_NOTES, data.getNotes());
                section.set(USER_DATA_PREFIX, data.getPrefix());
                section.set(USER_DATA_SUFFIX, data.getSuffix());

            } else if(subject.getType().equals(Subject.GROUP)){
                //Group Data
                GroupData data = new GroupData(subject.getData());
                ConfigurationSection section = getSection(subject.getIdentifier());
                if(section == null){
                    future.complete(null);
                    return null;
                }
                section.set(GROUP_DATA_PREFIX, data.getPrefix());
                section.set(GROUP_DATA_SUFFIX, data.getSuffix());
                section.set(GROUP_DATA_CHATCOLOUR, data.getChatColour());
                section.set(GROUP_DATA_DESCRIPTION, data.getDescription());

            } else {
                SpigotPerms.instance.getManager().getImplementation().addToLog("Tried to update subject data of unknown type " + subject.getType());
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addPermissions(Subject subject, ImmutablePermissionList list) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            for(PPermission p : list){
                try {
                    this.addPermission(subject, p).get();
                } catch (InterruptedException | ExecutionException ignored) { }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removePermissions(Subject subject, ArrayList<String> list) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            for(String p : list){
                try {
                    this.removePermission(subject, p).get();
                } catch (InterruptedException | ExecutionException ignored) { }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addSubjects(ArrayList<Subject> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            for(Subject subject : subjects){
                try {
                    this.addSubject(subject).get();
                } catch (InterruptedException | ExecutionException ignored) { }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeSubjects(ArrayList<String> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            for(String subject : subjects){
                try {
                    this.removeSubject(subject).get();
                } catch (InterruptedException | ExecutionException ignored) { }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeInheritances(Subject subject, ArrayList<String> parents) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            for(String parent : parents){
                try {
                    this.removeInheritance(subject, parent).get();
                } catch (InterruptedException | ExecutionException ignored) { }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> addInheritances(ArrayList<Inheritance> inheritances) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(() -> {
            for(Inheritance inheritance : inheritances){
                try {
                    this.addInheritance(inheritance.getChild(), inheritance.getParent(), inheritance.getContext()).get();
                } catch (InterruptedException | ExecutionException ignored) { }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<ArrayList<CachedSubject>> getAllSubjectsOfType(String type) {
        CompletableFuture<ArrayList<CachedSubject>> future = new CompletableFuture<>();
        runAsync(() -> {
            ArrayList<CachedSubject> subjects = new ArrayList<>();
            if(Subject.USER.equals(type)){
                for(String keys : usersConfig.getConfigurationSection(USER_SECTION).getKeys(false)){
                    try {
                        subjects.add(this.getSubject(keys).get());
                    } catch (Exception ignored){}
                }
            } else if(Subject.GROUP.equals(type)){
                for(String keys : groupsConfig.getConfigurationSection(GROUP_SECTION).getKeys(false)){
                    try {
                        subjects.add(this.getSubject(keys).get());
                    } catch (Exception ignored){}
                }
            } else {
                future.complete(null);
                return null;
            }
            future.complete(subjects);
            return null;
        });
        return future;
    }
}

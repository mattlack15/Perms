package me.gravitinos.perms.spigot.file;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.ladders.RankLadder;
import me.gravitinos.perms.core.subject.*;
import me.gravitinos.perms.core.user.UserData;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.core.util.MapUtil;
import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Spigot data manager
 */
public class SpigotFileDataManager extends DataManager {
    private final FileConfiguration groupsConfig = YamlConfiguration.loadConfiguration(Files.GROUPS_FILE);
    private final FileConfiguration usersConfig = YamlConfiguration.loadConfiguration(Files.USERS_FILE);

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
    private static final String GROUP_DATA_ID = "group_id_dont_change";
    private static final String GROUP_DATA_SUFFIX = "suffix";
    private static final String GROUP_DATA_CHATCOLOUR = "chatcolour";
    private static final String GROUP_DATA_DESCRIPTION = "description";
    private static final String GROUP_DATA_PRIORITY = "priority";

    private volatile boolean saveGroups = false;
    private volatile boolean saveUsers = false;

//    @Override
//    protected <T> CompletableFuture<T> runAsync(Supplier<T> supplier){
//
//        CompletableFuture<T> future = new CompletableFuture<>();
//
//        future.complete(null);
//        supplier.get();
//
//        return future;
//    }

    private synchronized boolean saveGroupsConfig() {
        this.setSavingGroups(true);
        return true;
    }

    public SpigotFileDataManager() {
        if (!groupsConfig.isConfigurationSection(GROUP_SECTION)) {
            groupsConfig.createSection(GROUP_SECTION);
        }
        if (!usersConfig.isConfigurationSection(USER_SECTION)) {
            usersConfig.createSection(USER_SECTION);
        }
    }

    private synchronized void setSavingGroups(boolean value) {
        if (!saveGroups && value) {
            saveGroups = true;
            SpigotPerms.instance.getImpl().getAsyncExecutor().execute(() -> {
                try {
                    synchronized (SpigotFileDataManager.this) {
                        groupsConfig.save(Files.GROUPS_FILE);
                    }
                    setSavingGroups(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else if (!value) {
            saveGroups = false;
        }
    }

    private synchronized void setSavingUsers(boolean value) {
        if (!saveUsers && value) {
            saveUsers = true;
            SpigotPerms.instance.getImpl().getAsyncExecutor().execute(() -> {
                try {
                    synchronized (this) {
                        usersConfig.save(Files.USERS_FILE);
                        setSavingUsers(false);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else if (!value) {
            saveUsers = false;
        }
    }

    private synchronized boolean saveUsersConfig() {
        this.setSavingUsers(true);
        return true;
    }


    /**
     * Adds a subject to the files
     *
     * @param subject
     * @return
     */
    @Override
    public CompletableFuture<Void> addSubject(Subject subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            //If it already exists, remove it, then continue execution
            if (this.subjectExists(subject.getSubjectId())) {
                try {
                    this.removeSubject(subject.getSubjectId()).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            //What type is it? user or group?
            if (subject.getType().equals(Subject.USER)) {
                //User Data
                UserData data = new UserData(subject.getData());
                ConfigurationSection section = usersConfig.createSection(USER_SECTION + "." + subject.getSubjectId());
                section.set(USER_DATA_USERNAME, data.getName());
                section.set(USER_DATA_NOTES, data.getNotes());
                section.set(USER_DATA_PREFIX, data.getPrefix());
                section.set(USER_DATA_SUFFIX, data.getSuffix());

                //Other Data
                saveUsersConfig();
                this.addPermissions(subject, Subject.getPermissions(subject));
                this.addInheritances(Subject.getInheritances(subject));

            } else if (subject.getType().equals(Subject.GROUP)) {
                //Group Data
                GroupData data = new GroupData(subject.getData());
                ConfigurationSection section = groupsConfig.createSection(GROUP_SECTION + "." + subject.getName());
                section.set(GROUP_DATA_PREFIX, data.getPrefix());
                section.set(GROUP_DATA_SUFFIX, data.getSuffix());
                section.set(GROUP_DATA_CHATCOLOUR, data.getChatColour());
                section.set(GROUP_DATA_DESCRIPTION, data.getDescription());
                section.set(GROUP_DATA_PRIORITY, data.getPriority());
                section.set(GROUP_DATA_ID, subject.getSubjectId().toString());

                //Other Data
                saveGroupsConfig();
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

    private synchronized boolean subjectExists(UUID subjectId) {
        return getGroupName(subjectId) != null || usersConfig.isConfigurationSection(USER_SECTION + "." + subjectId);
    }

    public synchronized ConfigurationSection getGroupConfigSection(UUID subjectId) {
        for (String keys : groupsConfig.getConfigurationSection(GROUP_SECTION).getKeys(false)) {
            if (groupsConfig.isString(GROUP_SECTION + "." + keys + "." + GROUP_DATA_ID)) {
                UUID id = UUID.fromString(groupsConfig.getString(GROUP_SECTION + "." + keys + "." + GROUP_DATA_ID));
                if (id.equals(subjectId)) {
                    return groupsConfig.getConfigurationSection(GROUP_SECTION + "." + keys);
                }
            }
        }
        return null;
    }

    public synchronized String getGroupName(UUID subjectId) {
        for (String keys : groupsConfig.getConfigurationSection(GROUP_SECTION).getKeys(false)) {
            if (groupsConfig.isString(GROUP_SECTION + "." + keys + "." + GROUP_DATA_ID)) {
                UUID id = UUID.fromString(groupsConfig.getString(GROUP_SECTION + "." + keys + "." + GROUP_DATA_ID));
                if (id.equals(subjectId)) {
                    return keys;
                }
            }
        }
        return null;
    }

    public synchronized boolean checkConverterIdentifierToSubjectId() {
        for (String keys : groupsConfig.getConfigurationSection(GROUP_SECTION).getKeys(false)) {
            if (!groupsConfig.isString(GROUP_SECTION + "." + keys + "." + GROUP_DATA_ID)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean convertIdentifierToSubjectId() {
        for (String keys : groupsConfig.getConfigurationSection(GROUP_SECTION).getKeys(false)) {
            if (!groupsConfig.isString(GROUP_SECTION + "." + keys + "." + GROUP_DATA_ID)) {
                groupsConfig.set(GROUP_SECTION + "." + keys + "." + GROUP_DATA_ID, UUID.randomUUID().toString());
            }
        }
        return !checkConverterIdentifierToSubjectId();
    }

    @Override
    public CompletableFuture<GenericSubjectData> getSubjectData(UUID subjectId) {
        CompletableFuture<GenericSubjectData> future = new CompletableFuture<>();
        if (this.getSubjectType(subjectId).equals(Subject.GROUP)) {
            GroupData data = new GroupData();
            synchronized (this) {
                ConfigurationSection section = getGroupConfigSection(subjectId);
                if (section == null) {
                    future.complete(null);
                    return future;
                }
                data.setChatColour(section.getString(GROUP_DATA_CHATCOLOUR));
                data.setDescription(section.getString(GROUP_DATA_DESCRIPTION));
                data.setPrefix(section.getString(GROUP_DATA_PREFIX));
                data.setSuffix(section.getString(GROUP_DATA_SUFFIX));
                data.setPriority(section.getInt(GROUP_DATA_PRIORITY));
                data.setName(getGroupName(subjectId));
            }

            future.complete(new GenericSubjectData(data));
        } else {
            UserData data = new UserData();
            synchronized (this) {
                ConfigurationSection section = usersConfig.getConfigurationSection(USER_SECTION + "." + subjectId.toString());
                if (section == null) {
                    future.complete(null);
                    return future;
                }
                data.setName(section.getString(USER_DATA_USERNAME));
                data.setNotes(section.getString(USER_DATA_NOTES));
                data.setPrefix(section.getString(USER_DATA_PREFIX));
                data.setSuffix(section.getString(USER_DATA_SUFFIX));
            }

            future.complete(new GenericSubjectData(data));
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> addOrUpdateSubjects(List<Subject<?>> subjects) {
        subjects.forEach(s -> {
            s.getOwnLoggedInheritances().resetLog();
            s.getOwnLoggedPermissions().resetLog();
        });
        return addSubjects(subjects);
    }

    private synchronized String getSubjectType(UUID subjectId) {
        if (getGroupName(subjectId) != null) {
            return Subject.GROUP;
        } else if (this.usersConfig.isConfigurationSection(USER_SECTION + "." + subjectId.toString())) {
            return Subject.USER;
        } else {
            return "GENERIC";
        }
    }

    @Override
    public CompletableFuture<CachedSubject> getSubject(UUID name) {
        CompletableFuture<CachedSubject> future = new CompletableFuture<>();
        try {
            if (!this.subjectExists(name)) {
                future.complete(null);
                return future;
            }
            CachedSubject subject = new CachedSubject(name, this.getSubjectType(name), this.getSubjectData(name).get(), this.getPermissions(name).get().getPermissions(), this.getInheritances(name).get());


            if (subject.getData() == null) {
                future.complete(null);
                return future;
            }
            future.complete(subject);
            return future;
        } catch (Exception e) {
            e.printStackTrace();
            future.complete(null);
            return future;
        }
    }

    @Override
    public CompletableFuture<Void> updateSubject(Subject subject) {
        return this.addSubject(subject);
    }

    @Override
    public CompletableFuture<Void> removeSubject(UUID name) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            synchronized (this) {
                if (this.getSubjectType(name).equals(Subject.USER)) {
                    usersConfig.set(USER_SECTION + "." + name, null);
                    saveUsersConfig();
                } else {
                    groupsConfig.set(GROUP_SECTION + "." + getGroupName(name), null);
                    saveGroupsConfig();
                }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    public CompletableFuture<ImmutablePermissionList> getPermissions(UUID name) {
        CompletableFuture<ImmutablePermissionList> future = new CompletableFuture<>();
        ConfigurationSection section;
        synchronized (this) {
            if (this.getSubjectType(name).equals(Subject.USER)) {
                section = usersConfig.getConfigurationSection(USER_SECTION + "." + name);
            } else {
                section = getGroupConfigSection(name);
            }
        }
        if (section == null) {
            future.complete(null);
            return future;
        }
        List<PPermission> perms;
        synchronized (this) {
            List<String> permStrings = Lists.newArrayList(section.getStringList(SUBJECT_PERMISSIONS));
            perms = new ArrayList<>();

            for (String permString : permStrings) {
                if (!permString.contains(" ")) {
                    perms.add(new PPermission(permString));
                    continue;
                }
                Map<String, String> deserialized = MapUtil.stringToMap(permString.substring(permString.indexOf(" ") + 1));
                ContextSet context = new MutableContextSet();
                long expiration = ContextSet.NO_EXPIRATION;
                if (deserialized.get("context") != null) {
                    try {
                        context = ContextSet.fromString(deserialized.get("context"));
                    } catch (Exception e) {
                        System.out.println("Could not deserialize contextset");
                        context = new MutableContextSet();
                    }
                }
                if (deserialized.get("expiration") != null) {
                    try {
                        expiration = Long.parseLong(deserialized.get("expiration"));
                    } catch (Exception ignored) {
                    }
                }

                context.setExpiration(expiration);

                perms.add(new PPermission(permString.substring(0, permString.indexOf(" ")), context));
            }
        }

        future.complete(new ImmutablePermissionList(perms));
        return future;
    }

//    @Override
//    public CompletableFuture<Void> updatePermissions(Subject<?> subject) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            ConfigurationSection section;
//            if (subject.getType().equals(Subject.USER)) {
//                section = usersConfig.getConfigurationSection(USER_SECTION + "." + subject.getSubjectId());
//            } else {
//                section = getGroupConfigSection(subject.getSubjectId());
//            }
//            if (section == null) {
//                future.complete(null);
//                return null;
//            }
//            section.set(SUBJECT_PERMISSIONS, new ArrayList<String>());
//            try {
//                this.addPermissions(subject, Subject.getPermissions(subject)).get();
//            } catch (InterruptedException | ExecutionException e) {
//                e.printStackTrace();
//            }
//            future.complete(null);
//            return null;
//        });
//        return future;
//    }

    public CompletableFuture<Void> addPermission(Subject<?> subject, PPermission permission) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            ConfigurationSection section;
            synchronized (this) {
                if (subject.getType().equals(Subject.USER)) {
                    section = usersConfig.getConfigurationSection(USER_SECTION + "." + subject.getSubjectId());
                } else {
                    section = getGroupConfigSection(subject.getSubjectId());
                }
            }
            if (section == null) {
                future.complete(null);
                return null;
            }
            synchronized (this) {
                Map<String, String> contextExpirationMap = new HashMap<>();
                if (permission.getContext().size() != 0) {
                    contextExpirationMap.put("context", permission.getContext().toString());
                }
                if (permission.getExpiry() != 0) {
                    contextExpirationMap.put("expiration", Long.toString(permission.getExpiry()));
                }
                String dataStr = contextExpirationMap.size() > 0 ? " " + MapUtil.mapToString(contextExpirationMap) : "";
                String permString = permission.getPermission() + dataStr;

                List<String> current = Lists.newArrayList(section.getStringList(SUBJECT_PERMISSIONS));
                current.add(permString);
                section.set(SUBJECT_PERMISSIONS, current);

                saveUsersConfig();
                saveGroupsConfig();
            }

            future.complete(null);
            return null;
        });
        return future;
    }

//    @Override
//    public CompletableFuture<Void> removePermission(Subject<?> subject, String permission) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            ConfigurationSection section;
//            if (subject.getType().equals(Subject.USER)) {
//                section = usersConfig.getConfigurationSection(USER_SECTION + "." + subject.getSubjectId());
//            } else {
//                section = getGroupConfigSection(subject.getSubjectId());
//            }
//            if (section == null) {
//                future.complete(null);
//                return null;
//            }
//            List<String> current = Lists.newList(section.getStringList(SUBJECT_PERMISSIONS));
//            current.removeIf(s -> {
//                if (s.equals(" ") && permission.equals(" ")) {
//                    return true;
//                }
//                String[] split = s.split(" ");
//                if (split.length == 0) {
//                    return false;
//                }
//                return split[0].equals(permission);
//            });
//            section.set(SUBJECT_PERMISSIONS, current);
//
//            saveUsersConfig();
//            saveGroupsConfig();
//
//            future.complete(null);
//            return null;
//        });
//        return future;
//    }

//    @Override
//    public CompletableFuture<Void> removePermissionExact(Subject subject, PPermission permission) {
//        return this.removePermission(subject, permission.getPermission());
//    }


    private synchronized ConfigurationSection getSection(UUID id) {
        if (this.getSubjectType(id).equals(Subject.USER)) {
            return usersConfig.getConfigurationSection(USER_SECTION + "." + id);
        } else {
            return getGroupConfigSection(id);
        }
    }

    public CompletableFuture<List<CachedInheritance>> getInheritances(UUID name) {
        CompletableFuture<List<CachedInheritance>> future = new CompletableFuture<>();
        ConfigurationSection section = getSection(name);
        if (section == null) {
            future.complete(null);
            return future;
        }

        String type = this.getSubjectType(name);

        String inheritanceType = Subject.GROUP;

        List<CachedInheritance> out = new ArrayList<>();

        List<String> inheritance;
        synchronized (this) {
            inheritance = section.getStringList(SUBJECT_INHERITANCES);
        }
        for (String inheritanceString : inheritance) {
            String inheritanceName;
            ContextSet context = new MutableContextSet();
            if (!inheritanceString.contains(" ")) {
                inheritanceName = inheritanceString;
            } else {
                Map<String, String> deserialized = MapUtil.stringToMap(inheritanceString.substring(inheritanceString.indexOf(" ") + 1));

                if (deserialized.containsKey("context")) {
                    try {
                        context = ContextSet.fromString(deserialized.get("context"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                inheritanceName = inheritanceString.substring(0, inheritanceString.indexOf(" "));
            }

            Group group = GroupManager.instance.getVisibleGroup(inheritanceName);
            if (group != null)
                out.add(new CachedInheritance(name, group.getSubjectId(), type, inheritanceType, context));
        }

        future.complete(out);
        return future;
    }

//    @Override
//    public CompletableFuture<Void> updateInheritances(Subject<?> subject) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            ConfigurationSection section = getSection(subject.getSubjectId());
//            if (section == null) {
//                future.complete(null);
//                return null;
//            }
//            section.set(SUBJECT_INHERITANCES, new ArrayList<String>());
//            try {
//                this.addInheritances(Subject.getInheritances(subject)).get();
//            } catch (InterruptedException | ExecutionException e) {
//                e.printStackTrace();
//            }
//            future.complete(null);
//            return null;
//        });
//        return future;
//    }

    public CompletableFuture<Void> addInheritance(@NotNull CachedInheritance inheritance) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            ConfigurationSection section = getSection(inheritance.getChild());

            if (section == null) {
                future.complete(null);
                return null;
            }

            synchronized (this) {
                List<String> current = Lists.newArrayList(section.getStringList(SUBJECT_INHERITANCES));
                Map<String, String> contextMap = new HashMap<>();

                if (inheritance.getContext().size() != 0) {
                    contextMap.put("context", inheritance.getContext().toString());
                }

                String contextString = contextMap.size() > 0 ? " " + MapUtil.mapToString(contextMap) : "";
                String inheritanceString = (UserManager.instance.getUser(inheritance.getParent()) != null ? UserManager.instance.getUser(inheritance.getParent()).getName() : GroupManager.instance.getGroupExact(inheritance.getParent()).getName()) + contextString;
                current.add(inheritanceString);

                section.set(SUBJECT_INHERITANCES, current);
                saveUsersConfig();
                saveGroupsConfig();
            }

            future.complete(null);
            return null;
        });
        return future;
    }


//    @Override
//    public CompletableFuture<Void> removeInheritance(Subject<?> subject, UUID parent) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            ConfigurationSection section = getSection(subject.getSubjectId());
//
//            if (section == null || GroupManager.instance.getGroupExact(parent) == null) {
//                future.complete(null);
//                return null;
//            }
//            List<String> current = Lists.newList(section.getStringList(SUBJECT_INHERITANCES));
//            String editedParent = GroupManager.instance.getGroupExact(parent).getName();
//            current.removeIf(s -> {
//                if (s.equals(" ") && editedParent.equals(" ")) {
//                    return true;
//                }
//                String[] split = s.split(" ");
//                if (split.length == 0) {
//                    return false;
//                }
//                return split[0].equals(editedParent);
//            });
//            section.set(SUBJECT_INHERITANCES, current);
//            saveUsersConfig();
//            saveGroupsConfig();
//            future.complete(null);
//            return null;
//        });
//        return future;
//    }

    @Override
    public CompletableFuture<Void> updateSubjectData(Subject<?> subject) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            ConfigurationSection section = getSection(subject.getSubjectId());

            if (section == null) {
                future.complete(null);
                return null;
            }

            if (subject.getType().equals(Subject.USER)) {
                //User Data
                UserData data = new UserData(subject.getData());

                synchronized (this) {

                    section.set(USER_DATA_USERNAME, data.getName());
//                section.set(USER_DATA_DISPLAYGROUP, data.getDisplayGroup());
                    section.set(USER_DATA_NOTES, data.getNotes());
                    section.set(USER_DATA_PREFIX, data.getPrefix());
                    section.set(USER_DATA_SUFFIX, data.getSuffix());
                    saveUsersConfig();
                }
            } else if (subject.getType().equals(Subject.GROUP)) {
                //Group Data
                GroupData data = new GroupData(subject.getData());

                //If group was renamed
                if (!getGroupName(subject.getSubjectId()).equals(data.getName())) {
                    synchronized (this) {
                        groupsConfig.set(section.getCurrentPath(), null);
                    }
                    this.addSubject(subject);
                    future.complete(null);
                    return null;
                }

                synchronized (this) {
                    section.set(GROUP_DATA_PREFIX, data.getPrefix());
                    section.set(GROUP_DATA_SUFFIX, data.getSuffix());
                    section.set(GROUP_DATA_CHATCOLOUR, data.getChatColour());
                    section.set(GROUP_DATA_DESCRIPTION, data.getDescription());
                    section.set(GROUP_DATA_PRIORITY, data.getPriority());
                    saveGroupsConfig();
                }
            } else {
                SpigotPerms.instance.getManager().getImplementation().addToLog("Tried to update subject data of unknown type " + subject.getType());
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    public CompletableFuture<Void> addPermissions(Subject<?> subject, ImmutablePermissionList list) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            for (PPermission p : list) {
                try {
                    this.addPermission(subject, p).get();
                } catch (InterruptedException | ExecutionException ignored) {
                }
            }
            future.complete(null);
            return null;
        });
        return future;
    }
//
//    @Override
//    public CompletableFuture<Void> removePermissions(Subject<?> subject, List<String> list) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            for (String p : list) {
//                try {
//                    this.removePermission(subject, p).get();
//                } catch (InterruptedException | ExecutionException ignored) {
//                }
//            }
//            future.complete(null);
//            return null;
//        });
//        return future;
//    }
//
//    @Override
//    public CompletableFuture<Void> removePermissionsExact(Subject<?> subject, List<PPermission> list) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            for (PPermission p : list) {
//                try {
//                    this.removePermission(subject, p.getPermission()).get();
//                } catch (InterruptedException | ExecutionException ignored) {
//                }
//            }
//            future.complete(null);
//            return null;
//        });
//        return future;
//    }

    public CompletableFuture<Void> addSubjects(List<Subject<?>> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            for (Subject<?> subject : subjects) {
                try {
                    this.addSubject(subject).get();
                } catch (InterruptedException | ExecutionException ignored) {
                }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeSubjects(List<UUID> subjects) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            for (UUID subject : subjects) {
                try {
                    this.removeSubject(subject).get();
                } catch (InterruptedException | ExecutionException ignored) {
                }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> removeInheritances(Subject<?> subject, List<UUID> parents) {
        return null;
    }

//    @Override
//    public CompletableFuture<Void> removeInheritances(Subject<?> subject, List<UUID> parents) {
//        CompletableFuture<Void> future = new CompletableFuture<>();
//        runAsync(() -> {
//            for (UUID parent : parents) {
//                try {
//                    this.removeInheritance(subject, parent).get();
//                } catch (InterruptedException | ExecutionException ignored) {
//                }
//            }
//            future.complete(null);
//            return null;
//        });
//        return future;
//    }

    @Override
    public CompletableFuture<Void> addInheritances(List<Inheritance> inheritances) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            for (Inheritance inheritance : inheritances) {
                try {
                    this.addInheritance(inheritance.toCachedInheritance()).get();
                } catch (InterruptedException | ExecutionException ignored) {
                }
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<List<CachedSubject>> getAllSubjectsOfType(String type) {
        CompletableFuture<List<CachedSubject>> future = new CompletableFuture<>();
        queueAsync(() -> {
            List<CachedSubject> subjects = new ArrayList<>();
            if (Subject.USER.equals(type)) {
                synchronized (this) {
                    for (String keys : usersConfig.getConfigurationSection(USER_SECTION).getKeys(false)) {
                        try {
                            subjects.add(this.getSubject(UUID.fromString(keys)).get());
                        } catch (Exception ignored) {
                        }
                    }
                }
            } else if (Subject.GROUP.equals(type)) {
                synchronized (this) {
                    for (String keys : groupsConfig.getConfigurationSection(GROUP_SECTION).getKeys(false)) {
                        try {
                            subjects.add(this.getSubject(getGroupId(keys)).get());
                        } catch (Exception ignored) {
                        }
                    }
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

    public synchronized UUID getGroupId(String groupName) {
        ConfigurationSection section = groupsConfig.getConfigurationSection(GROUP_SECTION + "." + groupName);
        return UUID.fromString(section.getString(GROUP_DATA_ID));
    }

    @Override
    public CompletableFuture<Void> clearAllData() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            synchronized (this) {
                groupsConfig.set(GROUP_SECTION, null);
                groupsConfig.createSection(GROUP_SECTION);

                usersConfig.set(USER_SECTION, null);
                usersConfig.createSection(USER_SECTION);

                saveGroupsConfig();
                saveUsersConfig();

            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> clearSubjectsOfType(String type) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        queueAsync(() -> {
            if (Subject.USER.equals(type)) {
                synchronized (this) {
                    usersConfig.set(USER_SECTION, null);
                    usersConfig.createSection(USER_SECTION);
                    saveUsersConfig();
                }
            } else if (Subject.GROUP.equals(type)) {
                synchronized (this) {
                    groupsConfig.set(GROUP_SECTION, null);
                    groupsConfig.createSection(GROUP_SECTION);
                    saveGroupsConfig();
                }
            } else {
                future.complete(null);
                return null;
            }
            future.complete(null);
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<List<RankLadder>> getRankLadders() {
        return runAsync(ArrayList::new);
    }

    @Override
    public CompletableFuture<RankLadder> getRankLadder(UUID id) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeRankLadder(UUID id) {
        return runAsync(() -> null);
    }

    @Override
    public CompletableFuture<Void> addRankLadder(RankLadder ladder) {
        return runAsync(() -> null);
    }

    @Override
    public CompletableFuture<Void> updateRankLadder(RankLadder ladder) {
        return runAsync(() -> null);
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getServerIndex() {
        CompletableFuture<Map<Integer, String>> future = new CompletableFuture<>();
        future.complete(new HashMap<>());
        return future;
    }

    @Override
    public CompletableFuture<Void> putServerIndex(int serverId, String serverName) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    @Override
    public CompletableFuture<Void> removeServerIndex(int serverId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    @Override
    public CompletableFuture<Boolean> testBackendConnection() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);
        return future;
    }

    private List<Runnable> taskQueue = new ArrayList<>();
    private ReentrantLock taskQueueLock = new ReentrantLock();

    private <T> CompletableFuture<T> queueAsync(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        taskQueueLock.lock();
        try {
            taskQueue.add(() -> {
                T obj = task.get();
                future.complete(obj);
            });
        } finally {
            taskQueueLock.unlock();
        }
        return future;
    }

    private CompletableFuture<Void> queueAsync(Runnable runnable) {
        return queueAsync(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> flushUpdateQueue() {
        runAsync(() -> {
            taskQueueLock.lock();
            List<Runnable> list;
            try {
                list = new ArrayList<>(taskQueue);
                taskQueue.clear();
            } finally {
                taskQueueLock.unlock();
            }
            list.forEach(Runnable::run);
            return null;
        });
        return super.flushUpdateQueue();
    }

    @Override
    public boolean isRemote() {
        return false;
    }
}

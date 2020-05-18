package me.gravitinos.perms.core.group;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.subject.SubjectRef;
import me.gravitinos.perms.core.util.SubjectSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class GroupManager {

    public static GroupManager instance;

    private DataManager dataManager;
    private ArrayList<Group> loadedGroups = new ArrayList<>();

    public GroupManager(@NotNull DataManager dataManager) {
        instance = this;
        this.dataManager = dataManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    /**
     * Loads all the groups available from the Data Manager specified in constructor
     *
     * @return A future representing the completion of the load operation, true if successful, false if their was an error
     */
    public CompletableFuture<Boolean> loadGroups() {
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        ArrayList<UUID> successfullyLoaded = new ArrayList<>();
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            synchronized (GroupManager.class) {
                try {
                    ArrayList<CachedSubject> groups = dataManager.getAllSubjectsOfType(Subject.GROUP).get();
                    Map<UUID, SubjectRef> references = new HashMap<>();

                    //Create references
                    groups.forEach(cs -> references.put(cs.getSubjectId(), new SubjectRef(null)));

                    //Load and set references
                    groups.forEach(cs -> {
                        Group g;

                        boolean exists = this.isGroupExactLoaded(cs.getSubjectId());

                        if (exists) {
                            g = this.getGroupExact(cs.getSubjectId());
                            synchronized (this) {
                                g.updateFromCachedSubject(cs, references::get, false);
                            }
                        } else {
                            g = new Group(cs, references::get, this);
                        }
                        references.get(cs.getSubjectId()).setReference(g);
                        if (!exists) {
                            synchronized (this) {
                                this.loadedGroups.add(g); //Add groups to loadedGroups
                            }
                        }
                        successfullyLoaded.add(g.getSubjectId());
                    });

                    //Get rid of groups that aren't supposed to be loaded
                    for (Group group : Lists.newArrayList(getLoadedGroups())) {
                        if (!successfullyLoaded.contains(group.getSubjectId())) {
                            this.unloadGroup(group);
                        }
                    }

                    //Check for inheritance mistakes
                    this.eliminateInheritanceMistakes();

                } catch (Exception e) {
                    out.complete(false);
                    e.printStackTrace();
                }

                // Convert (Probably remove later)

//                for (Group group : this.loadedGroups) {
//                    Map<Integer, String> index = PermsManager.instance.getCachedServerIndex();
//
//                    group.getDataManager().performOrderedOpAsync(() -> {
//                        for (int ints : index.keySet()) {
//                            String name = index.get(ints);
//                            String finalServer = Integer.toString(ints);
//
//                            if (group.getContext().equals(name)) {
//                                group.setContext(finalServer);
//                                PermsManager.instance.getImplementation().sendDebugMessage("Found old-formatted group &e" + group.getName() + "&7 changing to new identifier-serverName format!");
//                            }
//
//                            //inheritance
//                            boolean a = false;
//                            for (Inheritance inheritances : group.getInheritances()) {
//                                if (inheritances.getContext().getServer().equals(name)) {
//                                    a = true;
//                                }
//                            }
//                            if (a) {
//                                PermsManager.instance.getImplementation().sendDebugMessage("Found old-formatted subject inheritances in &e" + group.getName() + "&7 changing to new identifier-serverName format!");
//                                ArrayList<Inheritance> inheritances = group.getInheritances();
//                                group.clearInheritances();
//                                inheritances.forEach(i -> {
//                                    if (i.getContext().getServer().equals(name)) {
//                                        Context.setContextServer(i.getContext(), finalServer);
//                                    }
//                                });
//                                group.addInheritances(inheritances);
//                            }
//
//
//                            //perms
//                            a = false;
//                            for (PPermission perms : group.getOwnPermissions()) {
//                                if (perms.getContext().getServer().equals(name)) {
//                                    a = true;
//                                }
//                            }
//                            if (a) {
//                                PermsManager.instance.getImplementation().sendDebugMessage("Found old-formatted permissions in &e" + group.getName() + "&7 changing to new identifier-serverName format!");
//                                ArrayList<PPermission> perms = group.getOwnPermissions().getPermissions();
//                                group.removeOwnPermissions(perms);
//                                ArrayList<PPermission> newPerms = new ArrayList<>();
//                                perms.forEach(p -> {
//                                    if (p.getContext().getServer().equals(name)) {
//                                        newPerms.add(new PPermission(p.getPermission(), new Context(finalServer, p.getContext().getWorldName(), p.getExpiry()), p.getPermissionIdentifier()));
//                                    } else {
//                                        newPerms.add(p);
//                                    }
//                                });
//                                group.addOwnPermissions(newPerms);
//                            }
//                        }
//                        return null;
//                    });
//
//                }
                //
                out.complete(true);
            }
        });

        return out;
    }

    public synchronized void eliminateInheritanceMistakes() {
        ArrayList<Subject> grps = new ArrayList<>(this.loadedGroups);
        Subject.checkForAndRemoveInheritanceMistakes(grps);
        this.loadedGroups.clear();
        grps.forEach(s -> this.loadedGroups.add((Group) s));
    }

    public CompletableFuture<Void> saveTo(DataManager dataManager) {
        ArrayList<Subject> groups = Lists.newArrayList(loadedGroups);
        return dataManager.addSubjects(groups);
    }

    /**
     * Gets a group that is loaded within this group manager <br>
     * This will find a group in which the provided query context is satisfied (or an exact match if specified) by the group's context
     * NOTE: Finds the FIRST group that matches the query
     *
     * @param name              The name of the group to look for
     * @param exactContextMatch If true, then will return a group with the exact context provided
     * @return The group or null if the group is not contained within this group manager
     */
    private synchronized Group findGroup(String name, ContextSet contexts, boolean groupContextSatisfied, boolean exactContextMatch) {
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        for (Group g : loadedGroups) {
            if (caseSensitive && g.getName().equals(name) || !caseSensitive && g.getName().equalsIgnoreCase(name)) {
                if (exactContextMatch) {
                    if(!g.getContext().equals(contexts))
                        continue;
                } else if (!(groupContextSatisfied && g.getContext().isSatisfiedBy(contexts) || !groupContextSatisfied && contexts.isSatisfiedBy(g.getContext()))) {
                    continue;
                }
                return g;
            }
        }
        return null;
    }

    /**
     * Get groups that match a certain condition
     */
    public synchronized List<Group> getGroups(Predicate<Group> condition){
        List<Group> list = new ArrayList<>();
        this.getLoadedGroups().forEach(g -> {
            if(condition.test(g))
                list.add(g);
        });
        return list;
    }


    /**
     * Gets a local group with some name
     *
     * @param name Name
     * @return Group
     */
    public synchronized Group getGroupLocal(String name) {
        return this.findGroup(name, new MutableContextSet(Context.CONTEXT_SERVER_LOCAL), true, true);
    }

    /**
     * Get a group by name which is valid on a certain provided server
     */
    public synchronized Group getGroupOfServer(String name, int serverId) {
        return this.findGroup(name, new MutableContextSet(new Context(Context.SERVER_IDENTIFIER, Integer.toString(serverId))), false, false);
    }

    /**
     * Gets a global group with some name
     *
     * @param name Name
     * @return Group
     */
    public synchronized Group getGroupGlobal(String name) {
        return this.findGroup(name, new MutableContextSet(), true, true);
    }

    /**
     * Gets a group with some name that is visible to this server
     *
     * @param name The name of the group
     * @return The group
     */
    public synchronized Group getVisibleGroup(String name) {
        Group g = this.getGroupLocal(name);
        if (g == null) {
            g = this.getGroupGlobal(name);
        }
        return g;
    }

    /**
     * Gets a group with some exact identifier (identifier is slightly different to name with groups, it contains the server context as well)
     *
     * @param groupId The group's subject id
     * @return The group
     */
    public synchronized Group getGroupExact(UUID groupId) {
        if (!this.isGroupExactLoaded(groupId)) {
            try {
                this.loadGroup(groupId, (id) -> new SubjectRef(this.getGroupExact(id)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Group g : loadedGroups) {
            if (g.getSubjectId().equals(groupId)) {
                return g;
            }
        }
        return null;
    }

    /**
     * Get all loaded groups
     *
     * @return all loaded groups
     */
    public synchronized ArrayList<Group> getLoadedGroups() {
        this.loadedGroups.sort(Comparator.comparingInt(Group::getPriority));
        return Lists.newArrayList(this.loadedGroups);
    }

    /**
     * Reloads all the loaded groups
     *
     * @return A future
     */
    public synchronized CompletableFuture<Boolean> reloadGroups() {
        return this.loadGroups();
    }


    /**
     * Deletes a group
     *
     * @param group The group to delete
     */
    public synchronized CompletableFuture<Void> removeGroup(Group group) {
        if (group == null) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.complete(null);
            return f;
        }
        this.unloadGroup(group);
        return dataManager.removeSubject(group.getSubjectId());
    }

    /**
     * Adds a group to this group manager and saves it to the current data manager
     *
     * @param group The group to add
     * @return True if the operation was successful and the group was added, or false if the group is already contained within this group manager
     */
    public synchronized boolean addGroup(Group group) {
        if (this.canGroupContextCollideWithAnyLoaded(group.getName(), group.getContext()) || this.isGlobalGroupLoaded(group.getName())
                || this.isGroupExactLoaded(group.getSubjectId())) {
            return false;
        }

        this.loadedGroups.add(group);
        this.dataManager.addSubject(group); //Add the subject to the backend

        this.eliminateInheritanceMistakes();

        return true;
    }

    private static final Map<UUID, CompletableFuture<Boolean>> beingLoaded = new HashMap<>();

    /**
     * Loads a group
     *
     * @param groupId  The group's Subject Id
     * @param supplier The inheritance supplier (Usually it is (s) -> GroupManager.instance.getGroupExact(s))
     * @return a Future
     */
    public synchronized CompletableFuture<Boolean> loadGroup(@NotNull UUID groupId, @NotNull SubjectSupplier supplier) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        synchronized (beingLoaded) {
            if (beingLoaded.containsKey(groupId) && !beingLoaded.get(groupId).isDone()) {
                return beingLoaded.get(groupId);
            } else {
                beingLoaded.remove(groupId);
                beingLoaded.put(groupId, future);
            }
        }

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            try {

                boolean exists = this.isGroupExactLoaded(groupId);

                CachedSubject cachedSubject = dataManager.getSubject(groupId).get();
                if (cachedSubject == null || cachedSubject.getSubjectId() == null) {
                    future.complete(false);
                    return;
                }
                if (!exists) {
                    Group g = new Group(cachedSubject, supplier, this);
                    this.loadedGroups.add(g);
                } else {
                    this.getGroupExact(groupId).updateFromCachedSubject(cachedSubject, supplier);
                }
                this.eliminateInheritanceMistakes();
                future.complete(true);
                beingLoaded.remove(groupId);
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(false);
                beingLoaded.remove(groupId);
            }
        });

        return future;
    }

    /**
     * Unloads a group
     *
     * @param group The group to unload
     */
    public void unloadGroup(Group group) {
        this.loadedGroups.remove(group);
    }

    /**
     * Checks if the specified group is loaded in this group manager <br>
     * Checks for EXACT MATCH for context
     *
     * @param name                  The group name to check
     * @return True if the group is loaded in this group manager, false otherwise
     */
    public synchronized boolean isGroupLoaded(String name, ContextSet contexts) {
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        for (Group g : loadedGroups) {
            if (caseSensitive && g.getName().equals(name) || !caseSensitive && g.getName().equalsIgnoreCase(name)) {
                if (g.getContext().equals(contexts))
                    continue;
                return true;
            }
        }
        return false;
    }

    public synchronized boolean canGroupContextCollideWithAnyLoaded(String name, ContextSet contexts, ContextSet... exclusions) {
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        List<ContextSet> exclusion = Arrays.asList(exclusions);
        for (Group g : getLoadedGroups()) {
            if(exclusion.contains(g.getContext()))
                continue;
            if (caseSensitive && g.getName().equals(name) || !caseSensitive && g.getName().equalsIgnoreCase(name)) {
                if (g.getContext().canCollide(contexts)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a local group with some name is loaded
     *
     * @param name
     * @return
     */
    public boolean isLocalGroupLoaded(String name) {
        return this.isGroupLoaded(name, new MutableContextSet(Context.CONTEXT_SERVER_LOCAL));
    }

    /**
     * Checks if a global group with some name is loaded
     *
     * @param name
     * @return
     */
    public boolean isGlobalGroupLoaded(String name) {
        return this.isGroupLoaded(name, new MutableContextSet());
    }

    /**
     * Checks if a visible group with some name is loaded
     *
     * @param name
     * @return
     */
    public boolean isVisibleGroupLoaded(String name) {
        return this.isGlobalGroupLoaded(name) || this.isLocalGroupLoaded(name);
    }

    public boolean isGroupExactLoaded(UUID groupId) {
        for (Group g : loadedGroups) {
            if (g.getSubjectId().equals(groupId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a new default group
     *
     * @return a new default group
     */
    private Group getNewDefaultGroup() {
        return new GroupBuilder("default").setPrefix("[&7Default&f] ").setDescription("The default group")
                .addPermission(new PPermission("modifyworld.*")).build(); //Server Context is by default local
    }

    public Group getDefaultGroup() {
        String groupName = PermsManager.instance.getImplementation().getConfigSettings().getDefaultGroup();
        Group g = this.getVisibleGroup(groupName);
        if (g == null) {
            g = this.getNewDefaultGroup();
            this.addGroup(g);
            PermsManager.instance.getImplementation().getConfigSettings().setDefaultGroup(g.getName());
            PermsManager.instance.getImplementation().addToLog("Unable to find default group, a new default group was created!");
        }
        return g;
    }
}

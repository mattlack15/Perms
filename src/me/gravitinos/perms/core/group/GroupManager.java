package me.gravitinos.perms.core.group;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.subject.SubjectData;
import me.gravitinos.perms.core.subject.SubjectRef;
import me.gravitinos.perms.core.util.SubjectSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GroupManager {

    public static GroupManager instance;

    private DataManager dataManager;
    private ArrayList<Group> loadedGroups = new ArrayList<>();
    public GroupManager(@NotNull DataManager dataManager){
        instance = this;
        this.dataManager = dataManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    /**
     * Loads all the groups available from the Data Manager specified in constructor
     * @return A future representing the completion of the load operation, true if successful, false if their was an error
     */
    public CompletableFuture<Boolean> loadGroups(){
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                ArrayList<CachedSubject> groups = dataManager.getAllSubjectsOfType(Subject.GROUP).get();
                Map<String, SubjectRef> references = new HashMap<>();

                //Create references
                groups.forEach(cs -> references.put(cs.getIdentifier(), new SubjectRef(null)));

                //Load and set references
                groups.forEach(cs -> {
                    Group g = new Group(cs, references::get, this);
                    if(!this.isGroupExactLoaded(g.getIdentifier())){
                        this.loadedGroups.add(g); //Add groups to loadedGroups
                    }
                    references.get(cs.getIdentifier()).setReference(g);
                });

                //Check for inheritance mistakes
                this.eliminateInheritanceMistakes();

            } catch (Exception e) {
                out.complete(false);
                e.printStackTrace();
            }
            out.complete(true);
        });
        return out;
    }

    private void eliminateInheritanceMistakes(){
        ArrayList<Subject> grps = new ArrayList<>(this.loadedGroups);
        Subject.checkForAndRemoveInheritanceMistakes(grps);
        this.loadedGroups.clear();
        grps.forEach(s -> this.loadedGroups.add((Group) s));
    }

    public CompletableFuture<Void> saveTo(DataManager dataManager){
        ArrayList<Subject> groups = Lists.newArrayList(loadedGroups);
        return dataManager.addSubjects(groups);
    }

    /**
     * Gets a group that is loaded within this group manager
     * @param name The name of the group to look for
     * @param server The server of the group to look in
     * @return The group or null if the group is not contained within this group manager
     */
    public Group getGroup(String name, String server){
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        for(Group g : loadedGroups){
            if(caseSensitive && g.getName().equals(name) || !caseSensitive && g.getName().equalsIgnoreCase(name)){
                if(!g.getServerContext().equals(server)){ //Does about the same thing as using getIdentifier() and adding the server instead of getName()
                    continue;
                }
                return g;
            }
        }
        return null;
    }

    /**
     * Gets a local group with some name
     * @param name Name
     * @return Group
     */
    public Group getGroupLocal(String name){
        return this.getGroup(name, GroupData.SERVER_LOCAL);
    }

    /**
     * Gets a global group with some name
     * @param name Name
     * @return Group
     */
    public Group getGroupGlobal(String name){
        return this.getGroup(name, GroupData.SERVER_GLOBAL);
    }

    /**
     * Gets a group with some name that is visible to this server
     * @param name The name of the group
     * @return The group
     */
    public Group getVisibleGroup(String name){
        Group g = this.getGroupLocal(name);
        if(g == null){
            g = this.getGroupGlobal(name);
        }
        return g;
    }

    /**
     * Gets a group with some exact identifier (identifier is slightly different to name with groups, it contains the server context as well)
     * @param identifier The group identifier
     * @return The group
     */
    public Group getGroupExact(String identifier){
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        if(!this.isGroupExactLoaded(identifier)){
            try {
                this.loadGroup(identifier, (s) -> new SubjectRef(this.getGroupExact(identifier)));
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        for(Group g : loadedGroups){
            if(caseSensitive && g.getIdentifier().equals(identifier) || !caseSensitive && g.getIdentifier().equalsIgnoreCase(identifier)){
                return g;
            }
        }
        return null;
    }

    /**
     * Get all loaded groups
     * @return all loaded groups
     */
    public ArrayList<Group> getLoadedGroups(){
        return this.loadedGroups;
    }

    /**
     * Reloads all the loaded groups
     * @return A future
     */
    public CompletableFuture<Boolean> reloadGroups(){
        ((ArrayList<Group>)this.loadedGroups.clone()).forEach(this::unloadGroup);
        return this.loadGroups();
    }

    /**
     * Deletes a group
     * @param group The group to delete
     */
    public CompletableFuture<Void> removeGroup(Group group){
        if(group == null){
            CompletableFuture<Void> f =  new CompletableFuture<>();
            f.complete(null);
            return f;
        }
        this.unloadGroup(group);
        return dataManager.removeSubject(group.getIdentifier());
    }

    /**
     * Adds a group to this group manager and saves it to the current data manager
     * @param group The group to add
     * @return True if the operation was successful and the group was added, or false if the group is already contained within this group manager
     */
    public boolean addGroup(Group group){
        if(this.isGroupLoaded(group.getName(), group.getServerContext()) || this.isGlobalGroupLoaded(group.getName())){
            return false;
        }

        this.loadedGroups.add(group);
        this.dataManager.addSubject(group); //Add the subject to the backend

        this.eliminateInheritanceMistakes();

        return true;
    }

    /**
     * Loads a group
     * @param groupIdentifier The identifier of
     * @param supplier The inheritance supplier (Usually it is (s) -> GroupManager.instance.getGroupExact(s))
     * @return a Future
     */
    public CompletableFuture<Boolean> loadGroup(@NotNull String groupIdentifier, @NotNull SubjectSupplier supplier){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if(this.isGroupExactLoaded(groupIdentifier)){
            future.complete(false);
            return future;
        }

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                CachedSubject cachedSubject = dataManager.getSubject(groupIdentifier).get();
                Group g = new Group(cachedSubject, supplier, this);
                this.loadedGroups.add(g);
                this.eliminateInheritanceMistakes();
                future.complete(true);
            }catch (Exception e){
                e.printStackTrace();
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * Unloads a group
     * @param group The group to unload
     */
    public void unloadGroup(Group group){
        this.loadedGroups.remove(group);
    }

    /**
     * Checks if the specified group is loaded in this group manager
     * @param name The group name to check
     * @param server The server context to check
     * @return True if the group is loaded in this group manager, false otherwise
     */
    public boolean isGroupLoaded(String name, String server){
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        for(Group g : loadedGroups){
            if(caseSensitive && g.getName().equals(name) || !caseSensitive && g.getName().equalsIgnoreCase(name)){
                if(!g.getServerContext().equals(server)){
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a local group with some name is loaded
     * @param name
     * @return
     */
    public boolean isLocalGroupLoaded(String name){
        return this.isGroupLoaded(name, GroupData.SERVER_LOCAL);
    }

    /**
     * Checks if a global group with some name is loaded
     * @param name
     * @return
     */
    public boolean isGlobalGroupLoaded(String name){
        return this.isGroupLoaded(name, GroupData.SERVER_GLOBAL);
    }

    /**
     * Checks if a visible group with some name is loaded
     * @param name
     * @return
     */
    public boolean isVisibleGroupLoaded(String name){
        return this.isGlobalGroupLoaded(name) || this.isLocalGroupLoaded(name);
    }

    public boolean isGroupExactLoaded(String identifer){
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        for(Group g : loadedGroups){
            if(caseSensitive && g.getIdentifier().equals(identifer) || !caseSensitive && g.getIdentifier().equalsIgnoreCase(identifer)){
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a new default group
     * @return a new default group
     */
    private Group getNewDefaultGroup(){
        return new GroupBuilder("default").setPrefix("[&7Default&f] ").setDescription("The default group")
                .addPermission(new PPermission("modifyworld.*")).build(); //Server Context is by default local
    }

    public Group getDefaultGroup() {
        String groupName = PermsManager.instance.getImplementation().getConfigSettings().getDefaultGroup();
        Group g = this.getVisibleGroup(groupName);
        if(g == null){
            g = this.getNewDefaultGroup();
            this.addGroup(g);
            PermsManager.instance.getImplementation().getConfigSettings().setDefaultGroup(g.getName());
            PermsManager.instance.getImplementation().addToLog("Unable to find default group, a new default group was created!");
        }
        return g;
    }
}

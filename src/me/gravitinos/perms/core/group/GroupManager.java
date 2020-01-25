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
                    if(!this.isGroupLoaded(g.getName())){
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

    /**
     * Gets a group that is loaded within this group manager
     * @param name The name of the group to look for
     * @return The group or null if the group is not contained within this group manager
     */
    public Group getGroup(String name){
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        for(Group g : loadedGroups){
            if(caseSensitive && g.getName().equals(name) || !caseSensitive && g.getName().equalsIgnoreCase(name)){
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
        this.loadedGroups.stream().forEach(this::unloadGroup);
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
        if(this.isGroupLoaded(group.getName())){
            return false;
        }

        this.loadedGroups.add(group);
        this.dataManager.addSubject(group); //Add the subject to the backend

        this.eliminateInheritanceMistakes();

        return true;
    }

    public CompletableFuture<Boolean> loadGroup(@NotNull String groupName, @NotNull SubjectSupplier supplier){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if(this.isGroupLoaded(groupName)){
            future.complete(false);
            return future;
        }

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                CachedSubject cachedSubject = dataManager.getSubject(groupName).get();
                Group g = new Group(cachedSubject, supplier, this);
                this.loadedGroups.add(g);
                this.eliminateInheritanceMistakes();
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
     * @return True if the group is loaded in this group manager, false otherwise
     */
    public boolean isGroupLoaded(String name){
        boolean caseSensitive = PermsManager.instance.getImplementation().getConfigSettings().isCaseSensitiveGroups();
        for(Group g : loadedGroups){
            if(caseSensitive && g.getName().equals(name) || !caseSensitive && g.getName().equalsIgnoreCase(name)){
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
                .addPermission(new PPermission("modifyworld.*")).build();
    }

    public Group getDefaultGroup() {
        String groupName = PermsManager.instance.getImplementation().getConfigSettings().getDefaultGroup();
        Group g = this.getGroup(groupName);
        if(g == null){
            g = this.getNewDefaultGroup();
            PermsManager.instance.getImplementation().getConfigSettings().setDefaultGroup(g.getName());
            PermsManager.instance.getImplementation().addToLog("Unable to find default group, a new default group was created!");
        }
        return g;
    }
}

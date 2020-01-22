package me.gravitinos.perms.core.group;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.subject.SubjectData;
import me.gravitinos.perms.core.subject.SubjectRef;
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
                    this.addGroup(g);
                    references.get(cs.getIdentifier()).setReference(g);
                });

                //Check for inheritance mistakes
                ArrayList<Subject<? extends SubjectData>> grps = new ArrayList<>(this.loadedGroups);
                Subject.checkForAndRemoveInheritanceMistakes(grps);

            } catch (Exception e) {
                out.complete(false);
                e.printStackTrace();
            }
            out.complete(true);
        });
        return out;
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
     * Adds a group to this group manager
     * @param group The group to add
     * @return True if the operation was successful and the group was added, or false if the group is already contained within this group manager
     */
    public boolean addGroup(Group group){
        if(this.isGroupLoaded(group.getName())){
            return false;
        }

        this.loadedGroups.add(group);

        return true;
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

}

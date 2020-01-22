package me.gravitinos.perms.core.group;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.subject.Subject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GroupManager {
    private DataManager dataManager;
    private ArrayList<Group> loadedGroups = new ArrayList<>();
    public GroupManager(@NotNull DataManager dataManager){
        this.dataManager = dataManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CompletableFuture<Boolean> loadGroups(){
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                ArrayList<CachedSubject> groups = dataManager.getAllSubjectsOfType(Subject.GROUP).get();
                Map<String, Group> tempGroups = new HashMap<>();

                //Make half-built objects
                for(CachedSubject group : groups){
                    tempGroups.put(group.getIdentifier(), new Group(new GroupBuilder(group.getIdentifier()), (s) -> null, this));
                }

                //Configure inheritances
                for(CachedSubject group : groups){
                    tempGroups.get(group.getIdentifier()).updateFromCachedSubject(group, tempGroups::get);
                }
                //TODO ----------
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return out;
    }

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

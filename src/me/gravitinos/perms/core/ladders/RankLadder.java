package me.gravitinos.perms.core.ladders;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.util.MapUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@Getter
@AllArgsConstructor
public class RankLadder {
    private UUID id;
    private List<UUID> groups;
    private ContextSet context;
    private DataManager dataManager;
    private ConcurrentMap<String, String> data;

    public void setRank(User user, int index) {
        Group group = GroupManager.instance.getGroupExact(groups.get(index));
        if(group == null)
            return;
        user.addInheritance(group, context);
    }

    public synchronized void setName(String name){
        //TODO data update
        this.data.put("name", name);
        dataManager.updateRankLadder(this);
    }

    public String getName(){
        return this.data.get("name");
    }

    public synchronized void promote(User user) {
        this.cleanupGroups();
        int currentIndex = getRankIndex(user);
        if(currentIndex+1 >= this.groups.size())
            return;
        for(Group group : user.getGroupsInOrderOfPriority()){
            if(this.groups.contains(group.getSubjectId()))
                user.removeInheritance(group);
        }
        user.addInheritance(GroupManager.instance.getGroupExact(this.groups.get(currentIndex+1)), context);
    }

    public synchronized void demote(User user) {
        this.cleanupGroups();
        int currentIndex = getRankIndex(user);
        if(currentIndex-1 < 0)
            return;
        for(Group group : user.getGroupsInOrderOfPriority()){
            if(this.groups.contains(group.getSubjectId()))
                user.removeInheritance(group);
        }
        user.addInheritance(GroupManager.instance.getGroupExact(this.groups.get(currentIndex-1)), context);
    }

    public synchronized int getRankIndex(User user){
        List<Group> groups = user.getGroupsInOrderOfPriority();
        for(Group g : groups){
            if(this.groups.contains(g.getSubjectId()))
                return this.groups.indexOf(g);
        }
        return -1;
    }

    /**
     * Setting context might remove certain groups that become incompatible
     */
    public synchronized void setContext(ContextSet set){
        this.context = set;
        dataManager.updateRankLadder(this);
        this.cleanupGroups();
    }

    public String getDataEncoded(){
        return MapUtil.mapToString(data);
    }

    public synchronized List<UUID> cleanupGroups(){
        List<UUID> removed = new ArrayList<>();
        this.groups.removeIf(g -> {
            Group m = GroupManager.instance.getGroupExact(g);
            boolean rem = m == null || !canAddGroup(m);
            if(rem)
                removed.add(g);
            return rem;
        });
        return removed;
    }

    public synchronized boolean addGroup(Group group){
        if(!canAddGroup(group))
            return false;
        this.groups.add(id);
        dataManager.updateRankLadder(this);
        return true;
    }
    public synchronized boolean removeGroup(Group group) {
        return this.groups.remove(group.getSubjectId());
    }

    public boolean containsGroup(Group group) {
        return this.groups.contains(group.getSubjectId());
    }

    public boolean canAddGroup(Group group){
        return context.isSatisfiedBy(this.context);
    }
}
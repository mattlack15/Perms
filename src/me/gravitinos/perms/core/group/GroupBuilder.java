package me.gravitinos.perms.core.group;

import com.google.common.cache.Cache;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.subject.SubjectRef;

import java.util.ArrayList;
import java.util.UUID;

public class GroupBuilder {
    private GroupData data = new GroupData();
    private UUID groupId = UUID.randomUUID();
    private ArrayList<CachedInheritance> inherited = new ArrayList<>();
    private ArrayList<PPermission> permissions = new ArrayList<>();

    public GroupData getData(){
        return this.data;
    }
    public String getName(){
        return this.data.getName();
    }

    public GroupBuilder(String name){
        this.data.setName(name);
    }

    public GroupBuilder setName(String name){
        this.data.setName(name);
        inherited.forEach(i -> i.setChild(groupId)); // Update the cached Inheritances
        return this;
    }
    public GroupBuilder setPrefix(String prefix){
        this.data.setPrefix(prefix);
        return this;
    }
    public GroupBuilder setSuffix(String suffix){
        this.data.setSuffix(suffix);
        return this;
    }

    public GroupBuilder setDescription(String description){
        this.data.setDescription(description);
        return this;
    }

    public GroupBuilder setChatColour(String colour){
        this.data.setChatColour(colour);
        return this;
    }

    public GroupBuilder addInheritance(Subject inheritance){
        return this.addInheritance(inheritance, new MutableContextSet());
    }

    public GroupBuilder addInheritance(Subject inheritance, ContextSet context){
        this.inherited.add(new CachedInheritance(groupId, inheritance.getSubjectId(), Subject.GROUP, inheritance.getType(), context));
        return this;
    }

    public GroupBuilder addPermission(PPermission permission){
        this.permissions.add(permission);
        return this;
    }

    public CachedSubject toCachedSubject(){
        return new CachedSubject(groupId, Subject.GROUP, this.data, this.permissions, inherited);
    }

    public ArrayList<PPermission> getPermissions(){
        return this.permissions;
    }

    public ArrayList<CachedInheritance> getInherited(){
        return this.inherited;
    }

    public Group build(){
        return new Group(this.toCachedSubject(), (s) -> new SubjectRef(GroupManager.instance.getGroupExact(s)), GroupManager.instance);
    }

}

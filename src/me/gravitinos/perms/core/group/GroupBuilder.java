package me.gravitinos.perms.core.group;

import com.google.common.cache.Cache;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;

import java.util.ArrayList;

public class GroupBuilder {
    private GroupData data = new GroupData();
    private String name = "";
    private ArrayList<CachedInheritance> inherited = new ArrayList<>();
    private ArrayList<PPermission> permissions = new ArrayList<>();

    public GroupData getData(){
        return this.data;
    }
    public String getName(){
        return this.name;
    }

    public GroupBuilder(String name){
        this.name = name;
    }

    public GroupBuilder setName(String name){
        this.name = name;
        inherited.forEach(i -> i.setChild(this.name)); // Update the cached Inheritances
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
        return this.addInheritance(inheritance, Context.CONTEXT_ALL);
    }

    public GroupBuilder addInheritance(Subject inheritance, Context context){
        this.inherited.add(new CachedInheritance(this.name, inheritance.getIdentifier(), Subject.GROUP, inheritance.getType(), context));
        return this;
    }

    public CachedSubject toCachedSubject(){
        return new CachedSubject(this.name, Subject.GROUP, this.data, this.permissions, inherited);
    }

    public ArrayList<PPermission> getPermissions(){
        return this.permissions;
    }

    public ArrayList<CachedInheritance> getInherited(){
        return this.inherited;
    }

}

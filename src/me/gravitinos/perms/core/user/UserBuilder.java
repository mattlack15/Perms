package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.subject.SubjectRef;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.UUID;

public class UserBuilder {
    private UUID uuid;
    private String name;
    private UserData data = new UserData();
    private ArrayList<CachedInheritance> inherited = new ArrayList<>();
    private ArrayList<PPermission> permissions = new ArrayList<>();

    public UserBuilder(UUID id, String name){
        this.setUUIDAndName(id, name);
    }

    public UserBuilder setPermissions(@NotNull ImmutablePermissionList permissions){
        this.permissions = permissions.getPermissions();
        return this;
    }

    public UserBuilder setUUIDAndName(@NotNull UUID id, @NotNull String name){
        this.uuid = id;
        this.data.setName(name);
        return this;
    }

    public UserBuilder addInheritance(Subject subject, Context context){
        this.inherited.add(new CachedInheritance(this.getUniqueID().toString(), subject.getIdentifier(), Subject.USER, subject.getType(), context));
        return this;
    }
    public UserBuilder addPermission(PPermission perm){
        this.permissions.add(perm);
        return this;
    }


    public CachedSubject toCachedSubject(){
        return new CachedSubject(uuid.toString(), Subject.USER, data, permissions, inherited);
    }

    public String getName(){
        return this.data.getName();
    }

    public ArrayList<PPermission> getPermissions(){
        return this.permissions;
    }

    public UUID getUniqueID(){
        return this.uuid;
    }

    public UserData getData(){
        return this.data;
    }

    public UserBuilder setDisplayGroup(String serverContext, Group group){
        this.getData().setDisplayGroup(serverContext, group.getIdentifier());
        return this;
    }

    public UserBuilder setPrefix(String prefix){
        this.getData().setPrefix(prefix);
        return this;
    }

    public UserBuilder setSuffix(String suffix){
        this.getData().setSuffix(suffix);
        return this;
    }

    public User build(){
        return new User(this.toCachedSubject(), (s) -> new SubjectRef(GroupManager.instance.getGroupExact(s)), UserManager.instance);
    }

}

package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.UUID;

public class UserBuilder {
    private UUID uuid = null;
    private String name = "";
    private UserData data = new UserData();
    private ArrayList<CachedInheritance> inherited = new ArrayList<>();
    private ArrayList<PPermission> permissions = new ArrayList<>();

    public UserBuilder(UUID id, String name){
        this.uuid = id;
        this.name = name;
    }

    public UserBuilder setPermissions(@NotNull ImmutablePermissionList permissions){
        this.permissions = permissions.getPermissions();
        return this;
    }

    public UserBuilder setUUIDAndName(@NotNull UUID id, @NotNull String name){
        this.uuid = id;
        this.name = name;
        return this;
    }


    public CachedSubject toCachedSubject(){
        return new CachedSubject(uuid.toString(), Subject.USER, data, permissions, inherited);
    }

    public String getName(){
        return this.name;
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

}

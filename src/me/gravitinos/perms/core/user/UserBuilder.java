package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.UUID;

public class UserBuilder {
    protected UUID uuid = null;
    protected String name = "";
    protected ArrayList<PPermission> permissions = new ArrayList<>();

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

}

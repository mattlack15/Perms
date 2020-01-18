package me.gravitinos.perms.core.subject;

import java.util.ArrayList;

public final class ImmutablePermissionList {
    private final ArrayList<PPermission> perms;

    public ImmutablePermissionList(ArrayList<PPermission> perms){
        this.perms = perms;
    }

    public ArrayList<PPermission> getPermissions() {
        return (ArrayList<PPermission>) perms.clone();
    }
}

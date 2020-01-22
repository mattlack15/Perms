package me.gravitinos.perms.core.subject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;

public final class ImmutablePermissionList implements Iterable<PPermission>{
    private final ArrayList<PPermission> perms;

    public ImmutablePermissionList(ArrayList<PPermission> perms){
        this.perms = perms;
    }

    public ArrayList<PPermission> getPermissions() {
        return (ArrayList<PPermission>) perms.clone();
    }

    @NotNull
    @Override
    public Iterator<PPermission> iterator() {
        return perms.iterator();
    }
}

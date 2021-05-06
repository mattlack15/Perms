package me.gravitinos.perms.core.subject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ImmutablePermissionList implements Iterable<PPermission>{
    private final List<PPermission> perms;

    public ImmutablePermissionList(List<PPermission> perms){
        this.perms = perms;
    }

    public List<PPermission> getPermissions() {
        return new ArrayList<>(perms);
    }

    @NotNull
    @Override
    public Iterator<PPermission> iterator() {
        return getPermissions().iterator();
    }
}

package me.gravitinos.perms.core.cache;

import me.gravitinos.perms.core.subject.PPermission;

/**
 * Immutable pair of permission and owner identifier
 */
public class OwnerPermissionPair {
    private PPermission permission;
    private String ownerIdentifier;
    private String perm;

    public OwnerPermissionPair(String ownerIdentifier, PPermission permission) {
        this.ownerIdentifier = ownerIdentifier;
        this.permission = permission;
    }
    public OwnerPermissionPair(String ownerIdentifier, String perm){
        this.perm = perm;
    }

    public String getPermissionString(){
        return perm;
    }

    public PPermission getPermission() {
        return permission;
    }

    public String getOwnerIdentifier() {
        return ownerIdentifier;
    }
}

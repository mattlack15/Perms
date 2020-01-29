package me.gravitinos.perms.core.cache;

import me.gravitinos.perms.core.subject.PPermission;

import java.util.UUID;

/**
 * Immutable pair of permission and owner identifier
 */
public class OwnerPermissionPair {
    private PPermission permission;
    private String ownerIdentifier;
    private UUID identifier;
    private String perm;

    public OwnerPermissionPair(String ownerIdentifier, PPermission permission) {
        this.ownerIdentifier = ownerIdentifier;
        this.permission = permission;
        this.perm = permission.getPermission();
        this.identifier = permission.getPermissionIdentifier();
    }
    public OwnerPermissionPair(String ownerIdentifier, String perm, UUID permissionIdentifier){
        this.perm = perm;
        this.ownerIdentifier = ownerIdentifier;
        this.identifier = permissionIdentifier;
    }

    public String getPermissionString(){
        return perm;
    }

    public UUID getPermissionIdentifier() {return this.identifier;}

    public PPermission getPermission() {
        return permission;
    }

    public String getOwnerIdentifier() {
        return ownerIdentifier;
    }
}

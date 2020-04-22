package me.gravitinos.perms.core.cache;

import me.gravitinos.perms.core.subject.PPermission;

import java.util.UUID;

/**
 * Immutable pair of permission and owner identifier
 */
public class OwnerPermissionPair {
    private PPermission permission;
    private UUID ownerSubjectId;
    private UUID identifier;
    private String perm;

    public OwnerPermissionPair(UUID ownerSubjectId, PPermission permission) {
        this.ownerSubjectId = ownerSubjectId;
        this.permission = permission;
        this.perm = permission.getPermission();
        this.identifier = permission.getPermissionIdentifier();
    }
    public OwnerPermissionPair(UUID ownerSubjectId, String perm, UUID permissionIdentifier){
        this.perm = perm;
        this.ownerSubjectId = ownerSubjectId;
        this.identifier = permissionIdentifier;
    }

    public String getPermissionString(){
        return perm;
    }

    public UUID getPermissionIdentifier() {return this.identifier;}

    public PPermission getPermission() {
        return permission;
    }

    public UUID getOwnerSubjectId() {
        return ownerSubjectId;
    }
}

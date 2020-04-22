package me.gravitinos.perms.core.cache;

import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.SubjectData;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Immutable
 */
public final class CachedSubject {
    private String type;

    private UUID subjectId;

    private SubjectData data;
    private ArrayList<PPermission> permissions;
    private ArrayList<CachedInheritance> inheritances;

    public CachedSubject(UUID subjectId, String type, SubjectData data, ArrayList<PPermission> permissions, ArrayList<CachedInheritance> inheritances) {
        this.type = type;
        this.data = data;
        this.permissions = permissions;
        this.inheritances = inheritances;
        this.subjectId = subjectId;
    }

    public UUID getSubjectId(){
        return this.subjectId;
    }

    public ArrayList<CachedInheritance> getInheritances() {
        return inheritances;
    }

    public ArrayList<PPermission> getPermissions() {
        return permissions;
    }

    public String getType() {
        return type;
    }

    public SubjectData getData() {
        return data;
    }

    public void setPermissions(ArrayList<PPermission> permissions) {
        this.permissions = permissions;
    }


    public void setInheritances(ArrayList<CachedInheritance> inheritances) {
        this.inheritances = inheritances;
    }

    public void setData(SubjectData data) {
        this.data = data;
    }

    public void setType(String type) {
        this.type = type;
    }
}

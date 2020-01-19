package me.gravitinos.perms.core.cache;

import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.SubjectData;

import java.util.ArrayList;

/**
 * Immutable
 */
public final class CachedSubject {
    private String type;
    private String name;
    private SubjectData data;
    private ArrayList<PPermission> permissions;
    private ArrayList<CachedInheritance> inheritances;

    public CachedSubject(String name, String type, SubjectData data, ArrayList<PPermission> permissions, ArrayList<CachedInheritance> inheritances) {
        this.name = name;
        this.type = type;
        this.data = data;
        this.permissions = permissions;
        this.inheritances = inheritances;
    }

    public ArrayList<CachedInheritance> getInheritances() {
        return inheritances;
    }

    public ArrayList<PPermission> getPermissions() {
        return permissions;
    }

    public String getName() {
        return name;
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

    public void setName(String name) {
        this.name = name;
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

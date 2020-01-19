package me.gravitinos.perms.core.cache;

import me.gravitinos.perms.core.context.Context;

/**
 * Immutable
 */
public final class CachedInheritance {
    private String child;
    private String parent;
    private String childType;
    private String parentType;
    private Context context;

    public CachedInheritance(String child, String parent, String childType, String parentType, Context context){
        this.child = child;
        this.parent = parent;
        this.childType = childType;
        this.parentType = parentType;
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public String getChildType() {
        return childType;
    }

    public String getChild() {
        return child;
    }

    public String getParent() {
        return parent;
    }

    public String getParentType() {
        return parentType;
    }

}

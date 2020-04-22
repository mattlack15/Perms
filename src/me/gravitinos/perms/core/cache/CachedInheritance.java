package me.gravitinos.perms.core.cache;

import me.gravitinos.perms.core.context.Context;

import java.util.UUID;

/**
 * Immutable
 */
public final class CachedInheritance {
    private UUID child;
    private UUID parent;
    private String childType;
    private String parentType;
    private Context context;

    public CachedInheritance(UUID child, UUID parent, String childType, String parentType, String context){
        this.child = child;
        this.parent = parent;
        this.childType = childType;
        this.parentType = parentType;
        this.context = Context.fromString(context);
    }

    public CachedInheritance(UUID child, UUID parent, String childType, String parentType, Context context){
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

    public UUID getChild() {
        return child;
    }

    public UUID getParent() {
        return parent;
    }

    public String getParentType() {
        return parentType;
    }

    public void setChild(UUID child) {
        this.child = child;
    }

    public void setChildType(String childType) {
        this.childType = childType;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setParent(UUID parent) {
        this.parent = parent;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }
}

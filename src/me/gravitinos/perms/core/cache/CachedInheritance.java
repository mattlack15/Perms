package me.gravitinos.perms.core.cache;

import lombok.Getter;
import lombok.Setter;
import me.gravitinos.perms.core.context.ContextSet;

import java.util.UUID;

@Getter
@Setter
public final class CachedInheritance {
    private UUID child;
    private UUID parent;
    private String childType;
    private String parentType;
    private ContextSet context;

    public CachedInheritance(UUID child, UUID parent, String childType, String parentType, String context){
        this.child = child;
        this.parent = parent;
        this.childType = childType;
        this.parentType = parentType;
        this.context = ContextSet.fromString(context);
    }

    public CachedInheritance(UUID child, UUID parent, String childType, String parentType, ContextSet context){
        this.child = child;
        this.parent = parent;
        this.childType = childType;
        this.parentType = parentType;
        this.context = context;
    }
}

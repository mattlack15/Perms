package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.Subject;

import java.lang.ref.WeakReference;

public class Inheritance {
    private SubjectRef parent;
    private SubjectRef child;
    private Context context;

    public Inheritance(SubjectRef parent, SubjectRef child, Context context){
        this.parent = parent;
        this.child = child;
        this.context = context;
    }

    /**
     * Gets Parent of inheritance
     * @return the parent
     */
    public Subject<? extends SubjectData> getParent(){
        return this.parent.get();
    }

    /**
     * Gets Child of inheritance
     * @return the child
     */
    public Subject<? extends SubjectData> getChild(){
        return this.child.get();
    }

    public CachedInheritance toCachedInheritance(){
        if(!this.isValid()) {
            return null;
        }
        return new CachedInheritance(this.getChild().getIdentifier(), this.getParent().getIdentifier(), this.getChild().getType(), this.getParent().getType(), this.getContext());
    }

    /**
     * Gets context
     * @return the context
     */
    public Context getContext(){
        return this.context;
    }

    /**
     * Gets if this is valid
     * @return Whether or not the parent, child or context is null and therefore whether or not this inheritance is valid
     */
    public boolean isValid(){
        return this.child != null && this.parent != null && this.child.get() != null && this.parent.get() != null && this.context != null;
    }
}

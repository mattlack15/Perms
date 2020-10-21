package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.context.ContextSet;

public class Inheritance {
    private final SubjectRef parent;
    private final SubjectRef child;
    private final ContextSet context;

    public Inheritance(SubjectRef parent, SubjectRef child, ContextSet context){
        this.parent = parent;
        this.child = child;
        this.context = context;
    }

    /**
     * Gets Parent of inheritance
     * @return the parent
     */
    public Subject<? extends SubjectData> getParent(){
        return this.parent != null ? this.parent.get() : null;
    }

    /**
     * Gets Child of inheritance
     * @return the child
     */
    public Subject<? extends SubjectData> getChild(){
        return this.child != null ? this.child.get() : null;
    }

    public CachedInheritance toCachedInheritance(){
        if(!this.isValid()) {
            return null;
        }
        return new CachedInheritance(this.getChild().getSubjectId(), this.getParent().getSubjectId(), this.getChild().getType(), this.getParent().getType(), this.getContext());
    }

    /**
     * Gets contexts in which this inheritance should apply
     * @return the context
     */
    public ContextSet getContext(){
        return this.context;
    }

    /**
     * Gets if this is valid
     * @return Whether or not the parent, child or context is null and therefore whether or not this inheritance is valid
     */
    public boolean isValid(){
        return this.child != null && this.parent != null && this.child.get() != null && this.parent.get() != null && this.context != null;
    }

    @Override
    public boolean equals(Object o){
        if(o == this) return true;
        if(o instanceof Inheritance){
            if(!((Inheritance) o).isValid()){
                return !this.isValid();
            }

            if(((Inheritance) o).getChild().getSubjectId().equals(this.getChild().getSubjectId())){
                return ((Inheritance) o).getParent().getSubjectId().equals(this.getParent().getSubjectId());
            }
        }
        return false;
    }
}

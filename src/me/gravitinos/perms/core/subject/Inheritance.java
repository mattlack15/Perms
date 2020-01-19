package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.Subject;

import java.lang.ref.WeakReference;

public class Inheritance {
    private WeakReference<Subject> parent;
    private WeakReference<Subject> child;
    private Context context;

    public Inheritance(Subject parent, Subject child, Context context){
        this.parent = new WeakReference<>(parent);
        this.child = new WeakReference<>(child);
        this.context = context;
    }

    /**
     * Gets Parent of inheritance
     * @return the parent
     */
    public Subject getParent(){
        return this.parent.get();
    }

    /**
     * Gets Child of inheritance
     * @return the child
     */
    public Subject getChild(){
        return this.child.get();
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
        return this.child.get() != null && this.parent.get() != null && this.context != null;
    }
}

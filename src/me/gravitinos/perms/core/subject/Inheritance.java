package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.Subject;

public class Inheritance {
    private Subject parent;
    private Subject child;
    private Context context;

    public Inheritance(Subject parent, Subject child, Context context){
        this.parent = parent;
        this.child = child;
        this.context = context;
    }

    /**
     * Gets Parent of inheritance
     * @return the parent
     */
    public Subject getParent(){
        return this.parent;
    }

    /**
     * Gets Child of inheritance
     * @return the child
     */
    public Subject getChild(){
        return this.child;
    }

    /**
     * Gets context
     * @return the context
     */
    public Context getContext(){
        return this.context;
    }
}

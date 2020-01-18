package me.gravitinos.perms.core.subject;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.util.WeakList;

import java.util.ArrayList;

/**
 * A class that represents an object that can have permissions
 */
public abstract class Subject {

    //Types
    public static final String GROUP = "GROUP";
    public static final String USER = "USER";

    private String type;

    private final String name;

    private ArrayList<PPermission> ownPermissions = new ArrayList<>();
    private WeakList<Inheritance> inherited = new WeakList<>();

    public Subject(String name, String type){
        this.type = type;
        this.name = name;
    }

    /**
     * Sets this subject's own permissions
     * @param ownPermissions
     */
    protected void setOwnPermissions(ArrayList<PPermission> ownPermissions){
        this.ownPermissions = ownPermissions;
    }


    /**
     * Get an immutable set of the permissions this subject contains
     * @return Immutable set of permissions
     */
    protected ImmutablePermissionList getPermissions(){
        return new ImmutablePermissionList(ownPermissions);
    }

    /**
     * Get a list of the inherited subjects and the inheritance contexts
     * @return list of inheritances
     */
    protected ArrayList<Inheritance> getInheritances(){
        return Lists.newArrayList(this.inherited);
    }

    protected void removeInheritance(Subject subject){
    }

    /**
     * Adds an inheritance to this subject
     * @param subject The inheritance to add
     */
    protected void addInheritance(Subject subject, Context context){
        this.inherited.add(new Inheritance(subject, this, context));
    }

    /**
     * Adds a permission to this subject's own permission set
     * @param permission the permission to add
     */
    protected void addOwnPermission(PPermission permission){
        this.ownPermissions.add(permission);
    }

    /**
     * Checks if this subject contains this exact permission, including checks for context and expiration
     * @param permission
     * @return
     */
    protected boolean hasOwnPermission(PPermission permission){
        return this.ownPermissions.contains(permission);
    }

    /**
     * Checks if this subject contains this permission, excluding checks for context or expiration
     * @param permission
     * @return
     */
    protected boolean hasOwnPermission(String permission){
        for(PPermission perms : ownPermissions){
            if(perms.getPermission().equalsIgnoreCase(permission)){
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all permissions from this subject, including inherited permissions
     * @return
     */
    protected ArrayList<PPermission> getAllPermissions(Context context){
        ArrayList<PPermission> perms = new ArrayList<>(ownPermissions);

        for(Inheritance inheritances : inherited){

            //Check if the inheritance's context applies to the specified context
            if(!inheritances.getContext().applies(context)){
                continue;
            }

            //Add all the permissions to the perms array list
            perms.addAll(inheritances.getParent().getAllPermissions(context)); //TODO Possibly edit this if permissions are duplicated
        }

        return perms;
    }


}

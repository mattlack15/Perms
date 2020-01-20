package me.gravitinos.perms.core.subject;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.util.WeakList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * A class that represents an object that can have permissions
 */
public abstract class Subject<T extends SubjectData> {

    //Types
    public static final String GROUP = "GROUP";
    public static final String USER = "USER";

    private String type;

    private final String identifier;

    private ArrayList<PPermission> ownPermissions = new ArrayList<>();
    private ArrayList<Inheritance> inherited = new ArrayList<>();
    private T data;

    public Subject(String identifier, String type, T data){
        this.type = type;
        this.identifier = identifier;
        this.data = data;
    }

    public T getData(){
        return this.data;
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
        this.inherited.removeIf((i) -> !i.isValid());
        return Lists.newArrayList(this.inherited);
    }

    /**
     * Removes all inheritances where parent.equals(inheritance.getParent())
     * @param parent The specified parent to remove inheritances to
     */
    protected void removeInheritance(@NotNull Subject parent){
        this.inherited.removeIf(i -> !i.isValid() || parent.equals(i.getParent()));
    }

    /**
     * Removes all permissions where permission.equals(ownPermission)
     * @param permission The permission to remove
     */
    protected void removeOwnPermission(@NotNull PPermission permission){
        this.ownPermissions.removeIf(p -> permission.equals(p));
    }

    /**
     * Removes all permissions where p.getPermission().equalsIgnoreCase(permission)
     * @param permission the permission to remove
     */
    protected void removeOwnPermission(@NotNull String permission){
        this.ownPermissions.removeIf(p -> p.getPermission().equalsIgnoreCase(permission));
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
     * Gets the type of subject this is
     * @return Type
     */
    public String getType() {
        return type;
    }

    public String getIdentifier(){
        return identifier;
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

package me.gravitinos.perms.core.subject;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A class that represents an object that can have permissions, inheritances, and customizable data objects (SubjectData)
 */
public abstract class Subject<T extends SubjectData> {

    //Types
    public static final String GROUP = "GROUP";
    public static final String USER = "USER";

    private String type;

    private String identifier;

    private ArrayList<PPermission> ownPermissions = new ArrayList<>();
    private ArrayList<Inheritance> inherited = new ArrayList<>();
    private T data;

    public Subject(@NotNull String identifier, @NotNull String type, @NotNull T data){
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
    protected void setOwnPermissions(@NotNull ArrayList<PPermission> ownPermissions){
        this.ownPermissions = ownPermissions;
    }

    public static ArrayList<Inheritance> getInheritances(Subject<?> subject){
        return subject.getInheritances();
    }

    public static ImmutablePermissionList getPermissions(Subject<?> subject){
        return subject.getPermissions();
    }


    /**
     * Get an immutable set of the permissions this subject contains
     * @return Immutable set of permissions
     */
    protected ImmutablePermissionList getPermissions(){
        return new ImmutablePermissionList(ownPermissions);
    }

    /**
     * Sets the internal data object for this subject
     * @param data
     */
    protected void setData(T data){
        this.data = data;
    }

    /**
     * Sets the identifier for this subject
     * @param identifier
     */
    protected void setIdentifier(@NotNull String identifier){
        this.identifier = identifier;
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
    protected void removeInheritance(Subject<? extends SubjectData> parent){
        this.inherited.removeIf(i -> !i.isValid() || parent.equals(i.getParent()));
    }

    /**
     * Removes all permissions where permission.equals(ownPermission)
     * @param permission The permission to remove
     */
    protected PPermission removeOwnPermission(@NotNull PPermission permission){
        AtomicReference<PPermission> p = new AtomicReference<>();
        this.ownPermissions.stream().forEach(perm -> {
            if(perm.equals(permission)){
                p.set(perm);
                ownPermissions.remove(perm);
            }
        });
        return p.get();
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
    protected void addInheritance(Subject<? extends SubjectData> subject, Context context){
        this.inherited.add(new Inheritance(new SubjectRef(subject), new SubjectRef(this), context));
    }

    /**
     * Adds an inheritance to this subject
     * @param subject The inheritance to add
     */
    protected void addInheritance(SubjectRef subject, Context context){
        this.inherited.add(new Inheritance(subject, new SubjectRef(this), context));
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
    protected boolean hasOwnPermission(String permission, Context context){
        for(PPermission perms : ownPermissions){
            if(perms.getPermission().equalsIgnoreCase(permission) && perms.getContext().applies(context)){
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

    protected boolean hasOwnOrInheritedPermission(String permission, Context context){
        if(this.hasOwnPermission(permission, context)){
            return true;
        }
        for(Inheritance inheritances : getInheritances()){

            //Check if the inheritance's context applies to the specified context
            if(!inheritances.getContext().applies(context)){
                continue;
            }

            if(inheritances.getParent().hasOwnOrInheritedPermission(permission, context)){
                return true;
            }
        }

        return false;
    }

    /**
     * Gets all permissions from this subject, including inherited permissions
     * @return
     */
    protected ArrayList<PPermission> getAllPermissions(Context inheritanceContext){
        ArrayList<PPermission> perms = new ArrayList<>(ownPermissions);

        for(Inheritance inheritances : getInheritances()){

            //Check if the inheritance's context applies to the specified context
            if(!inheritances.getContext().applies(inheritanceContext)){
                continue;
            }

            //Add all the permissions to the perms array list
            inheritances.getParent().getAllPermissions(inheritanceContext);
            perms.addAll(inheritances.getParent().getAllPermissions(inheritanceContext)); //TODO Possibly edit this if permissions are duplicated
        }

        return perms;
    }

    /**
     * Checks for, removes, and logs inheritance mistakes in a group of subjects
     * @param subjects the group of subjects to check
     */
    public static void checkForAndRemoveInheritanceMistakes(ArrayList<Subject> subjects){
        ArrayList<Subject<?>> visited = new ArrayList<>();
        for(Subject<?> subject : subjects){
            visited.clear();
            visited.add(subject);

            for(int i1 = 0; i1 < visited.size(); i1++){
                int finalI = i1;
                visited.get(i1).getInheritances().forEach(i -> {
                    if (visited.contains(i.getParent())) {
                        visited.get(finalI).removeInheritance(i.getParent());
                        PermsManager.instance.getImplementation().addToLog(ChatColor.RED + "Mistake in inheritances, removed inheritance \"" +
                                i.getParent().getIdentifier() + "\" from subject \"" + i.getChild().getIdentifier());
                        return;
                    }
                    visited.add(i.getParent());
                });
            }
        }
    }


}

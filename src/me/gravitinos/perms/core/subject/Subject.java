package me.gravitinos.perms.core.subject;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.context.Context;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
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

    public abstract DataManager getDataManager();

    public T getData(){
        return this.data;
    }

    /**
     * Sets this subject's own permissions
     * @param ownPermissions
     */
    protected void setOwnPermissions(@NotNull ArrayList<PPermission> ownPermissions){
        this.ownPermissions = ownPermissions;
        this.ownPermissions.removeIf(Objects::isNull);
    }

    /**
     * Util
     */
    protected void removeExpiredPerms(){
        ImmutablePermissionList list = new ImmutablePermissionList(ownPermissions);

        long ctm = System.currentTimeMillis();

        ArrayList<PPermission> remove = new ArrayList<>();

        for(PPermission permissions : list){
            if(permissions.isExpired(ctm)){
                remove.add(permissions);
            }
        }

        if(remove.size() > 0) {
            remove.forEach(this::removeOwnSubjectPermission); //Remove from object
            if(this.getDataManager() != null) {
                this.getDataManager().removePermissionsExact(this, remove); //Update backend
            }
        }

        this.ownPermissions.removeIf(Objects::isNull);
    }

    public static ArrayList<Inheritance> getInheritances(Subject<?> subject){
        return subject.getInheritances();
    }

    public static ImmutablePermissionList getPermissions(Subject<?> subject){
        return subject.getPermissions();
    }

    public static void setIdentifier(Subject<?> subject, String identifier) {subject.setIdentifier(identifier); }


    /**
     * Get an immutable set of the permissions this subject contains
     * @return Immutable set of permissions
     */
    protected ImmutablePermissionList getPermissions(){
        this.removeExpiredPerms();
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
        this.inherited.removeIf(Objects::isNull);
        this.inherited.removeIf((i) -> !i.isValid());
        return Lists.newArrayList(this.inherited);
    }

    /**
     * Removes all inheritances where parent.equals(inheritance.getParent())
     * @param parent The specified parent to remove inheritances to
     */
    protected void removeOwnSubjectInheritance(Subject<? extends SubjectData> parent){
        this.inherited.removeIf(i -> !i.isValid() || parent.equals(i.getParent()));
    }

    /**
     * Removes all permissions where permission.equals(ownPermission)
     * @param permission The permission to remove
     */
    protected PPermission removeOwnSubjectPermission(@NotNull PPermission permission){
        AtomicReference<PPermission> p = new AtomicReference<>();
        this.ownPermissions.removeIf(Objects::isNull);
        ((ArrayList<PPermission>)this.ownPermissions.clone()).forEach(perm -> {
            if(perm.getPermissionIdentifier().equals(permission.getPermissionIdentifier())){
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
    protected void removeOwnSubjectPermission(@NotNull String permission){
        this.ownPermissions.removeIf(p -> p.getPermission().equalsIgnoreCase(permission));
    }

    /**
     * Adds an inheritance to this subject
     * @param subject The inheritance to add
     */
    protected void addOwnSubjectInheritance(Subject<?> subject, Context context){
        this.inherited.add(new Inheritance(new SubjectRef(subject), new SubjectRef(this), context));
    }

    /**
     * Adds an inheritance to this subject
     * @param subject The inheritance to add
     */
    protected void addOwnSubjectInheritance(SubjectRef subject, Context context){
        this.inherited.add(new Inheritance(subject, new SubjectRef(this), context));
    }


    /**
     * Adds a permission to this subject's own permission set
     * @param permission the permission to add
     */
    protected void addOwnSubjectPermission(@NotNull PPermission permission){
        if(permission == null){
            return;
        }
        this.ownPermissions.add(permission);
    }

    /**
     * Checks if this subject contains this exact permission, including checks for context and expiration
     * @param permission
     * @return
     */
    protected boolean hasOwnPermission(PPermission permission){
        return this.getPermissions().getPermissions().contains(permission);
    }

    /**
     * Checks if this subject contains this permission, excluding checks for context or expiration
     * @param permission
     * @return
     */
    protected boolean hasOwnPermission(String permission, Context context){
        for(PPermission perms : this.getPermissions()){
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
        ArrayList<PPermission> perms = new ArrayList<>(this.getPermissions().getPermissions());

        for(Inheritance inheritances : getInheritances()){

            //Check if the inheritance's context applies to the specified context
            if(!inheritances.getContext().applies(inheritanceContext)){
                continue;
            }

            //Add all the permissions to the perms array list
            inheritances.getParent().getAllPermissions(inheritanceContext);
            perms.addAll(inheritances.getParent().getAllPermissions(inheritanceContext));
        }

        return perms;
    }

    /**
     * Checks for, removes, and logs inheritance mistakes in a group of subjects
     * @param subjects the group of subjects to check
     */
    public static void checkForAndRemoveInheritanceMistakes(ArrayList<Subject> subjects){
        for(Subject subject : subjects){
            treeSearchThing(subject, new ArrayList<>());
        }
    }

    private static void treeSearchThing(Subject<?> sub, ArrayList<Subject<?>> prev){
        ArrayList<Subject<?>> subs = Lists.newArrayList(prev);
        subs.add(sub);

        for(Inheritance i : sub.getInheritances()){
            Subject<?> subj = i.getParent();
            if(subs.contains(subj)){
                sub.removeOwnSubjectInheritance(subj);
                PermsManager.instance.getImplementation().addToLog(ChatColor.RED + "Mistake in inheritances, temporarily removed inheritance \"" +
                        subj.getIdentifier() + "\" from subject \"" + i.getChild().getIdentifier() + "\" please manually remove inheritance for a permanent effect");
            } else {
                treeSearchThing(subj, subs);
            }
        }
    }


}

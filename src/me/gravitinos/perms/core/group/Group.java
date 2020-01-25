package me.gravitinos.perms.core.group;

import com.google.common.base.Preconditions;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.*;
import me.gravitinos.perms.core.util.SubjectSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Group extends Subject<GroupData> {
    private DataManager dataManager;

    public Group(GroupBuilder builder, SubjectSupplier inheritanceSupplier) {
        this(builder, inheritanceSupplier, null);
    }

    public Group(@NotNull CachedSubject cachedSubject, @NotNull SubjectSupplier inheritanceSupplier, GroupManager manager){
        super(cachedSubject.getIdentifier(), Subject.GROUP, new GroupData(cachedSubject.getData()));
        this.updateFromCachedSubject(cachedSubject, inheritanceSupplier, false);

        this.dataManager = manager != null ? manager.getDataManager() : null;

        if (dataManager != null) {
            this.getData().addUpdateListener("MAIN_LISTENER", (k, v) -> dataManager.updateSubjectData(this));
        }
    }

    public Group(@NotNull GroupBuilder builder, @NotNull SubjectSupplier inheritanceSupplier, GroupManager manager) {
        this(builder.toCachedSubject(), inheritanceSupplier, manager);
    }

    /**
     * Updates this group with a cachedSubject's values with what is compatible (everything, if this cachedSubject's type is Subject.GROUP)
     *
     * @param subject The cachedSubject to copy from
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     */
    public void updateFromCachedSubject(@NotNull CachedSubject subject, @NotNull SubjectSupplier inheritanceSupplier){
        this.updateFromCachedSubject(subject, inheritanceSupplier, false);
    }

    /**
     * Updates this group with a cachedSubject's values with what is compatible (everything, if this cachedSubject's type is Subject.GROUP)
     *
     * @param subject The cachedSubject to copy from
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     * @param save Whether or not to save this change to the backend (files or sql)
     */
    public void updateFromCachedSubject(@NotNull CachedSubject subject, @NotNull SubjectSupplier inheritanceSupplier, boolean save){

        this.setIdentifier(subject.getIdentifier());

        this.setData(new GroupData(subject.getData()));
        if (dataManager != null) {
            this.getData().addUpdateListener("MAIN_LISTENER", (k, v) -> dataManager.updateSubjectData(this));
        }

        this.setOwnPermissions(subject.getPermissions());
        subject.getInheritances().forEach(i -> this.addInheritance(inheritanceSupplier.getSubject(i.getParent()), i.getContext()));

    }

    /**
     * Updates this group with a builder's values
     *
     * @param builder the builder to update from
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     */
    public void updateFromBuilder(@NotNull GroupBuilder builder, @NotNull SubjectSupplier inheritanceSupplier, boolean save) {
        this.updateFromCachedSubject(builder.toCachedSubject(), inheritanceSupplier, save);
    }

    /**
     * Checks if this group has this group permission
     *
     * @param permission The permission to check for
     * @return Whether this group has that permission
     */
    public boolean hasOwnPermission(@NotNull String permission) {
        return super.hasOwnPermission(permission);
    }

    /**
     * Checks if this group has this group permission (this one also checks for context equality and expiration equality before removing)
     *
     * @param permission The permission to check for
     * @return Whether this group has that permission
     */
    public boolean hasOwnPermission(@NotNull PPermission permission) {
        return super.hasOwnPermission(permission);
    }

    /**
     * Adds a group permission to this group
     *
     * @param permission the permission to add
     */
    public void addOwnPermission(@NotNull PPermission permission) {
        if(this.hasOwnPermission(permission)){
            return;
        }

        super.addOwnPermission(permission);

        //Update backend
        if (dataManager != null) {
            dataManager.addPermission(this, permission);
        }
    }

    /**
     * Removes a specific permission from this group
     *
     * @param permission the permission to remove
     */
    public void removeOwnPermission(@NotNull PPermission permission) {
        super.removeOwnPermission(permission.getPermission());

        //Update backend
        if (dataManager != null) {
            dataManager.removePermission(this, permission.getPermission());
        }
    }

    /**
     * Removes a specific permission from this group
     *
     * @param permission the permission to remove
     */
    public void removeOwnPermission(@NotNull String permission) {
        super.removeOwnPermission(permission);

        //Update backend
        if (dataManager != null) {
            dataManager.removePermission(this, permission);
        }
    }

    public ArrayList<Inheritance> getInheritances(){
        return super.getInheritances();
    }

    //Bulk Ops

    /**
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void addOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        ArrayList<PPermission> ps = (ArrayList<PPermission>)permissions.clone();
        permissions.forEach(p -> {
            if(!this.hasOwnPermission(p)){
                super.addOwnPermission(p);
            } else {
                ps.remove(p);
            }
        });

        //Update backend
        if (dataManager != null) {
            dataManager.addPermissions(this, new ImmutablePermissionList(ps));
        }
    }

    /**
     * Gets a list of all the permissions that are possessed by this group
     * @return
     */
    public ImmutablePermissionList getOwnPermissions(){
        return super.getPermissions();
    }

    /**
     * removes a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void removeOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        permissions.forEach(p -> super.removeOwnPermission(p));

        //Update backend
        if(dataManager != null) {
            ArrayList<String> perms = new ArrayList<>();
            permissions.forEach(p -> perms.add(p.getPermission()));
            dataManager.removePermissions(this, perms);
        }
    }

    /**
     * Checks whether this has the parent/inheritance specified
     * @param subject The parent/inheritance to check for
     * @return true if this user has the specified inheritance
     */
    public boolean hasInheritance(@NotNull Subject subject){
        for(Inheritance inheritance : super.getInheritances()){
            if(subject.equals(inheritance.getParent())){
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a parent/inheritance to this user
     * @param subject The inheritance to add
     * @param context The context that this will apply in
     */
    public void addInheritance(Subject subject, Context context){
        if(this.hasInheritance(subject)){
            return;
        }

        super.addInheritance(new SubjectRef(subject), context);

        if(dataManager != null){
            dataManager.addInheritance(this, subject, context);
        }
    }

    /**
     * Removes a parent/inheritance from this group
     * @param subject the parent to remove
     */
    public void removeInheritance(Subject subject){
        super.removeInheritance(subject);

        if(dataManager != null){
            dataManager.removeInheritance(this, subject.getIdentifier());
        }
    }

    /**
     * Clears all inheritances from this group
     */
    public void clearInheritances(){
        ArrayList<String> parents = new ArrayList<>();

        for(Inheritance i : super.getInheritances()){
            super.removeInheritance(i.getParent());
            parents.add(i.getParent().getIdentifier());
        }

        if(dataManager != null){
            dataManager.removeInheritances(this, parents);
        }
    }

    /**
     * Gets the name of this group
     *
     * @return
     */
    public String getName() {
        return this.getIdentifier();
    }

    /**
     * Gets the set chat colour of this group
     *
     * @return
     */
    public String getChatColour() {
        return this.getData().getChatColour();
    }

    /**
     * Gets the set prefix of this group
     *
     * @return
     */
    public String getPrefix() {
        return this.getData().getPrefix();
    }

    /**
     * Gets the set suffix of this group
     *
     * @return
     */
    public String getSuffix() {
        return this.getData().getSuffix();
    }

    /**
     * Gets the set description of this group
     *
     * @return
     */
    public String getDescription() {
        return this.getData().getDescription();
    }

    /**
     * Sets the chat colour of this group
     *
     * @param colour
     */
    public void setChatColour(@NotNull String colour) {
        this.getData().setChatColour(colour); //Automatically saved to data-manager
    }

    /**
     * Sets the description of this group
     *
     * @param description
     */
    public void setDescription(@NotNull String description) {
        this.getData().setDescription(description); //Automatically saved to data-manager
    }

    /**
     * Sets the prefix of this group
     *
     * @param prefix
     */
    public void setPrefix(@NotNull String prefix) {
        this.getData().setPrefix(prefix); //Automatically saved to data-manager
    }

    /**
     * Sets the suffix of this group
     *
     * @param suffix
     */
    public void setSuffix(@NotNull String suffix) {
        this.getData().setSuffix(suffix); //Automatically saved to data-manager
    }

    /**
     * Gets the inheritances of this group
     * @return
     */
    public ArrayList<Inheritance> getOwnInheritances(){
        return super.getInheritances();
    }

}

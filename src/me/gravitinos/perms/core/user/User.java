package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.util.SubjectSupplier;
import org.jetbrains.annotations.NotNull;

import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.UUID;

public class User extends Subject<UserData> {
    private DataManager dataManager;

    public User(UserBuilder builder, SubjectSupplier inheritanceSupplier){
        this(builder, inheritanceSupplier, null);
    }
    public User(UserBuilder builder, SubjectSupplier inheritanceSupplier, UserManager userManager) {
        this(builder.toCachedSubject(), inheritanceSupplier, userManager);
    }

    public User(CachedSubject subject, SubjectSupplier inheritanceSupplier, UserManager userManager){
        super(subject.getIdentifier(), Subject.USER, new UserData(subject.getData()));
        this.updateFromCachedSubject(subject, inheritanceSupplier, false);

        this.dataManager = userManager != null ? userManager.getDataManager() : null;

        if(dataManager != null){
            this.getData().addUpdateListener("MAIN_LISTENER", (k, v) -> dataManager.updateSubjectData(this));
        }
    }

    /**
     * Updates this user with a cachedSubject's values with what is compatible (everything, if this cachedSubject's type is Subject.USER)
     *
     * @param subject The cachedSubject to update with
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     */
    public void updateFromCachedSubject(CachedSubject subject, SubjectSupplier inheritanceSupplier){
        this.updateFromCachedSubject(subject, inheritanceSupplier, true);
    }

    /**
     * Updates this user with a cachedSubject's values with what is compatible (everything, if this cachedSubject's type is Subject.USER)
     *
     * @param subject The cachedSubject to update with
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     * @param save Whether or not to save this change to the backend (files or sql)
     */
    public void updateFromCachedSubject(CachedSubject subject, SubjectSupplier inheritanceSupplier, boolean save){
        this.setIdentifier(subject.getIdentifier());

        this.setData(new UserData(subject.getData()));
        if(dataManager != null){
            this.getData().addUpdateListener("MAIN_LISTENER", (k, v) -> dataManager.updateSubjectData(this));
        }

        this.setOwnPermissions(subject.getPermissions());
        subject.getInheritances().forEach(i -> this.addInheritance(inheritanceSupplier.getSubject(i.getParent()), i.getContext()));

        if(save && dataManager != null){
            dataManager.updateSubject(this);
        }
    }

    /**
     * Updates this user with a builder's values
     * @param builder The builder to build from
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     * @param save Whether or not to save this change to the backend (files or sql)
     */
    public void updateFromBuilder(UserBuilder builder, SubjectSupplier inheritanceSupplier, boolean save){
        this.updateFromCachedSubject(builder.toCachedSubject(), inheritanceSupplier, save);
    }


    /**
     * Gets the unique ID of this user
     * @return UUID
     */
    public UUID getUniqueID(){
        return UUID.fromString(this.getIdentifier());
    }

    /**
     * Gets the name of the display-group of this user
     * @return The name of the display-group of this user
     */
    public String getDisplayGroup(){
        return this.getData().getDisplayGroup();
    }

    /**
     * Sets the display-group of this user
     * @param displayGroup
     */
    public void setDisplayGroup(Group displayGroup){
        this.getData().setDisplayGroup(displayGroup.getName());
    }

    /**
     * Gets the set prefix of this user
     * @return The set prefix of this user
     */
    public String getPrefix(){
        return this.getData().getPrefix();
    }

    /**
     * Sets the prefix of this user
     * @param prefix The prefix to set to
     */
    public void setPrefix(String prefix){
        this.getData().setPrefix(prefix);
    }

    /**
     * Gets the set suffix of this user
     * @return The set suffix of this user
     */
    public String getSuffix(){
        return this.getData().getSuffix();
    }

    /**
     * sets the suffix of this user
     * @param suffix The suffix to set to
     */
    public void setSuffix(String suffix){
        this.getData().setSuffix(suffix);
    }

    /**
     * Gets the username of this user
     * @return The user's username
     */
    public String getName(){
        return this.getData().getName();
    }


    /**
     * Checks if this user has this user permission
     *
     * @param permission The permission to check for
     * @return Whether this user has that permission
     */
    public boolean hasOwnPermission(@NotNull String permission) {
        return super.hasOwnPermission(permission);
    }

    /**
     * Checks if this user has this user permission (this one also checks for context equality and expiration equality before removing)
     *
     * @param permission The permission to check for
     * @return Whether this user has that permission
     */
    public boolean hasOwnPermission(@NotNull PPermission permission) {
        return super.hasOwnPermission(permission);
    }

    /**
     * Adds a user permission to this group
     *
     * @param permission the permission to add
     */
    public void addOwnPermission(@NotNull PPermission permission) {
        super.addOwnPermission(permission);

        //Update backend
        if (dataManager != null) {
            dataManager.addPermission(this, permission);
        }
    }

    /**
     * Removes a specific permission from this user
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
     * Removes a specific permission from this user
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

    //Bulk Ops

    /**
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void addOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        permissions.forEach(p -> super.addOwnPermission(p));

        //Update backend
        if (dataManager != null) {
            dataManager.addPermissions(this, new ImmutablePermissionList(permissions));
        }
    }

    /**
     * removes a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void removeOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        permissions.forEach(p -> super.removeOwnPermission(p));

        //Update backend
        if(dataManager != null) {
            dataManager.removePermissions(this, new ImmutablePermissionList(permissions));
        }
    }


}

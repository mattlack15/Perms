package me.gravitinos.perms.core.group;

import com.google.common.base.Preconditions;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.*;
import me.gravitinos.perms.core.util.SubjectSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

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
        subject.getInheritances().forEach(i -> this.addOwnSubjectInheritance(inheritanceSupplier.getSubject(i.getParent()), i.getContext()));

    }

    /**
     * Gets the priority or weight of this group
     * @return
     */
    public int getPriority(){
        return this.getData().getPriority();
    }

    /**
     * Sets the priority of weight of this group
     * @param i
     */
    public void setPriority(int i){
        this.getData().setPriority(i); //Automatically saved to data-manager
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
    public boolean hasOwnPermission(@NotNull String permission, @NotNull Context context) {
        return super.hasOwnPermission(permission, context);
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
    public CompletableFuture<Void> addOwnPermission(@NotNull PPermission permission) {
        if(this.hasOwnPermission(permission)){
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }

        super.addOwnSubjectPermission(permission);

        //Update backend
        if (dataManager != null) {
            return dataManager.addPermission(this, permission);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Removes a specific permission from this group
     *
     * @param permission the permission to remove
     */
    public CompletableFuture<Void> removeOwnPermission(@NotNull PPermission permission) {
        PPermission perm = super.removeOwnSubjectPermission(permission);

        //Update backend
        if (dataManager != null) {
            return dataManager.removePermissionExact(this, permission.getPermission(), perm.getPermissionIdentifier());
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Removes a specific permission from this group
     *
     * @param permission the permission to remove
     */
    public CompletableFuture<Void> removeOwnPermission(@NotNull String permission) {
        super.removeOwnSubjectPermission(permission);

        //Update backend
        if (dataManager != null) {
            return dataManager.removePermission(this, permission);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    @Override
    public DataManager getDataManager() {
        return this.dataManager;
    }

    public ArrayList<Inheritance> getInheritances(){
        return super.getInheritances();
    }

    //Bulk Ops

    /**
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public CompletableFuture<Void> addOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        ArrayList<PPermission> ps = (ArrayList<PPermission>)permissions.clone();
        permissions.forEach(p -> {
            if(!this.hasOwnPermission(p)){
                super.addOwnSubjectPermission(p);
            } else {
                ps.remove(p);
            }
        });

        //Update backend
        if (dataManager != null) {
            return dataManager.addPermissions(this, new ImmutablePermissionList(ps));
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Gets if this group is applicable to this server
     * @return true or false
     */
    public boolean serverContextAppliesToThisServer(){
        return this.getServerContext().equals(GroupData.SERVER_GLOBAL) || this.getServerContext().equals(GroupData.SERVER_LOCAL);
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
    public CompletableFuture<Void> removeOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        permissions.forEach(p -> super.removeOwnSubjectPermission(p));

        //Update backend
        if(dataManager != null) {
            return dataManager.removePermissionsExact(this, permissions);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
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
    public CompletableFuture<Void> addInheritance(Subject subject, Context context){
        if(this.hasInheritance(subject)){
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }

        super.addOwnSubjectInheritance(new SubjectRef(subject), context);

        if(dataManager != null){
            return dataManager.addInheritance(this, subject, context);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Removes a parent/inheritance from this group
     * @param subject the parent to remove
     */
    public CompletableFuture<Void> removeInheritance(Subject subject){
        super.removeOwnSubjectInheritance(subject);

        if(dataManager != null){
            return dataManager.removeInheritance(this, subject.getIdentifier());
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Clears all inheritances from this group
     */
    public CompletableFuture<Void> clearInheritances(){
        ArrayList<String> parents = new ArrayList<>();

        for(Inheritance i : super.getInheritances()){
            super.removeOwnSubjectInheritance(i.getParent());
            parents.add(i.getParent().getIdentifier());
        }

        if(dataManager != null){
            return dataManager.removeInheritances(this, parents);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
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
     * Get the server in which this group applies (Custom or GroupData.SERVER_LOCAL or GroupData.SERVER_GLOBAL)
     * @return
     */
    public String getServerContext(){
        return this.getData().getServerContext();
    }

    /**
     * Set the server in which this group applies (Custom or GroupData.SERVER_LOCAL or GroupData.SERVER_GLOBAL)
     * @param context
     */
    public void setServerContext(String context){
        this.getData().setServerContext(context);
    } //Automatically saved to data-manager

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

}

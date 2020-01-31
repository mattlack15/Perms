package me.gravitinos.perms.core.user;

import com.google.common.collect.ImmutableList;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.*;
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
     * Gets the name of the display-group of this user (Custom or UserData.SERVER_LOCAL or UserData.SERVER_GLOBAL)
     * @param server The server context
     * @return The name of the display-group of this user
     */
    public String getDisplayGroup(String server){
        return this.getData().getDisplayGroup(server);
    }

    /**
     * Gets the name of the display-group of this user, defaults to the local server's display group
     * @return The name of the display-group of this user
     */
    public String getDisplayGroup(){
        ArrayList<Subject<?>> subjects = new ArrayList<>();
        this.getInheritances().forEach(i -> subjects.add(i.getParent()));

        Group highest = null;
        for (Subject<?> subject : subjects) {
            if(subject instanceof Group){
                if((highest == null || ((Group) subject).getPriority() > highest.getPriority()) && ((Group) subject).serverContextAppliesToThisServer()){
                    highest = (Group) subject;
                }
            }
        }
        if(highest == null) {
            this.addInheritance(GroupManager.instance.getDefaultGroup(), Context.CONTEXT_SERVER_LOCAL);
            highest = GroupManager.instance.getDefaultGroup();
        }

        this.setDisplayGroup(highest);

        return this.getData().getDisplayGroup(UserData.SERVER_LOCAL);
    }

    /**
     * Sets the display-group of this user
     * @param server The server in which this will apply on (Custom or UserData.SERVER_LOCAL or UserData.SERVER_GLOBAL)
     * @param displayGroup The display group
     */
    protected void setDisplayGroup(String server, Group displayGroup){
        this.getData().setDisplayGroup(server, displayGroup.getName());
    }
    /**
     * Sets the display-group of this user defaults to whatever server context the display group has
     * @param displayGroup The display group
     */
    protected void setDisplayGroup(Group displayGroup){
        this.setDisplayGroup(displayGroup.getServerContext(), displayGroup);
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
    public boolean hasOwnPermission(@NotNull String permission, @NotNull Context context) {
        return super.hasOwnPermission(permission, context);
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
    public PPermission removeOwnPermission(@NotNull PPermission permission) {
        PPermission p = super.removeOwnPermission(permission);

        //Update backend
        if (dataManager != null) {
            dataManager.removePermissionExact(this, p.getPermission(), p.getPermissionIdentifier());
        }
        return p;
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
            dataManager.removePermissionsExact(this, permissions);
        }
    }

    /**
     * Gets a list of all the permissions that are possessed by this user
     * @return
     */
    public ImmutablePermissionList getOwnPermissions(){
        return super.getPermissions();
    }

    @Override
    public DataManager getDataManager() {
        return this.dataManager;
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
     * Removes a parent/inheritance from this user
     * @param subject the parent to remove
     */
    public void removeInheritance(Subject subject){
        super.removeInheritance(subject);

        if(dataManager != null){
            dataManager.removeInheritance(this, subject.getIdentifier());
        }
    }

    public ArrayList<Inheritance> getInheritances(){
        ArrayList<Inheritance> inheritances = super.getInheritances();
        if(inheritances.size() == 0){
            this.addInheritance(GroupManager.instance.getDefaultGroup(), Context.CONTEXT_SERVER_LOCAL);
        }
        return super.getInheritances();
    }


    /**
     * Clears all local inheritances
     */
    public void clearInheritancesLocal(){
        ArrayList<String> parents = new ArrayList<>();

        for(Inheritance i : super.getInheritances()){
            if(i.getContext().getServerName().equals(UserData.SERVER_LOCAL)){
                continue;
            }
            super.removeInheritance(i.getParent());
            parents.add(i.getParent().getIdentifier());
        }

        if(dataManager != null){
            dataManager.removeInheritances(this, parents);
        }
    }


    /**
     * Clears ALL inheritances from this user
     */
    public void clearInheritancesGlobal(){
        ArrayList<String> parents = new ArrayList<>();

        for(Inheritance i : super.getInheritances()){
            super.removeInheritance(i.getParent());
            parents.add(i.getParent().getIdentifier());
        }

        if(dataManager != null){
            dataManager.removeInheritances(this, parents);
        }
    }


}

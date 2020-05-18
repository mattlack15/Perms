package me.gravitinos.perms.core.group;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.subject.*;
import me.gravitinos.perms.core.util.SubjectSupplier;
import me.gravitinos.perms.spigot.messaging.MessageManager;
import me.gravitinos.perms.spigot.messaging.MessageReloadSubject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Group extends Subject<GroupData> {
    private DataManager dataManager;

    private volatile boolean updatingData = false;

    public Group(GroupBuilder builder, SubjectSupplier inheritanceSupplier) {
        this(builder, inheritanceSupplier, null);
    }

    public Group(@NotNull CachedSubject cachedSubject, @NotNull SubjectSupplier inheritanceSupplier, GroupManager manager) {
        super(cachedSubject.getSubjectId(), Subject.GROUP, new GroupData(cachedSubject.getData()));
        this.dataManager = manager != null ? manager.getDataManager() : null;
        this.updateFromCachedSubject(cachedSubject, inheritanceSupplier, false);
    }

    public Group(@NotNull GroupBuilder builder, @NotNull SubjectSupplier inheritanceSupplier, GroupManager manager) {
        this(builder.toCachedSubject(), inheritanceSupplier, manager);
    }

    /**
     * Updates this group with a cachedSubject's values with what is compatible (everything, if this cachedSubject's type is Subject.GROUP)
     * By default, this does not save to the backend
     *
     * @param subject             The cachedSubject to copy from
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     * @see #updateFromCachedSubject(CachedSubject, SubjectSupplier, boolean)
     */
    public void updateFromCachedSubject(@NotNull CachedSubject subject, @NotNull SubjectSupplier inheritanceSupplier) {
        this.updateFromCachedSubject(subject, inheritanceSupplier, false);
    }

    /**
     * Updates this group with a cachedSubject's values with what is compatible (everything, if this cachedSubject's type is Subject.GROUP)
     *
     * @param subject             The cachedSubject to copy from
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     * @param save                Whether or not to save this change to the backend (files or sql)
     */
    public void updateFromCachedSubject(@NotNull CachedSubject subject, @NotNull SubjectSupplier inheritanceSupplier, boolean save) {

        this.setSubjectId(subject.getSubjectId());

        this.setData(new GroupData(subject.getData()));

        this.getData().addUpdateListener("MAIN_LISTENER", (k, v) -> {
            if (dataManager != null) {
                synchronized (this) {
                    if (!updatingData) {
                        updatingData = true;
                        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                            try {
                                Thread.sleep(2); //Saves on database requests, so that it will update all of the changed subject data (if a thread makes more than one change) in one update
                                dataManager.updateSubjectData(this).get();
                                if(MessageManager.instance != null){
                                    MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                            updatingData = false;
                        });
                    }
                }
            }
        });


        super.setOwnPermissions(subject.getPermissions());
        ArrayList<Inheritance> inheritances = Lists.newArrayList(super.getInheritances());
        inheritances.forEach(i -> super.removeOwnSubjectInheritance(i.getParent()));
        subject.getInheritances().forEach(i -> super.addOwnSubjectInheritance(inheritanceSupplier.getSubject(i.getParent()), i.getContext()));

        if (save && dataManager != null) {
            dataManager.updateSubject(this);
        }
    }

    /**
     * Gets the priority or weight of this group
     *
     * @return
     */
    public int getPriority() {
        return this.getData().getPriority();
    }

    /**
     * Sets the priority of weight of this group
     *
     * @param i
     */
    public void setPriority(int i) {
        this.getData().setPriority(i); //Automatically saved to data-manager
    }

    /**
     * Updates this group with a builder's values
     *
     * @param builder             the builder to update from
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
    public boolean hasOwnPermission(@NotNull String permission, @NotNull ContextSet context) {
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
        if (this.hasOwnPermission(permission)) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }

        super.addOwnSubjectPermission(permission);

        //Update backend
        if (dataManager != null) {
            CompletableFuture<Void> future = dataManager.addPermission(this, permission);
            if (MessageManager.instance != null)
                PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
                });
            return future;
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
            CompletableFuture<Void> future = dataManager.removePermissionExact(this, permission);
            if (MessageManager.instance != null)
                PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
                });
            return future;
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
            CompletableFuture<Void> future = dataManager.removePermission(this, permission);
            if (MessageManager.instance != null)
                PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
                });
            return future;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    @Override
    public DataManager getDataManager() {
        return this.dataManager;
    }

    public ArrayList<Inheritance> getInheritances() {
        return super.getInheritances();
    }

    //Bulk Ops

    /**
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public CompletableFuture<Void> addOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        ArrayList<PPermission> ps = (ArrayList<PPermission>) permissions.clone();
        permissions.forEach(p -> {
            if (!this.hasOwnPermission(p)) {
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
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public CompletableFuture<Void> addInheritances(@NotNull ArrayList<Inheritance> inheritances) {
        ArrayList<Inheritance> ps = Lists.newArrayList(inheritances);
        inheritances.forEach(p -> {
            if (!this.getInheritances().contains(p)) {
                super.addOwnInheritance(p);
            } else {
                ps.remove(p);
            }
        });

        //Update backend
        if (dataManager != null) {
            return dataManager.addInheritances(ps);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Gets if this group is applicable to this server
     *
     * @return true or false
     */
    public boolean serverContextAppliesToThisServer() {
        return this.getContext().appliesToAny(Context.CONTEXT_SERVER_LOCAL);
    }

    /**
     * Gets a list of all the permissions that are possessed by this group
     *
     * @return
     */
    public ImmutablePermissionList getOwnPermissions() {
        return super.getPermissions();
    }

    /**
     * removes a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public CompletableFuture<Void> removeOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        permissions.forEach(p -> super.removeOwnSubjectPermission(p));

        //Update backend
        if (dataManager != null) {
            return dataManager.removePermissionsExact(this, permissions);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * removes a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public CompletableFuture<Void> removeOwnPermissionsStr(@NotNull ArrayList<String> permissions) {
        ArrayList<PPermission> permissions1 = new ArrayList<>();

        permissions.forEach(p1 -> this.getOwnPermissions().forEach(p -> {
                    if (p.getPermission().equalsIgnoreCase(p1)) {
                        permissions1.add(p);
                    }
                })
        );

        return this.removeOwnPermissions(permissions1);
    }


    /**
     * Checks whether this has the parent/inheritance specified
     *
     * @param subject The parent/inheritance to check for
     * @return true if this user has the specified inheritance
     */
    public boolean hasInheritance(@NotNull Subject subject) {
        if (this.equals(subject)) {
            return true;
        }
        for (Inheritance inheritance : super.getInheritances()) {
            if (subject.equals(inheritance.getParent())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a parent/inheritance to this user
     *
     * @param subject The inheritance to add
     * @param context The context that this will apply in
     */
    public CompletableFuture<Void> addInheritance(Subject<?> subject, ContextSet context) {
        if (this.hasInheritance(subject)) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }

        super.addOwnSubjectInheritance(new SubjectRef(subject), context);

        if (dataManager != null) {
            CompletableFuture<Void> future = dataManager.addInheritance(new CachedInheritance(this.getSubjectId(), subject.getSubjectId(), this.getType(), subject.getType(), context));
            if (MessageManager.instance != null)
                PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
                });
            return future;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Removes a parent/inheritance from this group
     *
     * @param subject the parent to remove
     */
    public CompletableFuture<Void> removeInheritance(Subject<?> subject) {
        super.removeOwnSubjectInheritance(subject);

        if (dataManager != null) {
            CompletableFuture<Void> future = dataManager.removeInheritance(this, subject.getSubjectId());
            if (MessageManager.instance != null)
                PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
                });
            return future;        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Clears all inheritances from this group
     */
    public CompletableFuture<Void> clearInheritances() {
        ArrayList<UUID> parents = new ArrayList<>();

        for (Inheritance i : super.getInheritances()) {
            super.removeOwnSubjectInheritance(i.getParent());
            parents.add(i.getParent().getSubjectId());
        }

        if (dataManager != null) {
            return dataManager.removeInheritances(this, parents);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Gets all permissions, including inherited
     */
    public ArrayList<PPermission> getAllPermissions() {
        return super.getAllPermissions();
    }

    /**
     * Gets all permissions, including inherited
     */
    public ArrayList<PPermission> getAllPermissions(ContextSet contexts) {
        return super.getAllPermissions(contexts);
    }

    /**
     * Checks if this group has a permission of its own or inherits a permission
     */
    public boolean hasPermission(String permission, ContextSet context) {
        ArrayList<PPermission> permissions = this.getAllPermissions(context);
        for (PPermission perms : permissions) {
            if (perms.getPermission().equalsIgnoreCase(permission) && context.isSatisfiedBy(perms.getContext())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this group has a permission of its own or inherits a permission
     */
    public boolean hasPermission(PPermission perm) {
        return this.getAllPermissions().contains(perm);
    }

    /**
     * Gets the name of this group
     *
     * @return
     */
    public String getName() {
        return super.getName();
    }

    /**
     * Sets the name of this group
     *
     * @param name The name to set to
     */
    public void setName(String name) {
        super.setName(name);
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
     * Get the id of the server in which this group applies (Custom or GroupData.SERVER_LOCAL or GroupData.SERVER_GLOBAL)
     *
     * @return The ID of the server in which this group applies
     */
    public ContextSet getContext() {
        return this.getData().getContext();
    }

    /**
     * Utility function
     * Returns whether or not this group is a fully global group
     */
    public boolean isGlobal(){
        return this.getContext().filterByKey(Context.SERVER_IDENTIFIER).size() == 0;
    }

    /**
     * Set the context in which this group applies (Custom or new MutableContextSet(Context.CONTEXT_SERVER_LOCAL or Context.CONTEXT_SERVER_GLOBAL))
     *
     * @param context
     * @return Whether it was successful, it could be unsuccessful if a group already exists with that server context and this group name
     */
    public boolean setContext(ContextSet context) {
        return this.getData().setContext(context, false);
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

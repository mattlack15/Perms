package me.gravitinos.perms.core.user;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedInheritance;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.context.ServerContextType;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.*;
import me.gravitinos.perms.core.util.SubjectSupplier;
import me.gravitinos.perms.spigot.messaging.MessageManager;
import me.gravitinos.perms.spigot.messaging.MessageReloadSubject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class User extends Subject<UserData> {
    private DataManager dataManager;

    private volatile boolean updatingData = false;

    public User(UserBuilder builder, SubjectSupplier inheritanceSupplier) {
        this(builder, inheritanceSupplier, null);
    }

    public User(UserBuilder builder, SubjectSupplier inheritanceSupplier, UserManager userManager) {
        this(builder.toCachedSubject(), inheritanceSupplier, userManager);
    }

    public User(CachedSubject subject, SubjectSupplier inheritanceSupplier, UserManager userManager) {
        super(subject.getSubjectId(), Subject.USER, new UserData(subject.getData()));
        this.dataManager = userManager != null ? userManager.getDataManager() : null;
        this.updateFromCachedSubject(subject, inheritanceSupplier, false);
    }

    /**
     * Updates this user with a cachedSubject's values with what is compatible (everything, if this cachedSubject's type is Subject.USER)
     * This does not save the user to the backend
     *
     * @param subject             The cachedSubject to update with
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     * @see #updateFromCachedSubject(CachedSubject, SubjectSupplier, boolean)
     */
    public void updateFromCachedSubject(CachedSubject subject, SubjectSupplier inheritanceSupplier) {
        this.updateFromCachedSubject(subject, inheritanceSupplier, false);
    }

    /**
     * Updates this user with a cachedSubject's values with what is compatible (everything, if this cachedSubject's type is Subject.USER)
     *
     * @param subject             The cachedSubject to update with
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     * @param save                Whether or not to save this change to the backend (files or sql)
     */
    public void updateFromCachedSubject(CachedSubject subject, SubjectSupplier inheritanceSupplier, boolean save) {

        this.setSubjectId(subject.getSubjectId());

        this.setData(new UserData(subject.getData()));
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
     * Updates this user with a builder's values
     *
     * @param builder             The builder to build from
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     * @param save                Whether or not to save this change to the backend (files or sql)
     */
    public void updateFromBuilder(UserBuilder builder, SubjectSupplier inheritanceSupplier, boolean save) {
        this.updateFromCachedSubject(builder.toCachedSubject(), inheritanceSupplier, save);
    }


    /**
     * Gets the unique ID of this user
     *
     * @return UUID
     */
    public UUID getUniqueID() {
        return this.getSubjectId();
    }

    /**
     * Gets the name of the display-group of this user (Custom or UserData.SERVER_LOCAL or UserData.SERVER_GLOBAL)
     *
     * @param server The server context
     * @return The name of the display-group of this user
     */
    public UUID getDisplayGroup(int server) {
        return this.getData().getDisplayGroup(server);
    }

    /**
     * Gets the name of the display-group of this user, defaults to the local server's display group
     *
     * @return The name of the display-group of this user
     */
    public UUID getDisplayGroup() {
        ArrayList<Subject<?>> subjects = new ArrayList<>();
        this.getInheritances().forEach(i -> {
            if (i.getContext().appliesToAny(Context.CONTEXT_SERVER_LOCAL)) {
                subjects.add(i.getParent());
            }
        });

        Group highest = null;
        for (Subject<?> subject : subjects) {
            if (subject instanceof Group) {
                if ((highest == null || ((Group) subject).getPriority() > highest.getPriority()) && ((Group) subject).serverContextAppliesToThisServer()) {
                    highest = (Group) subject;
                }
            }
        }
        if (highest == null) {
            highest = GroupManager.instance.getDefaultGroup();
        }

        if (!highest.getSubjectId().equals(this.getDisplayGroup(UserData.SERVER_LOCAL))) {
            this.setDisplayGroup(highest);
        }

        return this.getData().getDisplayGroup(UserData.SERVER_LOCAL);
    }

    /**
     * Gets the groups in the inheritance of this user in the order of highest priority
     */
    public ArrayList<Group> getGroupsInOrderOfPriority() {
        ArrayList<Group> groups = new ArrayList<>();
        for (Inheritance inheritance : getInheritances()) {
            if (inheritance.getParent() instanceof Group) {
                groups.add((Group) inheritance.getParent());
            }
        }
        groups.sort(Comparator.comparingInt(Group::getPriority));
        return groups;
    }

    /**
     * Sets the display-group of this user
     *
     * @param server       The server in which this will apply on (Custom or UserData.SERVER_LOCAL or UserData.SERVER_GLOBAL)
     * @param displayGroup The display group
     */
    protected void setDisplayGroup(int server, Group displayGroup) {
        this.getData().setDisplayGroup(server, displayGroup.getSubjectId());
    }

    /**
     * Sets the display-group of this user defaults to whatever server context the display group has
     *
     * @param displayGroup The display group
     */
    protected void setDisplayGroup(Group displayGroup) {
        this.setDisplayGroup(GroupData.SERVER_LOCAL, displayGroup);
    }

    /**
     * Gets the set prefix of this user
     *
     * @return The set prefix of this user
     */
    public String getPrefix() {
        return this.getData().getPrefix();
    }

    /**
     * Sets the prefix of this user
     *
     * @param prefix The prefix to set to
     */
    public void setPrefix(String prefix) {
        this.getData().setPrefix(prefix);
    }

    /**
     * Gets the set suffix of this user
     *
     * @return The set suffix of this user
     */
    public String getSuffix() {
        return this.getData().getSuffix();
    }

    /**
     * sets the suffix of this user
     *
     * @param suffix The suffix to set to
     */
    public void setSuffix(String suffix) {
        this.getData().setSuffix(suffix);
    }

    /**
     * Gets the username of this user
     *
     * @return The user's username
     */
    public String getName() {
        return this.getData().getName();
    }


    /**
     * Checks if this user has this user permission
     *
     * @param permission The permission to check for
     * @return Whether this user has that permission
     */
    public boolean hasOwnPermission(@NotNull String permission, @NotNull ContextSet context) {
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
                MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
            return future;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Removes a specific permission from this user
     *
     * @param permission the permission to remove
     */
    public CompletableFuture<Void> removeOwnPermission(@NotNull PPermission permission) {
        PPermission p = super.removeOwnSubjectPermission(permission);

        //Update backend
        if (dataManager != null) {
            CompletableFuture<Void> future = dataManager.removePermissionExact(this, p);

            if (MessageManager.instance != null)
                MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
            return future;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Removes a specific permission from this user
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

    //Bulk Ops

    /**
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public CompletableFuture<Void> addOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        permissions.forEach(p -> super.addOwnSubjectPermission(p));

        //Update backend
        if (dataManager != null) {
            return dataManager.addPermissions(this, new ImmutablePermissionList(permissions));
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
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
     * Gets a list of all the permissions that are possessed by this user
     *
     * @return
     */
    public ImmutablePermissionList getOwnPermissions() {
        return super.getPermissions();
    }

    @Override
    public ContextSet getContext() {
        return new MutableContextSet();
    }

    @Override
    public DataManager getDataManager() {
        return this.dataManager;
    }

    /**
     * Checks whether this has the parent/inheritance specified
     *
     * @param subject The parent/inheritance to check for
     * @return true if this user has the specified inheritance
     */
    public boolean hasInheritance(@NotNull Subject<?> subject) {
        for (Inheritance inheritance : super.getInheritances()) {
            if (subject.equals(inheritance.getParent())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInheritance(Subject<?> subject, ContextSet context) {
        for (Inheritance inheritance : super.getInheritances()) {
            if (subject.equals(inheritance.getParent()) && inheritance.getContext().equals(context)) {
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
    public CompletableFuture<Void> addInheritance(@NotNull Subject<?> subject, @NotNull ContextSet context) {

        if (this.hasInheritance(subject, context)) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }

        super.addOwnSubjectInheritance(new SubjectRef(subject), context);

        if (dataManager != null) {
            CompletableFuture<Void> future = dataManager.addInheritance(new CachedInheritance(this.getSubjectId(), subject.getSubjectId(), this.getType(), subject.getType(), context));
            if (MessageManager.instance != null)
                MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
            return future;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Removes a parent/inheritance from this user
     *
     * @param subject the parent to remove
     */
    public CompletableFuture<Void> removeInheritance(@NotNull Subject<?> subject) {
        super.removeOwnSubjectInheritance(subject);

        if (dataManager != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                try {
                    dataManager.removeInheritance(this, subject.getSubjectId()).get();
                    CompletableFuture<Void> future1 = addDefaultGroupIfNoInheritances();
                    if(future1 != null)
                        future1.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                future.complete(null);
            });
            if (MessageManager.instance != null)
                MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
            return future;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    @Nullable
    public CompletableFuture<Void> addDefaultGroupIfNoInheritances(){
        for(Inheritance inheritance : super.getInheritances()){
            if(inheritance.getContext().appliesToAny(Context.CONTEXT_SERVER_LOCAL)){
                return null;
            }
        }
        return this.addInheritance(GroupManager.instance.getDefaultGroup(), UserManager.instance.getDefaultGroupInheritanceContext());
    }

    public ArrayList<Inheritance> getInheritances() {
        return super.getInheritances();
    }


    /**
     * Clears all local inheritances
     */
    public CompletableFuture<Void> clearInheritancesLocal() {
        ArrayList<UUID> parents = new ArrayList<>();

        for (Inheritance i : super.getInheritances()) {
            if (!ServerContextType.getType(i.getContext()).equals(ServerContextType.LOCAL)) {
                continue;
            }
            super.removeOwnSubjectInheritance(i.getParent());
            parents.add(i.getParent().getSubjectId());
        }

        if (dataManager != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                try {
                    dataManager.removeInheritances(this, parents).get();
                    CompletableFuture<Void> future1 = addDefaultGroupIfNoInheritances();
                    if(future1 != null)
                        future1.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                future.complete(null);
            });
            return future;
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
     * Clears all global inheritances
     */
    public CompletableFuture<Void> clearInheritancesGlobal() {
        ArrayList<UUID> parents = new ArrayList<>();

        for (Inheritance i : super.getInheritances()) {
            if (i.getContext().filterByKey(Context.SERVER_IDENTIFIER).getContexts().size() == 0) {
                continue;
            }
            super.removeOwnSubjectInheritance(i.getParent());
            parents.add(i.getParent().getSubjectId());
        }

        if (dataManager != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                try {
                    dataManager.removeInheritances(this, parents).get();
                    CompletableFuture<Void> future1 = addDefaultGroupIfNoInheritances();
                    if(future1 != null)
                        future1.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                future.complete(null);
            });
            return future;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    /**
     * Checks if this user has a permission of its own or inherits a permission
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
     * Checks if this user has a permission of its own or inherits a permission
     */
    public boolean hasPermission(PPermission perm) {
        return this.getAllPermissions().contains(perm);
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
     * Clears ALL inheritances from this user
     */
    public CompletableFuture<Void> clearInheritances() {
        ArrayList<UUID> parents = new ArrayList<>();

        for (Inheritance i : super.getInheritances()) {
            super.removeOwnSubjectInheritance(i.getParent());
            parents.add(i.getParent().getSubjectId());
        }

        if (dataManager != null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                try {
                    dataManager.removeInheritances(this, parents).get();
                    CompletableFuture<Void> future1 = addDefaultGroupIfNoInheritances();
                    if(future1 != null)
                        future1.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                future.complete(null);
            });
            return future;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }


}

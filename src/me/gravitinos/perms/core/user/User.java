package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class User extends Subject<UserData> {
    private DataManager dataManager;

    private final AtomicBoolean updatingData = new AtomicBoolean(false);

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
    public synchronized void updateFromCachedSubject(CachedSubject subject, SubjectSupplier inheritanceSupplier, boolean save) {

        this.setSubjectId(subject.getSubjectId());

        this.setData(new UserData(subject.getData()));
        this.getData().addUpdateListener("MAIN_LISTENER", (k, v) -> {
            if (dataManager != null) {
                if (updatingData.compareAndSet(false, true)) {
                    PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                        try {
                            Thread.sleep(5); //Saves on database requests, so that it will update all of the changed subject data (if a thread makes more than one change) in one update
                            updatingData.set(false);
                            dataManager.updateSubjectData(this).get();
                            if (MessageManager.instance != null) {
                                MessageManager.instance.queueMessage(new MessageReloadSubject(this.getSubjectId()));
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            updatingData.compareAndSet(true, false);
                        }
                    });
                }
            }
        });

        super.setOwnPermissions(subject.getPermissions(), save);
        List<Inheritance> inheritanceList = new ArrayList<>();
        subject.getInheritances().forEach(i -> inheritanceList.add(new Inheritance(inheritanceSupplier.getSubject(i.getParent()), new SubjectRef(this), i.getContext())));
        super.getOwnLoggedInheritances().set(inheritanceList, save);

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
    public List<Group> getGroupsInOrderOfPriority() {
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

    //Bulk Ops

    /**
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void addPermissions(@NotNull ArrayList<PPermission> permissions) {
        permissions.forEach(super::addPermission);
    }

    /**
     * removes a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void removeOwnPermissions(@NotNull ArrayList<PPermission> permissions) {
        permissions.forEach(super::removePermission);
    }

    /**
     * Gets a list of all the permissions that are possessed by this user
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
     * Removes a parent/inheritance from this user
     *
     * @param subject the parent to remove
     */
    public void removeInheritance(@NotNull Subject<?> subject) {
        super.removeOwnSubjectInheritance(subject);
        addDefaultGroupIfNoInheritances();
    }

    public void addDefaultGroupIfNoInheritances() {
        for (Inheritance inheritance : super.getInheritances()) {
            if (inheritance.getContext().appliesToAny(Context.CONTEXT_SERVER_LOCAL)) {
                return;
            }
        }
        this.addInheritance(GroupManager.instance.getDefaultGroup(), GroupManager.instance.getDefaultGroup().getContext());
    }

    /**
     * Clears all local inheritances
     */
    public void clearInheritancesLocal() {
        for (Inheritance i : super.getInheritances()) {
            if (!ServerContextType.getType(i.getContext()).equals(ServerContextType.LOCAL)) {
                continue;
            }
            super.removeOwnSubjectInheritance(i.getParent());
        }
    }

    /**
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void addInheritances(@NotNull List<Inheritance> inheritances) {
        inheritances.forEach(p -> {
            if (!this.getInheritances().contains(p)) {
                super.addInheritance(p);
            }
        });
    }

    /**
     * Clears all global inheritances
     */
    public void clearInheritancesGlobal() {
        for (Inheritance i : super.getInheritances()) {
            if (i.getContext().filterByKey(Context.SERVER_IDENTIFIER).getContexts().size() != 0) {
                continue;
            }
            super.removeOwnSubjectInheritance(i.getParent());
        }
    }

    /**
     * Clears ALL inheritances from this user
     */
    public void clearInheritances() {
        for (Inheritance i : super.getInheritances()) {
            super.removeOwnSubjectInheritance(i.getParent());
        }
    }


}

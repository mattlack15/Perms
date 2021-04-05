package me.gravitinos.perms.core.group;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.cache.CachedSubject;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.subject.*;
import me.gravitinos.perms.core.util.SubjectSupplier;
import me.gravitinos.perms.spigot.messaging.MessageManager;
import me.gravitinos.perms.spigot.messaging.MessageReloadSubject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Group extends Subject<GroupData> {
    private final DataManager dataManager;

    private final AtomicBoolean updatingData = new AtomicBoolean(false);

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

    @Override
    public DataManager getDataManager() {
        return this.dataManager;
    }

    //Bulk Ops

    /**
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void addOwnPermissions(@NotNull List<PPermission> permissions) {
        permissions.forEach(super::addPermission);
    }

    /**
     * Adds a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void addInheritances(@NotNull List<Inheritance> inheritances) {
        inheritances.forEach(super::addInheritance);
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
     * removes a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void removeOwnPermissions(@NotNull List<PPermission> permissions) {
        permissions.forEach(super::removePermission);
    }

    /**
     * removes a lot of permissions in bulk, please use this for large amounts of permissions as Transfers to SQL can be a lot quicker
     */
    public void removeOwnPermissionsStr(@NotNull List<String> permissions) {
        List<PPermission> permissions1 = new ArrayList<>();

        permissions.forEach(p1 -> this.getPermissions().forEach(p -> {
                    if (p.getPermission().equalsIgnoreCase(p1)) {
                        permissions1.add(p);
                    }
                })
        );

        this.removeOwnPermissions(permissions1);
    }


    /**
     * Checks whether this has the parent/inheritance specified
     *
     * @param subject The parent/inheritance to check for
     * @return true if this user has the specified inheritance
     */
    public boolean hasInheritance(@NotNull Subject<?> subject) {
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
    public void addInheritance(Subject<?> subject, ContextSet context) {
        if (this.hasInheritance(subject)) {
            return;
        }

        super.addInheritance(new SubjectRef(subject), context);
    }

    /**
     * Removes a parent/inheritance from this group
     *
     * @param subject the parent to remove
     */
    public void removeInheritance(Subject<?> subject) {
        super.removeOwnSubjectInheritance(subject);
    }

    /**
     * Clears all inheritances from this group
     */
    public void clearInheritances() {

        for (Inheritance i : super.getInheritances()) {
            super.removeOwnSubjectInheritance(i.getParent());
        }
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

    public boolean isGodLocked() {
        return this.getData().isGodLocked();
    }

    public void setGodLocked(boolean value) {
        this.getData().setGodLocked(value);
    }

    /**
     * Sets the combined ID and Data of the Icon of this group<br>
     * You can get the combinedId by doing:<br>
     * combined = id << 4 | data
     */
    public void setIconCombinedId(int combinedId) {
        this.getData().setIconCombinedId(combinedId);
    }

    /**
     * Gets the combined ID and Data of the Icon of this group<br>
     * You can get the ID by doing combined >> 4 or combined >>> 4<br>
     * You can get the Data by doing combined & 15
     */
    public int getIconCombinedId() {
        return this.getData().getIconCombinedId();
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
    public boolean isGlobal() {
        return this.getContext().size() == 0;
    }

    /**
     * Set the context in which this group applies (Custom or new MutableContextSet(Context.CONTEXT_SERVER_LOCAL or Context.CONTEXT_SERVER_GLOBAL))
     *
     * @param context
     * @return Whether it was successful, it could be unsuccessful if a group already exists with that server context and this group name
     */
    public synchronized boolean setContext(ContextSet context) {
        return this.getData().setContext(context, false);
    } //Automatically saved to data-manager

    /**
     * Sets the chat colour of this group
     *
     * @param colour
     */
    public synchronized void setChatColour(@NotNull String colour) {
        this.getData().setChatColour(colour); //Automatically saved to data-manager
    }

    /**
     * Sets the description of this group
     *
     * @param description
     */
    public synchronized void setDescription(@NotNull String description) {
        this.getData().setDescription(description); //Automatically saved to data-manager
    }

    /**
     * Sets the prefix of this group
     *
     * @param prefix
     */
    public synchronized void setPrefix(@NotNull String prefix) {
        this.getData().setPrefix(prefix); //Automatically saved to data-manager
    }

    /**
     * Sets the suffix of this group
     *
     * @param suffix
     */
    public synchronized void setSuffix(@NotNull String suffix) {
        this.getData().setSuffix(suffix); //Automatically saved to data-manager
    }

}

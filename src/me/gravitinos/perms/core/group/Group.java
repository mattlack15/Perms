package me.gravitinos.perms.core.group;

import com.google.common.base.Preconditions;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.util.SubjectSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Group extends Subject<GroupData> {
    private DataManager dataManager;

    public Group(GroupBuilder builder, SubjectSupplier inheritanceSupplier) {
        this(builder, inheritanceSupplier, null);
    }

    public Group(GroupBuilder builder, SubjectSupplier inheritanceSupplier, GroupManager manager) {
        super(builder.getName(), Subject.GROUP, builder.getData());
        this.updateFromBuilder(builder, inheritanceSupplier);

        this.dataManager = manager != null ? manager.getDataManager() : null;

        if (dataManager != null) {
            this.getData().addUpdateListener((k, v) -> dataManager.updateSubjectData(this));
        }

    }

    /**
     * Updates this group with a builder's values
     *
     * @param builder             the builder to update from
     * @param inheritanceSupplier Inheritance supplier to handle getting inherited subject objects usually just groupManager::getGroup
     */
    public void updateFromBuilder(GroupBuilder builder, SubjectSupplier inheritanceSupplier) {

        this.setOwnPermissions(builder.getPermissions());

        builder.getInherited().forEach(i -> this.addInheritance(inheritanceSupplier.getSubject(i.getParent()), i.getContext()));
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
        this.getData().setChatColour(colour);
    }

    /**
     * Sets the description of this group
     *
     * @param description
     */
    public void setDescription(@NotNull String description) {
        this.getData().setDescription(description);
    }

    /**
     * Sets the prefix of this group
     *
     * @param prefix
     */
    public void setPrefix(@NotNull String prefix) {
        this.getData().setPrefix(prefix);
    }

    /**
     * Sets the suffix of this group
     *
     * @param suffix
     */
    public void setSuffix(@NotNull String suffix) {
        this.getData().setSuffix(suffix);
    }

}

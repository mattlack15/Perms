package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.ImmutableContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Immutable representation of a permission node
 */
public final class PPermission {
    private final String permission;
    private final ContextSet contextSet;
    private UUID permissionIdentifier = UUID.randomUUID();

    public PPermission(@NotNull String permission, @NotNull ContextSet contextSet) {
        this.contextSet = contextSet;
        this.permission = permission.toLowerCase();
    }

    public PPermission(@NotNull String permission, @NotNull ContextSet contextSet, @NotNull UUID permissionIdentifier) {
        this(permission, contextSet);
        this.permissionIdentifier = permissionIdentifier;
    }


    public PPermission(@NotNull String permission) {
        this(permission, new MutableContextSet(Context.CONTEXT_SERVER_LOCAL));
    }

    public UUID getPermissionIdentifier() {
        return permissionIdentifier;
    }

    /**
     * Gets the actual permission represented by this permission object
     *
     * @return Permission in string form
     */
    public String getPermission() {
        return this.permission;
    }

    /**
     * Check if this permission is expired
     *
     * @param currentTimeMs the System.currentTimeMillis() value
     * @return Whether this permission is expired
     */
    public boolean isExpired(long currentTimeMs) {
        return this.contextSet.isExpired(currentTimeMs);
    }

    /**
     * Gets the expiration of this permission
     *
     * @return The expiration in ms since epoch
     */
    public long getExpiry() {
        return this.contextSet.getExpiration();
    }

    /**
     * Get the contexts where this permission applies
     *
     * @return The contextSet
     */
    public ImmutableContextSet getContext() {
        return new ImmutableContextSet(this.contextSet);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof PPermission) {
            if (((PPermission) o).getPermissionIdentifier().equals(this.getPermissionIdentifier())) {
                return true;
            }
            if (((PPermission) o).getContext().equals(this.getContext())) {
                if (((PPermission) o).permission.equalsIgnoreCase(this.permission)) {
                    return true;
                }
            }
        }
        return false;
    }

}

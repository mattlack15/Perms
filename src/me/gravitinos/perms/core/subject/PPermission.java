package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.context.Context;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Immutable representation of a permission node
 */
public final class PPermission {
    private final String permission;
    private final Context context;
    private UUID permissionIdentifier = UUID.randomUUID();

    public PPermission(@NotNull String permission, @NotNull Context context) {
        this.context = context;
        this.permission = permission.toLowerCase();
    }

    public PPermission(@NotNull String permission, @NotNull Context context, @NotNull UUID permissionIdentifier) {
        this(permission, context);
        this.permissionIdentifier = permissionIdentifier;
    }


    public PPermission(@NotNull String permission) {
        this(permission, Context.CONTEXT_SERVER_LOCAL);
    }

    /**
     * Check if this permission can apply to a certain situation
     *
     * @param worldName     the world name of the situation
     * @param serverId      the server id of the situation
     * @param currentTimeMs the current time in milliseconds (System.currentTimeMillis())
     * @return Whether this permission can apply to the situation
     */
    public boolean applies(String worldName, int serverId, long currentTimeMs) {
        return !this.isExpired(currentTimeMs) && this.context.applies(serverId, worldName);
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
        return getContext().isExpired(currentTimeMs);
    }

    /**
     * Gets the expiration of this permission
     *
     * @return The expiration in ms since epoch
     */
    public long getExpiry() {
        return this.getContext().getBeforeTime();
    }

    /**
     * Get the context where this permission applies
     *
     * @return The context
     */
    public Context getContext() {
        return this.context;
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

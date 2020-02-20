package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.context.Context;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Immutable representation of a permission node
 */
public final class PPermission {
    private long expiry = 0;
    private final String permission;
    private final Context context;
    private UUID permissionIdentifier = UUID.randomUUID();

    public PPermission(@NotNull String permission, @NotNull Context context, long expiry){
        this.expiry = expiry;
        this.context = context;
        this.permission = permission.toLowerCase();
    }

    public PPermission(@NotNull String permission, @NotNull Context context, long expiry, @NotNull UUID permissionIdentifier){
        this(permission, context, expiry);
        this.permissionIdentifier = permissionIdentifier;
    }

    public PPermission(@NotNull String permission){
        this(permission, 0);
    }

    public PPermission(@NotNull String permission, @NotNull Context context){
        this(permission, context, 0);
    }

    public PPermission(@NotNull String permission, long expiry){
        this(permission, Context.CONTEXT_SERVER_LOCAL, expiry);
    }

    /**
     * Check if this permission can apply to a certain situation
     * @param worldName the world name of the situation
     * @param serverName the server name of the situation
     * @param currentTimeMs the current time in milliseconds (System.currentTimeMillis())
     * @return Whether this permission can apply to the situation
     */
    public boolean applies(String worldName, String serverName, long currentTimeMs){
        return !this.isExpired(currentTimeMs) && this.context.applies(worldName, serverName);
    }

    public UUID getPermissionIdentifier() {
        return permissionIdentifier;
    }

    /**
     * Gets the actual permission represented by this permission object
     * @return Permission in string form
     */
    public String getPermission(){
        return this.permission;
    }

    /**
     * Check if this permission is expired
     * @param currentTimeMs
     * @return Whether this permission is expired
     */
    public boolean isExpired(long currentTimeMs){
        return !(currentTimeMs < expiry || expiry == 0);
    }

    /**
     * Gets the expiration of this permission
     * @return The expiration in ms
     */
    public long getExpiry(){
        return this.expiry;
    }

    /**
     * Get the context where this permission applies
     * @return The context
     */
    public Context getContext(){
        return this.context;
    }

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }
        if(o instanceof PPermission){
            if(((PPermission) o).getPermissionIdentifier().equals(this.getPermissionIdentifier())){
                return true;
            }
            if(((PPermission) o).getContext().equals(this.getContext())){
                if(((PPermission) o).expiry == this.expiry){
                    if(((PPermission) o).permission.equalsIgnoreCase(this.permission)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

}

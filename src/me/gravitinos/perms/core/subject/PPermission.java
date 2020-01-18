package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.context.Context;

/**
 * Immutable representation of a permission node
 */
public final class PPermission {
    private final long expiry;
    private final String permission;
    private final Context context;

    public PPermission(String permission, Context context, long expiry){
        this.expiry = expiry;
        this.context = context;
        this.permission = permission;
    }

    public PPermission(String permission, Context context){
        this.expiry = 0;
        this.context = context;
        this.permission = permission;
    }

    public PPermission(String permission, long expiry){
        this.expiry = expiry;
        this.permission = permission;
        this.context = new Context();
    }

    /**
     * Check if this permission can apply to a certain situation
     * @param worldName the world name of the situation
     * @param serverName the server name of the situation
     * @param currentTimeMs the current time in milliseconds (System.currentTimeMillis())
     * @return Whether this permission can apply to the situation
     */
    public boolean applies(String worldName, String serverName, long currentTimeMs){
        if(currentTimeMs < expiry && this.context.applies(worldName, serverName)){
            return true;
        }
        return false;
    }

    public String getPermission(){
        return this.permission;
    }

    /**
     * Check if this permission is expired
     * @param currentTimeMs
     * @return Whether this permission is expired
     */
    public boolean isExpired(long currentTimeMs){
        return !(currentTimeMs < expiry);
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

package me.gravitinos.perms.core.context;


/**
 * This class is used as a pair for permissions and inheritance to add a context to them (conditions in which they apply)
 */
public class Context {

    private String serverName = "";
    private String worldName = "";

    public Context(String serverName, String worldName) {
        this.serverName = serverName;
        this.worldName = worldName;
    }

    public Context(){}

    /**
     * Set the serverName
     * @param serverName What to set it to
     */
    public void setServerName(String serverName){
        this.serverName = serverName;
    }

    /**
     * set the worldName
     * @param worldName What to set it to
     */
    public void setWorldName(String worldName){
        this.worldName = worldName;
    }

    /**
     * Checks if this context applies
     * @param serverName current server name
     * @param worldName current world name
     * @return whether this context applies
     */
    public boolean applies(String serverName, String worldName) {
        return this.applies(new Context(serverName, worldName));
    }

    /**
     * Checks if this context applies
     * @param context The context to compare to
     * @return whether this context applies
     */
    public boolean applies(Context context){
        if(!this.serverName.equals("") && !context.serverName.equals(this.serverName)){
            return false;
        }

        if(!this.worldName.equals("") && !context.worldName.equals(this.worldName)){
            return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }

        //Check if it is a context object and it's values equal this objects values
        if(o instanceof Context){
            if(((Context) o).serverName.equals(this.serverName)){
                if(((Context) o).worldName.equals(this.worldName)){
                    return true;
                }
            }
        }
        return false;
    }
}

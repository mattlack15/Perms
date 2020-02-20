package me.gravitinos.perms.core.context;


import me.gravitinos.perms.core.PermsManager;

/**
 * Immutable
 * This class is used as a pair for permissions and inheritance to add a context to them (conditions in which they apply)
 */
public final class Context {

    private String serverName;
    private String worldName;

    private static final String SERVER_IDENTIFIER = "server";
    private static final String WORLD_IDENTIFIER = "world";

    public static final String VAL_ALL = "";
    public static final String VAL_NONE = "|-|";

    public static final Context CONTEXT_NONE = new Context("|-|", "|-|");
    public static final Context CONTEXT_ALL = new Context("", "");
    public static final Context CONTEXT_SERVER_GLOBAL = new Context("", "");
    public static final Context CONTEXT_SERVER_LOCAL = new Context(PermsManager.instance.getImplementation().getConfigSettings().getServerName(), VAL_ALL);

    public Context(String serverName, String worldName) {
        this.serverName = serverName;
        this.worldName = worldName;
    }

    public Context(){
        this.serverName = VAL_ALL;
        this.worldName = VAL_ALL;
    }

    public static Context fromString(String str){
        if(str == null){
            return Context.CONTEXT_SERVER_LOCAL;
        }
        Context context = new Context(CONTEXT_SERVER_LOCAL.getServerName(), VAL_ALL);
        int index;

        index = str.indexOf(SERVER_IDENTIFIER);
        if(index != -1){
            index += SERVER_IDENTIFIER.length() + 1; //Add the length of the identifier and "=" to it
            String afterIdentifier = str.substring(index);
            String[] quoteSplit = afterIdentifier.split("'");
            String[] spaceSplit = afterIdentifier.split(" ");
            if(quoteSplit.length < 2){
                context.serverName = spaceSplit.length != 0 ? (quoteSplit.length != 0 ? spaceSplit[0] : VAL_ALL) : VAL_ALL;
            } else {
                context.serverName = quoteSplit[1];
            }
        }

        index = str.indexOf(WORLD_IDENTIFIER);
        if(index != -1){
            index += WORLD_IDENTIFIER.length() + 1; //Add the length of the identifier and "=" to it
            String afterIdentifier = str.substring(index);
            String[] quoteSplit = afterIdentifier.split("'");
            String[] spaceSplit = afterIdentifier.split(" ");
            if(quoteSplit.length < 2){
                context.worldName = spaceSplit.length != 0 ? (quoteSplit.length != 0 ? spaceSplit[0] : VAL_ALL) : VAL_ALL;
            } else {
                context.worldName = quoteSplit[1];
            }
        }
        return context;
    }

    public String toString(){
        String str = "";
        str += WORLD_IDENTIFIER + "='" + worldName + "', ";
        str += SERVER_IDENTIFIER + "='" + serverName + "'";
        return str;
    }

    public String getServerName() {
        return serverName;
    }

    public String getWorldName() {
        return worldName;
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
        if(this.equals(CONTEXT_NONE)){
            return false;
        }
        if((!this.serverName.equals(VAL_ALL) && !context.serverName.equals(this.serverName)) || this.serverName.equals(VAL_NONE)){
            return false;
        }

        if((!this.worldName.equals(VAL_ALL) && !context.worldName.equals(this.worldName)) || this.worldName.equals(VAL_NONE)){
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

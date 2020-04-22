package me.gravitinos.perms.core.context;


import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.GroupData;

/**
 * Immutable
 * This class is used as a pair for permissions and inheritance to add a context to them (conditions in which they apply)
 */
public final class Context {

    private String server;
    private String worldName;
    private long beforeTime = VAL_TIME_ALL;

    private static final String SERVER_IDENTIFIER = "server";
    private static final String WORLD_IDENTIFIER = "world";
    private static final String TIME_IDENTIFIER = "beforeTime";

    public static final String VAL_ALL = "";
    public static final String VAL_NONE = "|-|";

    public static final long VAL_TIME_ALL = 0;

    public static final Context CONTEXT_NONE = new Context("|-|", "|-|");
    public static final Context CONTEXT_ALL = new Context();
    public static final Context CONTEXT_SERVER_GLOBAL = new Context(GroupData.SERVER_GLOBAL, "");
    public static final Context CONTEXT_SERVER_LOCAL = new Context(GroupData.SERVER_LOCAL, VAL_ALL);

    public Context(String server, String worldName, long expiration) {
        this.server = server.equals("-1") ? GroupData.SERVER_GLOBAL : server;
        this.worldName = worldName;
        this.beforeTime = expiration;
    }

    public Context(String server, String worldName){
        this(server, worldName, VAL_TIME_ALL);
    }

    public Context(){
        this(VAL_ALL, VAL_ALL);
    }

    public static Context fromString(String str){
        if(str == null){
            return Context.CONTEXT_SERVER_LOCAL;
        }
        Context context = new Context(CONTEXT_SERVER_LOCAL.getServer(), VAL_ALL);
        int index;

        index = str.indexOf(SERVER_IDENTIFIER);
        if(index != -1){
            index += SERVER_IDENTIFIER.length() + 1; //Add the length of the identifier and "=" to it
            String afterIdentifier = str.substring(index);
            String[] quoteSplit = afterIdentifier.split("'");
            String[] spaceSplit = afterIdentifier.split(" ");
            if(quoteSplit.length < 2){
                context.server = spaceSplit.length != 0 ? (quoteSplit.length != 0 ? spaceSplit[0] : VAL_ALL) : VAL_ALL;
            } else {
                context.server = quoteSplit[1];
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

        index = str.indexOf(TIME_IDENTIFIER);
        if(index != -1){
            index += TIME_IDENTIFIER.length() + 1; //Add the length of the identifier and "=" to it
            String afterIdentifier = str.substring(index);
            String[] quoteSplit = afterIdentifier.split("'");
            String[] spaceSplit = afterIdentifier.split(" ");
            if(quoteSplit.length < 2){
                context.beforeTime = Long.parseLong(spaceSplit.length != 0 ? (quoteSplit.length != 0 ? spaceSplit[0] : VAL_ALL) : VAL_ALL);
            } else {
                context.beforeTime = Long.parseLong(quoteSplit[1]);
            }
        }
        return context;
    }

    public String toString(){
        String str = "";
        str += WORLD_IDENTIFIER + "='" + worldName + "', ";
        str += SERVER_IDENTIFIER + "='" + server + "', ";
        str += TIME_IDENTIFIER + "='" + beforeTime + "'";
        return str;
    }

    public String getServer() {
        return server;
    }

    public String getNameOfServer(){
        return PermsManager.instance.getServerName(server);
    }

    public String getWorldName() {
        return worldName;
    }

    /**
     * Also known as the expiration
     */
    public long getBeforeTime() { return this.beforeTime; }

    public boolean isExpired(){
        return !(System.currentTimeMillis() < this.beforeTime || this.beforeTime == VAL_TIME_ALL);
    }

    public boolean isExpired(long ctm){
        return !(ctm < this.beforeTime || this.beforeTime == VAL_TIME_ALL);
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

    public static void setContextServer(Context context, String server){
        context.server = server;
    }
    public static void setContextTime(Context context, long time){
        context.beforeTime = time;
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
        if((!this.server.equals(VAL_ALL) && !context.server.equals(this.server)) || this.server.equals(VAL_NONE)){
            return false;
        }

        if((!this.worldName.equals(VAL_ALL) && !context.worldName.equals(this.worldName)) || this.worldName.equals(VAL_NONE)){
            return false;
        }

        return !this.isExpired();
    }

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }

        //Check if it is a context object and it's values equal this objects values
        if(o instanceof Context){
            if(((Context) o).server.equals(this.server)){
                if(((Context) o).worldName.equals(this.worldName)){
                   // if(((Context) o).getBeforeTime() == this.beforeTime) {
                        return true;
                   // }
                }
            }
        }
        return false;
    }
}

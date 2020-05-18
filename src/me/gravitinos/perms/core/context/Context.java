package me.gravitinos.perms.core.context;


import lombok.Getter;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.util.StringSerializer;

import java.io.*;

/**
 * Immutable
 * This class is used as a pair for permissions and inheritance to add a context to them (conditions in which they apply)
 */
public final class Context {

    public static final String WORLD_IDENTIFIER = "world";
    public static final String SERVER_IDENTIFIER = "server";

    public static final String VAL_STR_ALL = "";
    public static final Context CONTEXT_SERVER_LOCAL = new Context(SERVER_IDENTIFIER, GroupData.SERVER_LOCAL + "");

    @Getter
    private String key;

    @Getter
    private String value;

    public Context(String key, String value) {

        //Auto-Correct
        ContextAutoCorrect.StrPair pair = ContextAutoCorrect.autoCorrect(key, value);

        this.key = pair.getKey();
        this.value = pair.getValue();
    }

    public boolean equals(Object o) {
        return o instanceof Context && (((Context) o).getKey().equals(this.getKey())) && ((Context) o).getValue().equals(this.getValue());
    }

    public String toString() {
        StringSerializer serializer = new StringSerializer();
        serializer.writeString(key);
        serializer.writeString(value);
        return serializer.toString();
    }

    public static Context fromString(String str){
        StringSerializer serializer = new StringSerializer(str);
        String key = serializer.readString();
        String value = serializer.readString();
        return new Context(key, value);
    }

    public static Context fromStringChatFormat(String str){
        try{
            String key = str.split("=")[0];
            String value = str.split("=")[1];
            return new Context(key, value);
        } catch(Exception e){
            return null;
        }
    }

    //---- OLD ----

//    public static final String VAL_STR_ALL = "";
//    public static final int VAL_INT_ALL = -1;
//    public static final long VAL_TIME_ALL = 0;
//    public static final Context CONTEXT_ALL = new Context();
//    public static final Context CONTEXT_SERVER_GLOBAL = new Context(VAL_INT_ALL, VAL_STR_ALL);
//    public static final Context CONTEXT_SERVER_LOCAL = new Context(GroupData.SERVER_LOCAL, VAL_STR_ALL);
//    private static final String SERVER_IDENTIFIER = "server";
//    private static final String WORLD_IDENTIFIER = "world";
//    private static final String TIME_IDENTIFIER = "beforeTime";
//    private int server;
//    private String worldName;
//    private long beforeTime;
//
//    public Context(int server, String worldName, long expiration) {
//        this.server = server == -1 ? GroupData.SERVER_GLOBAL : server;
//        this.worldName = worldName;
//        this.beforeTime = expiration;
//    }
//
//    public Context(int server, String worldName) {
//        this(server, worldName, VAL_INT_ALL);
//    }
//
//    public Context() {
//        this(GroupData.SERVER_GLOBAL, VAL_STR_ALL);
//    }
//
//    public static Context fromString(String str) {
//        if (str == null) {
//            return Context.CONTEXT_SERVER_LOCAL;
//        }
//        Context context = new Context(CONTEXT_SERVER_LOCAL.getServer(), VAL_STR_ALL);
//        int index;
//
//        index = str.indexOf(SERVER_IDENTIFIER);
//        if (index != -1) {
//            index += SERVER_IDENTIFIER.length() + 1; //Add the length of the identifier and "=" to it
//            String afterIdentifier = str.substring(index);
//            String[] quoteSplit = afterIdentifier.split("'");
//            String[] spaceSplit = afterIdentifier.split(" ");
//            try {
//                if (quoteSplit.length < 2) {
//                    context.server = Integer.parseInt(spaceSplit.length != 0 ? (quoteSplit.length != 0 ? spaceSplit[0] : VAL_STR_ALL) : VAL_STR_ALL);
//                } else {
//                    context.server = Integer.parseInt(quoteSplit[1]);
//                }
//            } catch(NumberFormatException e){
//                //To convert from old "" as VAL_INT_ALL
//                context.server = Context.VAL_INT_ALL;
//            }
//        }
//
//        index = str.indexOf(WORLD_IDENTIFIER);
//        if (index != -1) {
//            index += WORLD_IDENTIFIER.length() + 1; //Add the length of the identifier and "=" to it
//            String afterIdentifier = str.substring(index);
//            String[] quoteSplit = afterIdentifier.split("'");
//            String[] spaceSplit = afterIdentifier.split(" ");
//            if (quoteSplit.length < 2) {
//                context.worldName = spaceSplit.length != 0 ? (quoteSplit.length != 0 ? spaceSplit[0] : VAL_STR_ALL) : VAL_STR_ALL;
//            } else {
//                context.worldName = quoteSplit[1];
//            }
//        }
//
//        index = str.indexOf(TIME_IDENTIFIER);
//        if (index != -1) {
//            index += TIME_IDENTIFIER.length() + 1; //Add the length of the identifier and "=" to it
//            String afterIdentifier = str.substring(index);
//            String[] quoteSplit = afterIdentifier.split("'");
//            String[] spaceSplit = afterIdentifier.split(" ");
//            if (quoteSplit.length < 2) {
//                context.beforeTime = Long.parseLong(spaceSplit.length != 0 ? (quoteSplit.length != 0 ? spaceSplit[0] : VAL_STR_ALL) : VAL_STR_ALL);
//            } else {
//                context.beforeTime = Long.parseLong(quoteSplit[1]);
//            }
//        }
//        return context;
//    }
//
//    public static void setContextServer(Context context, int server) {
//        context.server = server;
//    }
//
//    public static void setContextTime(Context context, long time) {
//        context.beforeTime = time;
//    }
//
//    public String toString() {
//        String str = "";
//        str += WORLD_IDENTIFIER + "='" + worldName + "', ";
//        str += SERVER_IDENTIFIER + "='" + server + "', ";
//        str += TIME_IDENTIFIER + "='" + beforeTime + "'";
//        return str;
//    }
//
//    public int getServer() {
//        return server;
//    }
//
//    public String getNameOfServer() {
//        return PermsManager.instance.getServerName(server);
//    }
//
//    public String getWorldName() {
//        return worldName;
//    }
//
//    /**
//     * Also known as the expiration
//     */
//    public long getBeforeTime() {
//        return this.beforeTime;
//    }
//
//    public boolean isExpired() {
//        return !(System.currentTimeMillis() < this.beforeTime || this.beforeTime == VAL_TIME_ALL);
//    }
//
//    public boolean isExpired(long ctm) {
//        return !(ctm < this.beforeTime || this.beforeTime == VAL_TIME_ALL);
//    }
//
//    /**
//     * Checks if this context applies
//     *
//     * @param serverId  current server id
//     * @param worldName current world name
//     * @return whether this context applies
//     */
//    public boolean applies(int serverId, String worldName) {
//        return this.applies(new Context(serverId, worldName));
//    }
//
//    /**
//     * Checks if this context applies
//     *
//     * @param context The context to compare to
//     * @return whether this context applies
//     */
//    public boolean applies(Context context) {
//        if (!(this.server == GroupData.SERVER_GLOBAL || context.server == this.server)) {
//            return false;
//        }
//
//        if (!(this.worldName.equals(VAL_STR_ALL) || context.worldName.equals(this.worldName))) {
//            return false;
//        }
//
//        return !this.isExpired();
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (o == this) {
//            return true;
//        }
//        if (!(o instanceof Context)) {
//            return false;
//        }
//        Context c = (Context) o;
//        return (c.server == this.server && c.worldName.equals(this.worldName));
//    }
}

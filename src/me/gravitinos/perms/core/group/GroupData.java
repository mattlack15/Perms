package me.gravitinos.perms.core.group;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.subject.SubjectData;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public class GroupData extends SubjectData {
    public static final int SERVER_LOCAL = PermsManager.instance.getImplementation().getConfigSettings().getServerId();
    private static final String PREFIX = "prefix";
    private static final String SUFFIX = "suffix";
    private static final String CHAT_COLOUR = "chat_colour";
    private static final String DESCRIPTION = "description";
    private static final String CONTEXT = "context";
    private static final String PRIORITY = "priority";
    private static final String GOD_LOCK = "god_locked";
    private static final String ICON = "icon";
   // private static final String GOD_LOCK_INHERITANCE = "god_lock_inheritances";
   // private static final String GOD_LOCK_PERMISSION = "god_lock_permissions";
   // private static final String GOD_LOCK_DELETE = "god_lock_deletion";
   // private static final String GOD_LOCK_OPTIONS = "god_lock_options";

    private volatile ContextSet cachedContext = null;

    public GroupData() {
    }

    public GroupData(SubjectData data) {
        super(data);
    }

    public void setExtraData(String key, String value) {
        this.setData("EXTRA_" + key, value);
    }

    public String getExtraData(String key) {
        return this.getData("EXTRA_" + key);
    }

    public String getDescription() {
        return this.getData(DESCRIPTION, "");
    }

    public void setDescription(String description) {
        this.setData(DESCRIPTION, description);
    }

    public String getPrefix() {
        return this.getData(PREFIX, "");
    }

    public void setPrefix(String prefix) {
        this.setData(PREFIX, prefix);
    }

    public String getSuffix() {
        return this.getData(SUFFIX, "");
    }

    public void setSuffix(String suffix) {
        this.setData(SUFFIX, suffix);
    }

    public String getChatColour() {
        return this.getData(CHAT_COLOUR, "");
    }

    public void setChatColour(String colour) {
        this.setData(CHAT_COLOUR, colour);
    }

    public boolean isGodLocked(){
        return Boolean.parseBoolean(this.getData(GOD_LOCK));
    }

    public void setGodLocked(boolean value) {
        this.setData(GOD_LOCK, Boolean.toString(value));
    }

//    public void setGodLockPermission(boolean val) {
//        this.setData(GOD_LOCK_PERMISSION, Boolean.toString(val));
//    }
//
//    public void setGodLockInheritance(boolean val){
//        this.setData(GOD_LOCK_INHERITANCE, Boolean.toString(val));
//    }
//
//    public void setGodLockDelete(boolean val){
//        this.setData(GOD_LOCK_DELETE, Boolean.toString(val));
//    }
//
//    public void setGodLockOptions(boolean val){
//        this.setData(GOD_LOCK_OPTIONS, Boolean.toString(val));
//    }
//
//    public boolean isGodLockPermission(){
//        return Boolean.valueOf(this.getData(GOD_LOCK_PERMISSION));
//    }
//
//    public boolean isGodLockInheritance(){
//        return Boolean.valueOf(this.getData(GOD_LOCK_INHERITANCE));
//    }
//
//    public boolean isGodLockDelete(){
//        return Boolean.valueOf(this.getData(GOD_LOCK_DELETE));
//    }
//
//    public boolean isGodLockOptions(){
//        return Boolean.valueOf(this.getData(GOD_LOCK_OPTIONS));
//    }

    public ContextSet getContext() {
        if(cachedContext == null) {
            String data = getData(CONTEXT);
            if (data == null) {
                this.setContext(new MutableContextSet(Context.CONTEXT_SERVER_LOCAL), true);
            } else {
                cachedContext = ContextSet.fromString(getData(CONTEXT));
            }
        }
        return cachedContext;
    }

    public int getPriority() {
        String priority = this.getData(PRIORITY);
        if (priority == null) {
            this.setPriority(0);
            return 0;
        }
        try {
            return Integer.parseInt(priority);
        } catch (Exception e) {
            this.setPriority(0);
            return 0;
        }
    }

    public int getIconCombinedId() {
        try {
            return Integer.parseInt(getData(ICON));
        } catch (NumberFormatException e) {
            this.setIconCombinedId(Material.BOOK.getId() << 4);
            return Material.BOOK.getId() << 4;
        }
    }

    public void setIconCombinedId(int combinedId) {
        this.setData(ICON, Integer.toString(combinedId));
    }

    public void setPriority(int i) {
        this.setData(PRIORITY, Integer.toString(i));
    }

    public boolean setContext(ContextSet context, boolean force){
        if(!force && GroupManager.instance.canGroupContextCollideWithAnyLoaded(this.getName(), context, this.getContext())){
            return false;
        }
        this.setData(CONTEXT, context.toString());
        cachedContext = context.clone();
        return true;
    }
}
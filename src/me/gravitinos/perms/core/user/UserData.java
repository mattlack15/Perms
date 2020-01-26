package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.subject.SubjectData;

public class UserData extends SubjectData {
    public UserData(SubjectData data) {
        super(data);
    }

    public static final String SERVER_LOCAL = PermsManager.instance.getImplementation().getConfigSettings().getServerName();
    public static final String SERVER_GLOBAL = Context.VAL_ALL;

    public UserData() {
    }

    public void setName(String name) {
        this.setData("username", name);
    }

    public void setDisplayGroup(String server, String displayGroup) {
        this.setData("displaygroup_" + server, displayGroup);
    }

    public void setPrefix(String prefix) {
        this.setData("prefix", prefix);
    }

    public void setSuffix(String suffix) {
        this.setData("suffix", suffix);
    }

    public void setNotes(String notes) {
        this.setData("notes", notes);
    }

    public String getNotes() {
        return this.getData("notes", "");
    }

    public String getDisplayGroup(String server) {
        String out = this.getData("displaygroup_" + server);
        if(out == null){
            out = this.getData("displaygroup_");
        }
        return out;
    }

    public String getPrefix() {
        return this.getData("prefix", "");
    }

    public String getSuffix() {
        return this.getData("suffix", "");
    }

    public String getName() {
        return this.getData("username", "");
    }

}

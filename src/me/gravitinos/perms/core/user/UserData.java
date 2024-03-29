package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.subject.SubjectData;

import java.util.UUID;

public class UserData extends SubjectData {
    public UserData(SubjectData data) {
        super(data);
    }

    public static final int SERVER_LOCAL = GroupData.SERVER_LOCAL;

    public UserData() {
    }

    public void setExtraData(String key, String value){
        this.setData("EXTRA_" + key, value);
    }

    public String getExtraData(String key){
        return this.getData("EXTRA_" + key);
    }

    public boolean hasExtraData(String key) { return this.hasData("EXTRA_" + key); }

    public void setName(String name) {
        this.setData("username", name);
    }

    public void setDisplayGroup(int server, UUID groupId) {
        this.setData("displaygroup_" + server, groupId.toString());
    }

    public long getFirstJoined(){
        try {
            return Long.parseLong(this.getData("first_joined_ms"));
        } catch(Exception e){
            return -1;
        }
    }

    public void setFirstJoined(long timeMs){
        this.setData("first_joined_ms", Long.toString(timeMs));
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

    public UUID getDisplayGroup(int server) {
        String out = this.getData("displaygroup_" + server);
        if(out == null){
            out = this.getData("displaygroup_");
        }
        try {
            return UUID.fromString(out);
        } catch(Exception e){
            return null;
        }
    }

    public String getPrefix() {
        return this.getData("prefix", "");
    }

    public String getSuffix() {
        return this.getData("suffix", "");
    }

    @Override
    public String getName() {
        return this.getData("username", "");
    }

}

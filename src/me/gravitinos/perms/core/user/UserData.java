package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.subject.SubjectData;

public class UserData extends SubjectData {
    public UserData(SubjectData data) {
        super(data);
    }

    public UserData() {
    }

    public void setName(String name) {
        this.setData("username", name);
    }

    public void setDisplayGroup(String displayGroup) {
        this.setData("displaygroup", displayGroup);
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

    public String getDisplayGroup() {
        return this.getData("displaygroup");
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

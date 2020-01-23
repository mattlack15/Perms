package me.gravitinos.perms.core.subject;

import org.jetbrains.annotations.NotNull;

public class GenericSubjectData extends SubjectData {

    public String type = "GENERIC";

    public GenericSubjectData(SubjectData data) {
        super(data);
    }

    public void setType(@NotNull String type){
        this.type = type;
    }

    public String getType(){
        return this.type;
    }

    public GenericSubjectData() {
    }

    public void setData(String key, String value) {
        super.setData(key, value);
    }

    public String getData(String key) {
        return super.getData(key);
    }

    public String getData(String key, String defaultValue) {
        return super.getData(key, defaultValue);
    }
}

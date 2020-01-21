package me.gravitinos.perms.core.subject;

public class GenericSubjectData extends SubjectData {

    public GenericSubjectData(SubjectData data) {
        super(data);
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

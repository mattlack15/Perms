package me.gravitinos.perms.core.subject;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for GroupData and UserData
 */
public class SubjectData {
    private Map<String, String> data = new HashMap<>();
    public SubjectData(SubjectData copy){
        data = copy.data;
    }
    public SubjectData(){
    }

    protected void setData(String key, String val){
        this.data.put(key, val);
    }
    protected String getData(String key){
        return this.data.get(key);
    }
    protected String getData(String key, String defaultValue){
        String out = this.data.get(key);
        return out != null ? out : defaultValue;
    }

}

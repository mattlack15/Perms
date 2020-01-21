package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.util.SubjectDataUpdateListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for GroupData and UserData
 */
public abstract class SubjectData {
    private Map<String, SubjectDataUpdateListener> listeners = new HashMap<>();
    private Map<String, String> data = new HashMap<>();
    public SubjectData(SubjectData copy){
        data = copy.data;
        this.listeners = copy.listeners;
    }
    public SubjectData(){
    }

    protected void setData(String key, String val){
        this.data.put(key, val);
        this.listeners.values().forEach(v -> v.update(key, val));
    }

    protected String getData(String key){
        return this.data.get(key);
    }
    protected String getData(String key, String defaultValue){
        String out = this.data.get(key);
        return out != null ? out : defaultValue;
    }

    public void addUpdateListener(String name, SubjectDataUpdateListener listener){
        this.listeners.put(name, listener);
    }
}

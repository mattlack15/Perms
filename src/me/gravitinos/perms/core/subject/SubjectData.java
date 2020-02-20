package me.gravitinos.perms.core.subject;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.util.MapUtil;
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

    public static GenericSubjectData fromString(String str){

        GenericSubjectData subjectData = new GenericSubjectData();

        Map<String, String> d = MapUtil.stringToMap(str);
        if(d == null){
            return null;
        }

        for(String keys : d.keySet()){
            subjectData.setData(keys, d.get(keys));
        }

        return subjectData;
    }

    @Override
    public String toString(){
        return MapUtil.mapToString(this.data);
    }

    public void addUpdateListener(String name, SubjectDataUpdateListener listener){
        this.listeners.put(name, listener);
    }

    public SubjectData copy(){
        GenericSubjectData data = new GenericSubjectData();
        for(String keys : this.data.keySet()){
            data.setData(keys, this.data.get(keys));
        }
        for(String keys : this.listeners.keySet()) {
            data.addUpdateListener(keys, this.listeners.get(keys));
        }
        return data;
    }
}

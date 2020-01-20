package me.gravitinos.perms.core.group;

import me.gravitinos.perms.core.backend.DataManager;

public class GroupManager {
    private DataManager dataManager;
    public GroupManager(DataManager dataManager){
        this.dataManager = dataManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}

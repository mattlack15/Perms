package me.gravitinos.perms.core.user;

import me.gravitinos.perms.core.backend.DataManager;

public class UserManager {

    private DataManager dataManager;
    public UserManager(DataManager dataManager){
        this.dataManager = dataManager;
    }
    public DataManager getDataManager(){
        return this.dataManager;
    }
}

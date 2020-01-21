package me.gravitinos.perms.core;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.file.FileHandler;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;

public class PermsManager {
    public static PermsManager instance = null;

    private GroupManager groupManager;
    private UserManager userManager;
    private DataManager dataManager;

    private PermsImplementation implementation;

    public PermsManager(PermsImplementation implementation){
        instance = this;
        this.implementation = implementation;

        if(implementation.getConfigSettings().isUsingSQL()){
            this.dataManager = new SQLHandler();
        } else {
            this.dataManager = new FileHandler();
        }

        this.groupManager = new GroupManager(dataManager);
        this.userManager = new UserManager(dataManager);
    }

    public PermsImplementation getImplementation() {
        return implementation;
    }
}

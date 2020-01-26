package me.gravitinos.perms.core;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.UserManager;
import org.jetbrains.annotations.NotNull;

public class PermsManager {
    public static PermsManager instance = null;

    private GroupManager groupManager;
    private UserManager userManager;
    private DataManager dataManager;

    private PermsImplementation implementation;

    public PermsManager(PermsImplementation implementation, @NotNull DataManager dataManager){
        instance = this;
        this.implementation = implementation;

        this.dataManager = dataManager;

        this.groupManager = new GroupManager(dataManager);
        this.userManager = new UserManager(dataManager);

        //Config checks
        if(implementation.getConfigSettings().getServerName().equals("")){
            implementation.getConfigSettings().setServerName("server_" + Math.round(Math.random() * 1000));
        }
    }

    public PermsImplementation getImplementation() {
        return implementation;
    }
}

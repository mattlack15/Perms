package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.PermsImplementation;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.spigot.file.SpigotFileDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotPerms extends JavaPlugin {
    public static SpigotPerms instance;

    public static final String commandName = "ranks";

    private PermsManager manager;

    private SpigotImpl impl;

    public SpigotPerms(){
        instance = this;
        this.impl = new SpigotImpl();
        DataManager dataManager;
        if(impl.getConfigSettings().isUsingSQL()){
            dataManager = new SQLHandler();
        } else {
            dataManager = new SpigotFileDataManager();
        }
        manager = new PermsManager(impl, dataManager);
    }

    public SpigotImpl getImpl(){
        return this.impl;
    }

    public PermsManager getManager(){
        return this.manager;
    }

}

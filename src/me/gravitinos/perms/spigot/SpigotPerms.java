package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.spigot.file.SpigotFileDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotPerms extends JavaPlugin {
    public static SpigotPerms instance;

    private PermsManager manager;

    public SpigotPerms(){
        instance = this;
        SpigotImpl impl = new SpigotImpl();
        DataManager dataManager;
        if(impl.getConfigSettings().isUsingSQL()){
            dataManager = new SQLHandler();
        } else {
            dataManager = new SpigotFileDataManager();
        }
        manager = new PermsManager(impl, dataManager);
    }

    public PermsManager getManager(){
        return this.manager;
    }

}

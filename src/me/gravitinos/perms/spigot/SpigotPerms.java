package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.PermsImplementation;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.config.PermsConfiguration;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.command.CommandPerms;
import me.gravitinos.perms.spigot.file.SpigotFileDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class SpigotPerms extends JavaPlugin {
    public static SpigotPerms instance;

    public static final String commandName = "ranks";

    public static String pluginPrefix;

    private PermsManager manager;

    private SpigotImpl impl;

    public SpigotPerms(){
        instance = this;
        this.impl = new SpigotImpl();
        DataManager dataManager;
        if(impl.getConfigSettings().isUsingSQL()){
            dataManager = new SQLHandler();
            if(this.connectSQL((SQLHandler) dataManager, impl.getConfigSettings())){
                this.impl.consoleLog(ChatColor.GREEN + "Connected to SQL successfully!");
            }
        } else {
            dataManager = new SpigotFileDataManager();
        }
        pluginPrefix = impl.getConfigSettings().getPrefix();
        manager = new PermsManager(impl, dataManager);

        //Commands
        new CommandPerms();

        //For all online players
        for(Player p : Bukkit.getOnlinePlayers()){
            //Load the user
            UserManager.instance.loadUser(p.getUniqueId(), p.getName());

            //Inject user with permissible
            SpigotPermissible.inject(p);
        }

    }

    private boolean connectSQL(SQLHandler dataManager, PermsConfiguration config){
        try {
            if (!dataManager.startConnection("jdbc:mysql://" + config.getSQLHost() + ":" + config.getSQLPort() + "/" + config.getSQLDatabase() + "?testWhileIdle=true?rewriteBatchedStatements=true", config.getSQLUsername(), config.getSQLPassword())) {
                this.impl.addToLog(ChatColor.RED + "Unable to connect to SQL!");
                this.impl.consoleLog(ChatColor.RED + "Unable to connect to SQL!");
                return false;
            }
        } catch (SQLException e){
            e.printStackTrace();
            this.impl.addToLog(ChatColor.RED + "Unable to connect to SQL!");
            this.impl.consoleLog(ChatColor.RED + "Unable to connect to SQL!");
            return false;
        }
        try {
            dataManager.setup().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            this.impl.addToLog(ChatColor.RED + "Unable to setup SQL Database!");
            this.impl.consoleLog(ChatColor.RED + "Unable to setup SQL Database!");
            return false;
        }
        return true;
    }

    public SpigotImpl getImpl(){
        return this.impl;
    }

    public PermsManager getManager(){
        return this.manager;
    }

}

package me.gravitinos.perms.spigot;

import me.clip.placeholderapi.PlaceholderAPI;
import me.gravitinos.perms.core.PermsImplementation;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.config.PermsConfiguration;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.command.CommandPerms;
import me.gravitinos.perms.spigot.command.GravCommand;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import me.gravitinos.perms.spigot.file.Files;
import me.gravitinos.perms.spigot.file.SpigotFileDataManager;
import me.gravitinos.perms.spigot.listeners.ChatListener;
import me.gravitinos.perms.spigot.listeners.LoginListener;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class SpigotPerms extends JavaPlugin {
    public static SpigotPerms instance;

    public static final String commandName = "ranks";

    public static String pluginPrefix;

    private PermsManager manager;

    private GravCommand mainCommand;

    private SpigotImpl impl;

    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.impl = new SpigotImpl();
        new Files();
        DataManager dataManager;
        if (impl.getConfigSettings().isUsingSQL()) {
            dataManager = new SQLHandler();
        } else {
            dataManager = new SpigotFileDataManager();
        }
        pluginPrefix = impl.getConfigSettings().getPrefix();
        manager = new PermsManager(impl, dataManager);

        //Commands
        this.mainCommand = new CommandPerms();

        //Listeners
        new ChatListener();
        new LoginListener();

        //For all online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            //Load the user
            UserManager.instance.loadUser(p.getUniqueId(), p.getName());

            //Inject user with permissible
            SpigotPermissible.inject(p);
        }

        PlaceholderAPI.registerPlaceholderHook(this, new Placeholders());
        if(integrateWithVault()){
            impl.consoleLog("Integrated with vault!");
        }
    }

    public void onDisable() {
    }

    public SpigotImpl getImpl() {
        return this.impl;
    }

    public PermsManager getManager() {
        return this.manager;
    }

    public boolean integrateWithVault() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            PermsVault permsVault = new PermsVault();
            this.getServer().getServicesManager().register(Permission.class, permsVault, this, ServicePriority.High);
            return true;
        }
        return false;
    }

}

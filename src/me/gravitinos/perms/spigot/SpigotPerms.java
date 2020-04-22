package me.gravitinos.perms.spigot;

import me.clip.placeholderapi.PlaceholderAPI;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.command.CommandPerms;
import me.gravitinos.perms.spigot.command.GravCommand;
import me.gravitinos.perms.spigot.file.Files;
import me.gravitinos.perms.spigot.file.SpigotFileDataManager;
import me.gravitinos.perms.spigot.listeners.ChatListener;
import me.gravitinos.perms.spigot.listeners.LoginListener;
import me.gravitinos.perms.spigot.verbose.VerboseController;
import net.milkbowl.vault.permission.Permission;
import net.minecraft.server.v1_12_R1.ChunkSection;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpigotPerms extends JavaPlugin {
    public static SpigotPerms instance;

    public static final String commandName = "ranks";

    public static String pluginPrefix;

    private PermsManager manager;

    private GravCommand mainCommand;

    private Map<String, List<String>> permissionIndex = new HashMap<>();

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

        //Verbose
        new VerboseController();

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

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PlaceholderAPI.unregisterPlaceholderHook(this.getName());
            PlaceholderAPI.registerPlaceholderHook(this.getName(), new Placeholders());
        }
        if (integrateWithVault()) {
            impl.consoleLog("Integrated with vault!");
        }

        //Permission Index
        loadPermissionIndex();

        new BukkitRunnable(){
            @Override
            public void run() {
                for(Plugin plugin : Bukkit.getPluginManager().getPlugins()){
                    plugin.getDescription().getPermissions().forEach(p -> addPermissionToIndex(plugin.getName(), p.getName()));
                }

                for(org.bukkit.permissions.Permission permissions : Bukkit.getPluginManager().getPermissions()){
                    addPermissionToIndex(permissions.getName());
                }
            }
        }.runTaskLaterAsynchronously(this, 40);
    }

    public void onDisable() {
        PlaceholderAPI.unregisterPlaceholderHook(this);
    }

    public SpigotImpl getImpl() {
        return this.impl;
    }

    public PermsManager getManager() {
        return this.manager;
    }

    /**
     * Integrates with vault
     */
    public boolean integrateWithVault() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            PermsVault permsVault = new PermsVault();
            this.getServer().getServicesManager().unregister(Permission.class);
            this.getServer().getServicesManager().register(Permission.class, permsVault, this, ServicePriority.High);
            return true;
        }
        return false;
    }

    /**
     * Loads the permission index from the files
     */
    public void loadPermissionIndex() {
        permissionIndex.clear();
        synchronized (Files.PERMISSION_INDEX_FILE) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(Files.PERMISSION_INDEX_FILE);
            for (String keys : config.getKeys(false)) {
                permissionIndex.put(keys, config.getStringList(keys));
            }
        }
    }

    public Map<String, List<String>> getPermissionIndex() {
        return this.permissionIndex;
    }

    /**
     * Adds a permission to the permission index, if it doesn't already exist in the permission index
     */
    public void addPermissionToIndex(String permission){
        permission = permission.toLowerCase();
        ArrayList<String> alreadyKnown = new ArrayList<>();
        for (String keys : permissionIndex.keySet()) {
            alreadyKnown.addAll(permissionIndex.get(keys));
        }
        if (alreadyKnown.contains(permission)) {
            return;
        }

        //Figure out the most likely plugin
        String mostLikelyPlugin = "Unknown";
        String[] split = permission.split("\\.");
        if (split.length > 1) { // > 1 because if the permission is 1 word, it's probably not the plugin name
            String startsWith = permission.split("\\.")[0];
            for (Plugin plugins : Bukkit.getPluginManager().getPlugins()) {
                if (plugins.getName().equalsIgnoreCase(startsWith)) {
                    mostLikelyPlugin = plugins.getName();
                }
            }
        }
        addPermissionToIndex(mostLikelyPlugin, permission);
    }

    /**
     * Adds a permission to the permission index, if it doesn't already exist in the permission index
     */
    public void addPermissionToIndex(String plugin, String permission) {

        permission = permission.toLowerCase();
        ArrayList<String> alreadyKnown = new ArrayList<>();
        for (String keys : permissionIndex.keySet()) {
            alreadyKnown.addAll(permissionIndex.get(keys));
        }
        if (alreadyKnown.contains(permission)) {
            return;
        }

        //Add to file
        synchronized (Files.PERMISSION_INDEX_FILE) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(Files.PERMISSION_INDEX_FILE);

            List<String> alreadyThere = new ArrayList<>();
            if (config.isList(plugin)) {
                alreadyThere = config.getStringList(plugin);
            }

            alreadyThere.add(permission);

            config.set(plugin, alreadyThere);
            permissionIndex.put(plugin, alreadyThere);
            try {
                config.save(Files.PERMISSION_INDEX_FILE);
            } catch (IOException e) {
                impl.addToLog("Could not save Permission Index File");
                impl.consoleLog("Could not save Permission Index File");
                e.printStackTrace();
            }
        }
    }

}

package me.gravitinos.perms.bungee;

import me.gravitinos.perms.core.PermsImplementation;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class BungeePerms extends Plugin implements Listener {
    public static BungeePerms instance;
    public static Configuration config;

    private volatile ArrayList<UUID> loggingIn = new ArrayList<>();

    private PermsManager manager;

    @Override
    public void onEnable(){
        instance = this;

        //Save default config
        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        File file = new File(getDataFolder(), "config.yml");


        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Load config
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        SQLHandler handler = new SQLHandler();
        manager = new PermsManager(new BungeeImpl(), handler);

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new CommandReloadGroups());
    }

    @EventHandler
    public void onPermCheck(PermissionCheckEvent event){
        event.setHasPermission(this.hasPermission(event.getSender(), event.getPermission()));
    }

    @EventHandler
    public void onLeave(net.md_5.bungee.api.event.PlayerDisconnectEvent event){
        UserManager.instance.unloadUser(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onLogin(net.md_5.bungee.api.event.LoginEvent event){
        if(event.isCancelled()) return;

        String name = event.getConnection().getName();
        UUID id = event.getConnection().getUniqueId();

        try {
            UserManager.instance.loadUser(id, name).get();
            User user = UserManager.instance.getUser(id);
            getLogger().info("Userdata loaded for " + user.getName() + " (" + user.getUniqueID() + ")");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void onSwitchServers(ServerConnectEvent event){
        UserManager.instance.loadUser(event.getPlayer().getUniqueId(), event.getPlayer().getName()); //reload the user again (the load operation is async since .get() is not called)
    }

    public boolean hasPermission(CommandSender sender, String requ) {
        User user = UserManager.instance.getUserFromName(sender.getName());
        if((BungeeConfigSettings.instance.getGodUsers().contains(sender.getName()))) {
            return true;
        }
        if(user == null){
            return false;
        }

        Context context = new Context(BungeeConfigSettings.instance.getServerName(), Context.VAL_ALL);

        ArrayList<String> perms = new ArrayList<>();
        user.getAllPermissions(context).forEach(p -> {
            if(p.getContext().applies(context)) { //Check context
                perms.add(p.getPermission());
            }
        });
        if(perms.contains("-" + requ)) {
            return false;
        }
        for (String perm : perms) {
            boolean value = true;
            if (perm.startsWith("-")) {
                perm = perm.substring(1);
                value = false;
            }
            if (perm.equals("*") || perm.equalsIgnoreCase(requ)) {
                return value;
            }
            if (perm.endsWith("*") && requ.startsWith(perm.substring(0, perm.length() - 1))) {
                return value;
            }
        }
        return false;
    }
}

package me.gravitinos.perms.bungee;

import com.zaxxer.hikari.HikariDataSource;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.StorageCredentials;
import me.gravitinos.perms.core.backend.mongo.MongoHandler;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
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


        BungeeImpl bi = new BungeeImpl();
        DataManager dataManager;
        if (bi.getConfigSettings().isUsingSQL()) {
            String type = bi.getConfigSettings().getDatabaseType();
            if(type.equalsIgnoreCase("mongo")) {
                dataManager = new MongoHandler(new StorageCredentials(bi.getConfigSettings().getSQLUsername(), bi.getConfigSettings().getSQLPassword(),
                        bi.getConfigSettings().getSQLHost(), bi.getConfigSettings().getSQLPort()), bi.getConfigSettings().getSQLDatabase());
            } else {
                dataManager = new SQLHandler();
            }
        } else {
            getLogger().severe("PERMS > SQL must be set to true and configured in config.yml");
            return;
        }
        manager = new PermsManager(bi, dataManager);

        UserManager.instance.setDefaultGroupInheritanceContext(new MutableContextSet());

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new CommandReloadGroups());

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("BungeeCord");
    }

    @Override
    public void onDisable(){
        if (GroupManager.instance.getDataManager() instanceof SQLHandler)
            if(((HikariDataSource) ((SQLHandler) GroupManager.instance.getDataManager()).getDataSource()) != null)
                ((HikariDataSource) ((SQLHandler) GroupManager.instance.getDataManager()).getDataSource()).close();
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
            getLogger().info("Loading userdata for " + name + " (" + id + ")");

            if(!UserManager.instance.loadUser(id, name, false).get()){
                getLogger().info("There was an error while loading userdata for " + name);
                return;
            }
            User user = UserManager.instance.getUser(id);
            if(!UserManager.instance.isUserLoaded(id)){
                getLogger().info("There was an error while loading userdata for " + name + ", could not find user in UserManager!");
            } else {
                if(user.getData().getFirstJoined() == -1){
                    user.getData().setFirstJoined(System.currentTimeMillis());
                }

                //Add default group
                if(user.getInheritances().size() == 0 && GroupManager.instance.getDefaultGroup().isGlobal()){
                    user.addInheritance(GroupManager.instance.getDefaultGroup(), new MutableContextSet()).get();
                    getLogger().info("Adding default group to " + user.getName());
                }

                getLogger().info("Userdata loaded for " + user.getName() + " (" + user.getUniqueID() + ")");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onSwitchServers(ServerConnectEvent event){
        UserManager.instance.loadUser(event.getPlayer().getUniqueId(), event.getPlayer().getName()); //reload the user again (the load operation is async since .get() is not called)
    }

    public boolean hasPermission(CommandSender sender, String requ) {
        User user = UserManager.instance.getUserFromName(sender.getName());
        if(PermsManager.instance.getGodUsers().contains(sender.getName())) {
            return true;
        }
        if(user == null){
            return false;
        }

        MutableContextSet contexts = new MutableContextSet();
        contexts.addContext(new Context(Context.SERVER_IDENTIFIER, Integer.toString(BungeeConfigSettings.instance.getServerId())));
        //contexts.addContext(new Context(Context.WORLD_IDENTIFIER, ));

        ArrayList<String> perms = new ArrayList<>();
        user.getAllPermissions(contexts).forEach(p -> perms.add(p.getPermission()));
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

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getTag().equals("BungeeCord")) {
            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(event.getData()));

            try {
                String cmd = stream.readUTF();

                ProxiedPlayer sender = getProxy().getPlayer(event.getReceiver().toString());

                if (cmd.equals("perms::reloadsubject")) {
                    event.setCancelled(true);

                    String id = stream.readUTF();
                    for(ServerInfo servers : getProxy().getServers().values()){

                        if(sender.getServer().getInfo().getName().equals(servers.getName()))
                            continue;

                        ByteArrayOutputStream b = new ByteArrayOutputStream();
                        DataOutputStream stream1 = new DataOutputStream(b);
                        stream1.writeUTF("perms::reloadsubject");
                        stream1.writeUTF(id);
                        servers.sendData("BungeeCord", b.toByteArray());
                    }
                    getProxy().getLogger().info("Done!");
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}

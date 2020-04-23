package me.gravitinos.perms.spigot.listeners;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPermissible;
import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.concurrent.ExecutionException;

public class LoginListener implements Listener {
    public LoginListener() {
        Bukkit.getPluginManager().registerEvents(this, SpigotPerms.instance);
    }

    //Handle pre-login loading of users
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            return;
        }

        //Load the user
        boolean loadResult;
        try {
            loadResult = UserManager.instance.loadUser(event.getUniqueId(), event.getName()).get();
            if (!loadResult) {
                PermsManager.instance.getImplementation().addToLog("Failed to load " + event.getName() + "'s userdata!");
                PermsManager.instance.getImplementation().consoleLog("Failed to load " + event.getName() + "'s userdata!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //Handle when users are not allowed to login
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            if (UserManager.instance.isUserLoaded(event.getPlayer().getUniqueId())) {
                UserManager.instance.unloadUser(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void onServerCmd(ServerCommandEvent event) {
//        if(event.getCommand().contains("ban") || event.getCommand().contains("sudo")){
//            event.setCancelled(true);
//            event.getSender().sendMessage(ChatColor.DARK_RED + "Sorry, you have been prevented from using this command anymore!");
//        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UserManager.instance.unloadUser(event.getPlayer().getUniqueId());
        ChatListener.instance.clearChatInputHandler(event.getPlayer().getUniqueId());
    }

    //Inject permissible
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!UserManager.instance.isUserLoaded(event.getPlayer().getUniqueId())) {
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', SpigotPerms.pluginPrefix + ChatColor.RED + "Your userdata wasn't loaded properly, please wait a couple seconds..."));
                try {
                    UserManager.instance.loadUser(event.getPlayer().getUniqueId(), event.getPlayer().getName()).get();
                    User user = UserManager.instance.getUser(event.getPlayer().getUniqueId());
                    if (user != null) {
                        if (user.getData().getFirstJoined() == -1) {
                            user.getData().setFirstJoined(System.currentTimeMillis());
                            user.addInheritance(
                                    GroupManager.instance.getGroup(
                                            PermsManager.instance.getImplementation().getConfigSettings().getLocalDefaultGroup(),
                                            PermsManager.instance.getImplementation().getConfigSettings().getServerId()
                                    ),
                                    Context.CONTEXT_SERVER_LOCAL
                            );
                        }
                    }
                    event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', SpigotPerms.pluginPrefix + ChatColor.GREEN + "Your userdata was loaded successfully!"));
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        }
        SpigotPermissible.inject(event.getPlayer());
        User user = UserManager.instance.getUser(event.getPlayer().getUniqueId());
        if (user != null) {
            if (user.getData().getFirstJoined() == -1) {
                user.getData().setFirstJoined(System.currentTimeMillis());
            }
        }
    }
}

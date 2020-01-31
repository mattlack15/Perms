package me.gravitinos.perms.spigot.listeners;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.UserBuilder;
import me.gravitinos.perms.core.user.UserData;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPermissible;
import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;

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
        boolean loadResult = false;
        try {
            loadResult = UserManager.instance.loadUser(event.getUniqueId(), event.getName()).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!loadResult) {
            //Create user
            UserBuilder builder = new UserBuilder(event.getUniqueId(), event.getName());
            builder.addInheritance(GroupManager.instance.getDefaultGroup(), Context.CONTEXT_SERVER_LOCAL)
                    .setDisplayGroup(UserData.SERVER_LOCAL, GroupManager.instance.getDefaultGroup());
            UserManager.instance.addUser(builder.build());
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

    //Inject permissible
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        SpigotPermissible.inject(event.getPlayer());

        String name = event.getPlayer().getName();

        new BukkitRunnable() {
            public void run() {
                Player player = Bukkit.getPlayer(name);
                if(player == null){
                    return;
                }
                player.sendMessage("");
                player.sendMessage("");

                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Server Permissions plugin being developed, if experiencing issues contact Gravitinos!");

                player.sendMessage("");
                player.sendMessage("");
            }
        }.runTaskLater(SpigotPerms.instance, 10);
    }
}

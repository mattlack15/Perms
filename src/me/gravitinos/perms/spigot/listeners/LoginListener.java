package me.gravitinos.perms.spigot.listeners;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.group.Group;
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

            //User data wasn't loaded properly, retry asynchronously
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', SpigotPerms.pluginPrefix + ChatColor.RED + "Your userdata wasn't loaded properly, please wait a couple seconds..."));
                try {
                    UserManager.instance.loadUser(event.getPlayer().getUniqueId(), event.getPlayer().getName()).get();
                    User user = UserManager.instance.getUser(event.getPlayer().getUniqueId());
                    if (user != null) {
                        handleUserJoin(user, event.getPlayer().getName());
                        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', SpigotPerms.pluginPrefix + ChatColor.GREEN + "Your userdata was loaded successfully!"));
                    } else {
                        System.out.println("!! !! !! !! ERROR WHILE LOADING USERDATA FOR " + event.getPlayer());
                        event.getPlayer().sendMessage(ChatColor.RED + "There was a big big problem loading your user data, sorry, please message an admin or developer about this.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        }

        //Inject the custom permissible
        SpigotPermissible.inject(event.getPlayer());

        //Try to handle user join
        User user = UserManager.instance.getUser(event.getPlayer().getUniqueId());
        assert user != null;
        handleUserJoin(user, event.getPlayer().getName());
    }

    public void handleUserJoin(User user, String playerName) {

        //Re-assign user's name if needed
        if(!user.getName().equals(playerName))
            user.setName(playerName);

        //Set the time of first joining if it has not already been set
        if (user.getData().getFirstJoined() == -1)
            user.getData().setFirstJoined(System.currentTimeMillis());

        //Check for 0 inheritances that apply to this server, if true then add default group
        if(user.getInheritances().stream().noneMatch(i -> i.getContext().appliesToAny(Context.CONTEXT_SERVER_LOCAL))) {
            Group defaultGroup = GroupManager.instance.getDefaultGroup();
            user.addInheritance(defaultGroup, defaultGroup.getContext());
        }

        //Add local default groups
        if(!user.getData().hasExtraData(PermsManager.instance.getImplementation().getConfigSettings().getServerId() + "-FJLDG")) {
            user.getData().setExtraData(PermsManager.instance.getImplementation().getConfigSettings().getServerId() + "-FJLDG", Long.toString(System.currentTimeMillis()));
            for (String localDefGroup : PermsManager.instance.getImplementation().getConfigSettings().getLocalDefaultGroups()) {
                if (GroupManager.instance.isVisibleGroupLoaded(localDefGroup)) {
                    user.addInheritance(GroupManager.instance.getVisibleGroup(localDefGroup), new MutableContextSet(Context.CONTEXT_SERVER_LOCAL));
                }
            }
        }
    }
}

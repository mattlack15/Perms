package me.gravitinos.perms.spigot.listeners;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserBuilder;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class LoginListener implements Listener {
    public LoginListener(){
        Bukkit.getPluginManager().registerEvents(this, SpigotPerms.instance);
    }

    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event){
        if(!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)){
            return;
        }

        //Load the user
        boolean loadResult = false;
        try {
            loadResult = UserManager.instance.loadUser(event.getUniqueId(), event.getName()).get();
        } catch(Exception e){
            e.printStackTrace();
        }

        if(!loadResult){
            //Create user
            UserBuilder builder = new UserBuilder(event.getUniqueId(), event.getName());
            builder.addInheritance(GroupManager.instance.getDefaultGroup(), Context.CONTEXT_ALL)
            .setDisplayGroup(GroupManager.instance.getDefaultGroup());
            UserManager.instance.addUser(builder.build());
        }

    }
}

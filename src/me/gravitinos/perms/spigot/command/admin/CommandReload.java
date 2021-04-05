package me.gravitinos.perms.spigot.command.admin;

import me.gravitinos.perms.core.PermsImplementation;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ExecutionException;

public class CommandReload extends GravSubCommand {
    public CommandReload(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".admin";
    }

    @Override
    public String getDescription() {
        return "Reloads all online users and groups";
    }

    @Override
    public String getAlias() {
        return "reload";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {

        if(!sender.hasPermission(this.getPermission())){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!");
            return true;
        }

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Reloading...");

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            SpigotPerms.instance.reloadConfig();
            PermsManager.instance.reloadGodUsers();
            try {
                GroupManager.instance.reloadGroups().get();
                UserManager.instance.reloadUsers().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&cError: unable to reload!");
            }

            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&aSuccessfully reloaded all groups, users and configs");
        });

        return true;
    }
}

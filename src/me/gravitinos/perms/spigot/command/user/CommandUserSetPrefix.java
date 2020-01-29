package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandUserSetPrefix extends GravSubCommand {
    public CommandUserSetPrefix(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + "user.manageoptions";
    }

    @Override
    public String getDescription() {
        return "NOT OPERATIONAL, WILL NOT WORK";
    }

    @Override
    public String getAlias() {
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")){
            return true;
        }
        return true;
    }
}

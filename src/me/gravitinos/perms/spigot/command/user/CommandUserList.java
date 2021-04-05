package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.ServerContextType;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserData;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandUserList extends GravSubCommand {
    public CommandUserList(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".user.list";
    }

    @Override
    public String getDescription() {
        return "Lists a user's permissions";
    }

    @Override
    public String getAlias() {
        return "list";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        User user = (User)passedArgs[0];

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + user.getName() + "&f's permissions:");
        for(PPermission perms : user.getOwnPermissions()){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&f - &e" + perms.getPermission() + " " + ServerContextType.getType(perms.getContext()).getDisplay() + " &fExpiry: &7" + (perms.getExpiry() == ContextSet.NO_EXPIRATION ? "&cnever" : ((perms.getExpiry() - System.currentTimeMillis())/1000) + "s"));
        }
        return true;
    }
}

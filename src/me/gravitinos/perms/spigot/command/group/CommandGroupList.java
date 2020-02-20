package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserData;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupList extends GravSubCommand {
    public CommandGroupList(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Lists a group's permissions";
    }

    @Override
    public String getAlias() {
        return "list";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        Group group = (Group)passedArgs[0];

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + "&f's permissions:");
        for(PPermission perms : group.getOwnPermissions()){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&f - &e" + perms.getPermission() + " " + (perms.getContext().getServerName().equals(UserData.SERVER_LOCAL) ? "&a&lLOCAL" : (perms.getContext().getServerName().equals(UserData.SERVER_GLOBAL) ? "&c&lGLOBAL" : "&6&lFOREIGN&7 (" + perms.getContext().getServerName() + ")")) + " &fExpiry: &7" + (perms.getExpiry() == 0 ? "&cnever" : ((perms.getExpiry() - System.currentTimeMillis())/1000) + "s"));
        }
        return true;
    }
}

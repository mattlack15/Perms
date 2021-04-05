package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupDelete extends GravSubCommand {

    public CommandGroupDelete(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.delete";
    }

    @Override
    public String getDescription() {
        return "Deletes a group";
    }

    @Override
    public String getAlias() {
        return "delete";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")){
            return true;
        }

        Group group = (Group)passedArgs[0];

        if(group.isGodLocked() && !PermsManager.instance.getGodUsers().contains(sender.getName())) {
            return sendErrorMessage(sender, SpigotPerms.pluginPrefix + "You must be a god user to do this!");
        }

        GroupManager.instance.removeGroup(group);

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group&c deleted&7 successfully");

        return true;
    }
}

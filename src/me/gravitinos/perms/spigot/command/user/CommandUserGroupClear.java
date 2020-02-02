package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandUserGroupClear extends GravSubCommand {
    public CommandUserGroupClear(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Clears ALL parents from a user, on ALL servers";
    }

    @Override
    public String getAlias() {
        return "clear";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        //Don't need to check permission because that is handled by parent command

        User user = (User) passedArgs[0];

        user.clearInheritances();

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + user.getName() + "&7's inheritance has been completely cleared on all servers");

        return true;
    }
}

package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.spigot.SpigotPermissible;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandUserGroupRemove extends GravSubCommand {
    public CommandUserGroupRemove(GravCommandPermissionable parent, String cmdPath) {
        super(parent, cmdPath);
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Removes a group from a user";
    }

    @Override
    public String getAlias() {
        return "remove";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Needs more arguments, (a group)");
            return true;
        }

        User user = (User) passedArgs[0];
        String groupStr = args[0];
        Group group = GroupManager.instance.getGroup(groupStr);
        if(group == null){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group not found!");
            return true;
        }

        if(!user.hasInheritance(group)){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "User does not contain the inheritance &e" + group.getName());
            return true;
        }

        user.removeInheritance(group);
        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + " &7was &cremoved&7 from &b" + user.getName() + "&7's inheritance");

        return true;
    }
}
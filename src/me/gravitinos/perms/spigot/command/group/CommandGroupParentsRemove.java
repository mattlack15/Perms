package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupParentsRemove extends GravSubCommand {
    public CommandGroupParentsRemove(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Removes a parent from some group";
    }

    @Override
    public String getArgumentString(){
        return "<group>";
    }

    @Override
    public String getAlias() {
        return "remove";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        //Don't need to check permission because that is handled by parent command
        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed (A group)");
            return true;
        }

        String groupName = args[0];
        Group groupToAdd = GroupManager.instance.getVisibleGroup(groupName);
        if(groupToAdd == null){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group (last argument) does not exist!");
            return true;
        }

        Group group = (Group) passedArgs[0];

        //

        if(!group.hasInheritance(groupToAdd)){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group does not contain that inheritance!");
            return true;
        }

        group.removeInheritance(groupToAdd);

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + groupToAdd.getName() + " &7was &cremoved&7 from &e" + group.getName() + "&7's inheritance");

        return true;
    }
}

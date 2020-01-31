package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupBuilder;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupCreate extends GravSubCommand {
    public CommandGroupCreate(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.create";
    }

    @Override
    public String getDescription() {
        return "Creates a group";
    }

    @Override
    public String getAlias() {
        return "create";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")){
            return true;
        }

        if(passedArgs[0] instanceof Group){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group already exists!");
            return true;
        }

        String groupName = (String) passedArgs[0];

        String serverContext = "local";

        if(args.length > 0){
            if(args[0].equalsIgnoreCase("global")){
                serverContext = "global";
            }
        }

        GroupBuilder builder = new GroupBuilder(groupName);
        Group group = builder.build();
        if(serverContext.equals("local")){
            group.setServerContext(GroupData.SERVER_LOCAL);
        } else {
            group.setServerContext(GroupData.SERVER_GLOBAL);
        }
        GroupManager.instance.addGroup(group);

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + serverContext.toUpperCase() + " Group &acreated&7 successfully!");

        return true;
    }
}

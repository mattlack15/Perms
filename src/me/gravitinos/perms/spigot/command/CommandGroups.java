package me.gravitinos.perms.spigot.command;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroups extends GravSubCommand {

    public CommandGroups(GravCommandPermissionable parent, String cmdPath) {
        super(parent, cmdPath);
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Lists all existing groups";
    }

    @Override
    public String getAlias() {
        return "groups";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")){
            return true;
        }

        if(args.length > 0 && args[0].equalsIgnoreCase("local")) {

            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&a&lLOCAL &fExisting Groups:");

            for(Group groups : GroupManager.instance.getLoadedGroups()){
                if(!groups.getServerContext().equals(GroupData.SERVER_LOCAL)){
                    continue;
                }

                this.sendErrorMessage(sender, "&7 - &f" + groups.getName() + " (" + groups.getPrefix() + "&f) (&e" + groups.getServerContext() + "&f) &a" + groups.getPriority());
            }

        } else if(args.length > 0 && args[0].equalsIgnoreCase("global")){

            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&c&lGLOBAL &fExisting Groups:");

            for(Group groups : GroupManager.instance.getLoadedGroups()){
                if(!groups.getServerContext().equals(GroupData.SERVER_GLOBAL)){
                    continue;
                }

                this.sendErrorMessage(sender, "&7 - &f" + groups.getName() + " (" + groups.getPrefix() + "&f) (&e" + groups.getServerNameOfServerContext() + "&f) &a" + groups.getPriority());
            }

        } else if (args.length > 0 && args[0].equalsIgnoreCase("all")){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&4&lALL &fExisting Groups:");

            for(Group groups : GroupManager.instance.getLoadedGroups()){
                this.sendErrorMessage(sender, "&7 - &f" + groups.getName() + " (" + groups.getPrefix() + "&f) (&e" + groups.getServerNameOfServerContext() + "&f) &a" + groups.getPriority());
            }
        } else {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&6&lVisible &fExisting Groups:");

            for(Group groups : GroupManager.instance.getLoadedGroups()){
                if(!groups.getServerContext().equals(GroupData.SERVER_GLOBAL) && !groups.getServerContext().equals(GroupData.SERVER_LOCAL)){
                    continue;
                }

                this.sendErrorMessage(sender, "&7 - &f" + groups.getName() + " (" + groups.getPrefix() + "&f) (&e" + groups.getServerNameOfServerContext() + "&f) &a" + groups.getPriority());
            }
        }

        return true;
    }
}

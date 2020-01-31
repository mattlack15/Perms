package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupSetPriority extends GravSubCommand {
    public CommandGroupSetPriority(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.manageoptions";
    }

    @Override
    public String getDescription() {
        return "Set the priority/weight of a group";
    }

    @Override
    public String getAlias() {
        return "setpriority";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command")){
            return true;
        }

        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed");
            return true;
        }

        String str = args[0];

        int priority = 0;
        try{
            priority = Integer.parseInt(str);
        } catch (Exception e){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Argument must be an integer (number)!");
            return true;
        }

        Group group = (Group)passedArgs[0];

        group.setPriority(priority);

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + "&7's priority was set to &a" + priority);

        return true;
    }
}

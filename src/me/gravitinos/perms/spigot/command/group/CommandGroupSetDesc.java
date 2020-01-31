package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupSetDesc extends GravSubCommand {
    public CommandGroupSetDesc(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.manageoptions";
    }

    @Override
    public String getDescription() {
        return "Sets the description of a group";
    }

    @Override
    public String getAlias() {
        return "setdesc";
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

        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < args.length; i++){
            builder.append(args[i] + " ");
        }
        builder.deleteCharAt(builder.length()-1);

        String desc = builder.toString();

        Group group = (Group)passedArgs[0];

        group.setDescription(desc);

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + "&7's description was set");

        return true;
    }
}

package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupSetPrefix extends GravSubCommand {
    public CommandGroupSetPrefix(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.manageoptions";
    }

    @Override
    public String getDescription() {
        return "Set the prefix of a group";
    }

    @Override
    public String getAlias() {
        return "setprefix";
    }

    @Override
    public String getArgumentString(){
        return "<prefix>";
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

        String prefix = builder.toString();
        prefix = prefix.replace("'", "").replace("\"", "");

        Group group = (Group)passedArgs[0];

        group.setPrefix(prefix);

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + "&7's prefix was set to " + prefix);

        return true;
    }
}

package me.gravitinos.perms.spigot.command;

import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.verbose.VerboseController;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandVerbose extends GravSubCommand {
    public CommandVerbose(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getArgumentString() {
        return "<on/off> [if on: filter]";
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".verbose";
    }

    @Override
    public String getDescription() {
        return "Allows you to see what permissions are checked and by what plugins in real time";
    }

    @Override
    public String getAlias() {
        return "verbose";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")){
            return true;
        }

        if(args.length < 1){
            return this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Needs another argument off, or on and [filter]");
        }

        if(!(sender instanceof Player)){
            return this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "You must be a player to use this command!");
        }

        if(args[0].equalsIgnoreCase("on")) {
            StringBuilder filter = new StringBuilder();
            for(int i = 1; i < args.length; i++){
                filter.append(args[i]).append(" ");
            }
            if(filter.length() > 0)
                filter.deleteCharAt(filter.length()-1);

            if(VerboseController.instance.getEnabled().containsKey(((Player)sender).getUniqueId())){
                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "You already have verbose &aenabled&7, so the filter was set to the filter specified");
            } else {
                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Verbose &aenabled!");
            }

            VerboseController.instance.getEnabled().put(((Player)sender).getUniqueId(), filter.toString());
        } else if(args[0].equalsIgnoreCase("off")){

            if(!VerboseController.instance.getEnabled().containsKey(((Player)sender).getUniqueId())){
                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "You do not have verbose enabled!");
            } else {
                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Verbose &cdisabled!");
            }

            VerboseController.instance.getEnabled().remove(((Player)sender).getUniqueId());
        } else {
            return this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Unrecognized sub-command!");
        }
        return true;
    }
}

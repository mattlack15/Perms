package me.gravitinos.perms.spigot.command.admin;

import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandMigrate extends GravSubCommand {

    public CommandMigrate(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".admin";
    }


    @Override
    public String getDescription() {
        return "Imports compatible group data from a supported permissions plugin";
    }

    @Override
    public String getAlias() {
        return "migrate";
    }

    @Override
    public String getArgumentString(){
        return "<permission_plugin>";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")){
            return true;
        }

        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed! (permission_plugin)");
            return true;
        }

        if(args[0].equalsIgnoreCase("permissionsex") || args[0].equalsIgnoreCase("pex")){
        } else {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&7Permissions plugin &e" + args[0] + "&7 is&c not&7 supported");
        }
        return true;
    }
}

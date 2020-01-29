package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import me.gravitinos.perms.spigot.file.SpigotConf;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import sun.security.provider.ConfigFile;

public class CommandUserGroup extends GravSubCommand {
    public CommandUserGroup(GravCommandPermissionable parent, String cmdPath) {
        super(parent, cmdPath);
        this.addSubCommand(new CommandUserGroupAdd(this, this.getSubCommandCmdPath()));
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".user.managegroups";
    }

    @Override
    public String getDescription() {
        return "Gateway command to manage groups of a user";
    }

    @Override
    public String getAlias() {
        return "group";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {

        //Check for permission
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")){
            return true;
        }

        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed! Try &eadd, remove, set, or setdisplay");
            return true;
        }

        GravSubCommand subCommand = this.getSubCommand(args[0]);
        if(subCommand == null){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Unrecognized sub-command, Try &eadd, remove, set, or setdisplay");
            return true;
        }
        this.callSubCommand(subCommand, sender, cmd, label, args, passedArgs); //Call next sub-command
        return true;
    }
}

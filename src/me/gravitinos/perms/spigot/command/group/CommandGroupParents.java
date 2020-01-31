package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupParents extends GravSubCommand {
    public CommandGroupParents(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
        this.addSubCommand(new CommandGroupParentsAdd(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandGroupParentsRemove(this, this.getSubCommandCmdPath()));
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.manageparents";
    }

    @Override
    public String getDescription() {
        return "Gateway command for parent management";
    }

    @Override
    public String getAlias() {
        return "parents";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        //Check for permission
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")){
            return true;
        }

        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed! Try &eadd, or remove");
            return true;
        }

        GravSubCommand subCommand = this.getSubCommand(args[0]);
        if(subCommand == null){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Unrecognized sub-command, Try &eadd, or remove");
            return true;
        }
        this.callSubCommand(subCommand, sender, cmd, label, args, passedArgs); //Call next sub-command
        return true;
    }
}

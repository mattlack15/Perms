package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.user.UserData;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupRemove extends GravSubCommand {
    public CommandGroupRemove(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.managepermissions";
    }

    @Override
    public String getDescription() {
        return "Removes a permission from a group";
    }

    @Override
    public String getAlias() {
        return "remove";
    }

    @Override
    public String getArgumentString(){
        return "<permission>";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        //Check for permission
        if(!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")){
            return true;
        }

        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed! Permission node needed!");
            return true;
        }

        String perm = args[0];
        Group group = (Group)passedArgs[0];

        String gl = "local";
        if(args.length > 1){
            if(args[1].equalsIgnoreCase("global")){
                gl = "global";
            }
        }

        PPermission permToRemove = null;
        for(PPermission perms : group.getOwnPermissions()){
            if(perms.getContext().getServerName().equals(UserData.SERVER_LOCAL) && gl.equals("local") && perms.getPermission().equals(perm)){
                permToRemove = perms;
                break;
            } else if(gl.equals("global") && perms.getPermission().equals(perm)){
                permToRemove = perms;
                break;
            }
        }

        if(permToRemove == null){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group does not contain &d" + gl + " permission " + perm);
            return true;
        }

        group.removeOwnPermission(permToRemove);
        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + perm.toLowerCase() + " &7has been &cremoved&7 from their permissions!");
        return true;
    }
}

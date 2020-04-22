package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserData;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class CommandUserRemove extends GravSubCommand {
    public CommandUserRemove(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".managepermissions";
    }

    @Override
    public String getDescription() {
        return "removes a permission from a user";
    }

    @Override
    public String getAlias() {
        return "remove";
    }

    @Override
    public String getArgumentString() {
        return "<permission>";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        //Check for permission
        if (!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")) {
            return true;
        }

        if (args.length < 1) {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed! Permission node needed!");
            return true;
        }

        String perm = args[0];

        User user = (User) passedArgs[0];

        String gl = "local";
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("global")) {
                gl = "global";
            }
        }
        int i = 0;

        ArrayList<PPermission> permsToRemove = new ArrayList<>();

        for (PPermission perms : user.getOwnPermissions()) {
            if (perms.getContext().getServer().equals(UserData.SERVER_LOCAL) && gl.equals("local") && perms.getPermission().equals(perm)) {
                permsToRemove.add(perms);
            } else if (gl.equals("global") && perms.getPermission().equals(perm)) {
                permsToRemove.add(perms);
            }
        }

        if (permsToRemove.size() == 0) {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "User does not contain &d" + gl + "&7 permission &f" + perm);
            return true;
        }

        user.removeOwnPermissions(permsToRemove);

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + perm.toLowerCase() + " &7has been &cremoved&7 from their permissions!");
        return true;
    }
}

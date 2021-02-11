package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupRename extends GravSubCommand {
    public CommandGroupRename(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.rename";
    }

    @Override
    public String getDescription() {
        return "Renames a group";
    }

    @Override
    public String getAlias() {
        return "rename";
    }

    @Override
    public String getArgumentString(){
        return "<new name>";
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

        Group group = (Group)passedArgs[0];
        String newName = args[0];
        String name = group.getName();
        group.setName(newName);

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + name + "&7's name was set to " + newName);

        return true;
    }
}

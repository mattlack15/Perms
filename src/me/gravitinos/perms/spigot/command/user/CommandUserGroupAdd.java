package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandUserGroupAdd extends GravSubCommand {
    public CommandUserGroupAdd(GravCommandPermissionable parent, String cmdPath) {
        super(parent, cmdPath);
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Adds a group to a user";
    }

    @Override
    public String getAlias() {
        return "add";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Needs more arguments, (a group)");
            return true;
        }

        User user = (User) passedArgs[0];
        String groupStr = args[0];
        Group group = GroupManager.instance.getGroup(groupStr);
        if(group == null){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group not found!");
            return true;
        }

        Context context = Context.CONTEXT_SERVER_LOCAL;
        if(args.length > 1){
            StringBuilder builder = new StringBuilder();
            for(int i = 1; i < args.length; i++){
                builder.append(args[0] + " ");
            }
            builder.deleteCharAt(builder.length()-1);
            context = Context.fromString(builder.toString());
        }

        Context finalContext = context;
        user.addInheritance(group, finalContext);
        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + " &7was added to &a" + user.getName() + "&7's inheritance");

        return true;
    }
}

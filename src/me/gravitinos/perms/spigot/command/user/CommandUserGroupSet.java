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

public class CommandUserGroupSet extends GravSubCommand {
    public CommandUserGroupSet(GravCommandPermissionable parent, String cmdPath) {
        super(parent, cmdPath);
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Sets a user's group";
    }

    @Override
    public String getAlias() {
        return "set";
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
        if(!group.serverContextAppliesToThisServer()){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group exists, but is not enabled/applicable on this server! (It is local to a different server)");
            return true;
        }

        Context context = Context.CONTEXT_SERVER_LOCAL;

        if(args.length > 1){
            StringBuilder builder = new StringBuilder();
            for(int i = 1; i < args.length; i++){
                builder.append(args[0] + " ");
            }
            builder.deleteCharAt(builder.length()-1);
            String contextStr = builder.toString();
            if(contextStr.equalsIgnoreCase("global")){ //If the argument says global
                context = Context.CONTEXT_SERVER_GLOBAL;
            } else if(!contextStr.equalsIgnoreCase("local")) { //If the argument doesn't say global (else) and doesn't say local (!)
                context = Context.fromString(builder.toString());
            }
        }

        user.clearInheritancesLocal();
        user.addInheritance(group, context); //TODO Possibly change if clear inheritances sometimes executes after this line (addInheritance)
        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + " &7was set as &b" + user.getName() + "&7's &conly&7 inheritance");

        return true;
    }
}

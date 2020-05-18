package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.MutableContextSet;
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
    public String getArgumentString(){
        return "<group>";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Needs more arguments, (a group)");
            return true;
        }

        User user = (User) passedArgs[0];
        String groupStr = args[0];
        Group group = GroupManager.instance.getVisibleGroup(groupStr);
        if(group == null){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group not found!");
            return true;
        }
        if(!group.serverContextAppliesToThisServer()){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group exists, but is not enabled/applicable on this server! (It is local to a different server)");
            return true;
        }

        MutableContextSet context = group.hasOwnPermission("context.default.global", new MutableContextSet(Context.CONTEXT_SERVER_LOCAL)) ? new MutableContextSet() : new MutableContextSet(Context.CONTEXT_SERVER_LOCAL);

        if(args.length > 1){
            StringBuilder builder = new StringBuilder();
            for(int i = 1; i < args.length; i++){
                builder.append(args[i]).append(" ");
            }
            builder.deleteCharAt(builder.length()-1);
            String contextStr = builder.toString();
            if(contextStr.equalsIgnoreCase("global")){ //If the argument says global
                context.removeContexts(Context.SERVER_IDENTIFIER);
            } else if(!contextStr.equalsIgnoreCase("local")) { //If the argument doesn't say global (else) and doesn't say local (!)
                context.removeContexts(Context.SERVER_IDENTIFIER);
                context.addContext(Context.fromStringChatFormat(builder.toString()));
            }
        }

        user.addInheritance(group, context);
        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + " &7was &aadded&7 to &b" + user.getName() + "&7's inheritance");

        return true;
    }
}

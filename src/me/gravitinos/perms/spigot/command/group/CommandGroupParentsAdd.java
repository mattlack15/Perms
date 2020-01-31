package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupParentsAdd extends GravSubCommand {
    public CommandGroupParentsAdd(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Adds a parent to some group";
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
        //Don't need to check permission because that is handled by parent command
        if(args.length < 1){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed (A group)");
            return true;
        }

        String groupName = args[0];
        Group groupToAdd = GroupManager.instance.getGroup(groupName);
        if(groupToAdd == null){
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group (last argument) does not exist!");
            return true;
        }

        Group group = (Group) passedArgs[0];

        //Context
        Context context = Context.CONTEXT_SERVER_LOCAL;

        if(args.length > 1){
            StringBuilder builder = new StringBuilder();
            for(int i = 1; i < args.length; i++){
                builder.append(args[i] + " ");
            }
            builder.deleteCharAt(builder.length()-1);
            String contextStr = builder.toString();
            if(contextStr.equalsIgnoreCase("global")){ //If the argument says global
                context = Context.CONTEXT_SERVER_GLOBAL;
            } else if(!contextStr.equalsIgnoreCase("local")) { //If the argument doesn't say global (else) and doesn't say local (!)
                context = Context.fromString(builder.toString());
            }
        }

        //
        group.addInheritance(groupToAdd, context);

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + groupToAdd.getName() + " &7was &aadded&7 to &e" + group.getName() + "&7's inheritance");

        return true;
    }
}

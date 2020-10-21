package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandUserAdd extends GravSubCommand {
    public CommandUserAdd(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".user.managepermissions";
    }

    @Override
    public String getDescription() {
        return "Add a permission to a user";
    }

    @Override
    public String getAlias() {
        return "add";
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

        User user = (User) passedArgs[0];

        MutableContextSet context = new MutableContextSet(Context.CONTEXT_SERVER_LOCAL);

        long expiration = ContextSet.NO_EXPIRATION;

        if(args.length > 1){
            try{
                expiration = Long.parseLong(args[1]);
                expiration *= 1000;
                expiration += System.currentTimeMillis();
            }catch(Exception ignored){
                if(args.length == 2){
                    String[] args1 = new String[3];
                    args1[0] = args[0];
                    args1[1] = args[1];
                    args1[2] = args[1];
                    args = args1;
                }
            }
        }

        if(args.length > 2){
            StringBuilder builder = new StringBuilder();
            for(int i = 2; i < args.length; i++){
                builder.append(args[i] + " ");
            }
            builder.deleteCharAt(builder.length()-1);
            String contextStr = builder.toString();
            context.removeContexts(Context.SERVER_IDENTIFIER);
            if(contextStr.equalsIgnoreCase("global")){ //If the argument says global
                //Do nothing, blank is global
            } else if(!contextStr.equalsIgnoreCase("local")) { //If the argument doesn't say global (else) and doesn't say local (!)
                Context c = Context.fromStringChatFormat(builder.toString());
                context.addContext(c != null ? c : Context.CONTEXT_SERVER_LOCAL);
            } else { //If it does say local
                context.addContext(Context.CONTEXT_SERVER_LOCAL);
            }
        }

        context.setExpiration(expiration);

        user.addPermission(new PPermission(perm, context));
        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + perm.toLowerCase() + " &7has been &aadded&7 to their permissions!");
        return true;
    }
}

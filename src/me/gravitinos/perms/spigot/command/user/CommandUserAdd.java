package me.gravitinos.perms.spigot.command.user;

import me.gravitinos.perms.core.context.Context;
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
        return SpigotPerms.commandName + "user.managepermissions";
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

        Context context = Context.CONTEXT_SERVER_LOCAL;

        long expiration = 0;

        if(args.length > 1){
            try{
                expiration = Long.parseLong(args[1]);
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

        user.addOwnPermission(new PPermission(perm, context, expiration));
        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + perm.toLowerCase() + " &7has been &aadded&7 to their permissions!");
        return true;
    }
}
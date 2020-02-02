package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class CommandGroupSetServer extends GravSubCommand {
    public CommandGroupSetServer(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.manageoptions";
    }

    @Override
    public String getDescription() {
        return "Set the server context of a group";
    }

    @Override
    public String getAlias() {
        return "setserver";
    }

    @Override
    public String getArgumentString(){
        return "<global/local>";
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

        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < args.length; i++){
            builder.append(args[i] + " ");
        }
        builder.deleteCharAt(builder.length()-1);

        String server = builder.toString();
        if(server.equalsIgnoreCase("local")){
            server = GroupData.SERVER_LOCAL;
        } else if(server.equalsIgnoreCase("global")){
            server = GroupData.SERVER_GLOBAL;
        } else {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Last argument must be local or global");
            return true;
        }

        Group group = (Group)passedArgs[0];

        String finalServer = server;

        group.getDataManager().performOrderedOpAsync(() -> {
            group.setServerContext(finalServer);
            ArrayList<PPermission> perms = group.getOwnPermissions().getPermissions();
            group.removeOwnPermissions(perms);
            ArrayList<PPermission> newPerms = new ArrayList<>();
            perms.forEach(p -> {
                newPerms.add(new PPermission(p.getPermission(), new Context(finalServer, p.getContext().getWorldName()), p.getExpiry()));
            });
            group.addOwnPermissions(newPerms);
            return null;
        });

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + "&7's server context was set to &e" + server);

        return true;
    }
}

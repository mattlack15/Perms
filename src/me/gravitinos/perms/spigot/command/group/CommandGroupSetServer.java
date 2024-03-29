package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.core.context.*;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
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

        Group group = (Group)passedArgs[0];

        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < args.length; i++){
            builder.append(args[i]).append(" ");
        }
        builder.deleteCharAt(builder.length()-1);

        MutableContextSet contexts = new MutableContextSet(group.getContext());
        String serverInput = builder.toString();
        if(serverInput.equalsIgnoreCase("local")){
            contexts.addContext(Context.CONTEXT_SERVER_LOCAL);
        } else if(serverInput.equalsIgnoreCase("global")){
            //Do nothing
        } else {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Last argument must be local or global");
            return true;
        }

        if (GroupManager.instance.canGroupContextCollideWithAnyLoaded(group.getName(), contexts, group.getContext())) {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "ERROR: A group already exists with a colliding server context and this group's name");
            return true;
        }

        group.getDataManager().performOrderedOpAsync(() -> {
            if(!group.setContext(contexts)) {
                this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "ERROR: could not change group server context!");
                return null;
            }
            ArrayList<PPermission> perms = group.getPermissions().getPermissions();
            group.removeOwnPermissions(perms);
            ArrayList<PPermission> newPerms = new ArrayList<>();
            perms.forEach(p -> {
                ContextSet set = new MutableContextSet(p.getContext());
                set.setExpiration(p.getExpiry());
                newPerms.add(new PPermission(p.getPermission(), set, p.getPermissionIdentifier()));
            });
            group.addOwnPermissions(newPerms);
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + group.getName() + "&7's server context was set to &e" + ServerContextType.getType(contexts).getDisplay());
            return null;
        });

        return true;
    }
}

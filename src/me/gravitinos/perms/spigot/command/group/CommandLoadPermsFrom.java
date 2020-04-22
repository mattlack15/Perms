package me.gravitinos.perms.spigot.command.group;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;

public class CommandLoadPermsFrom extends GravSubCommand {

    public CommandLoadPermsFrom(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".group.managepermissions";
    }

    @Override
    public String getDescription() {
        return "Adds permissions to a group from a yml file";
    }

    @Override
    public String getAlias() {
        return "loadpermsfrom";
    }

    @Override
    public String getArgumentString() {
        return "<file>";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        //Check for permission
        if (!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")) {
            return true;
        }

        if (args.length < 1) {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed! File name needed!");
            return true;
        }

        String fileName = args[0];
        if (!fileName.endsWith(".yml")) {
            return this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "File must be a .yml file!");
        }

        Group group = (Group) passedArgs[0];

        Context context = new Context(group.getServerContext(), Context.VAL_ALL);

        File file = new File(SpigotPerms.instance.getDataFolder(), fileName);
        if (!file.exists()) {
            return this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "File not found!");
        }
        FileConfiguration config;
        try {
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            return this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Error! Please make sure the file is a yml file, and is configured properly");
        }

        boolean noContext = false;
        if (config.isString("context")) {
            String contextF = config.getString("context");
            if (contextF.equalsIgnoreCase("global")) {
                context = Context.CONTEXT_SERVER_GLOBAL;
            } else if (contextF.equalsIgnoreCase("local")) {
                context = Context.CONTEXT_SERVER_LOCAL;
            } else {
                noContext = true;
            }
        } else {
            noContext = true;
        }

        if (noContext) {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&6Warning! &7No context specified in the file (field \"context\"), defaulting to group's server context");
        }
        if (!config.isList("permissions")) {
            return this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&cError! &7There is no yml string list \"permissions\"!");
        }

        ArrayList<PPermission> permsToAdd = new ArrayList<>();
        for (String perm : config.getStringList("permissions")) {
            permsToAdd.add(new PPermission(perm, context));
        }

        if (permsToAdd.size() == 0) {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&6Warning! &7Permissions list is empty, adding &e" + permsToAdd.size() + "&7 permissions!");
        }

        boolean remove = config.isBoolean("remove") && config.getBoolean("remove");

        if (remove) {
            group.removeOwnPermissionsStr(Lists.newArrayList(config.getStringList("permissions")));
        } else {
            group.addOwnPermissions(permsToAdd);
        }

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&e" + permsToAdd.size() + " &7permissions have been " + (remove ? "&cremoved&7" : "&aadded&7") + " to the group!");
        return true;
    }
}

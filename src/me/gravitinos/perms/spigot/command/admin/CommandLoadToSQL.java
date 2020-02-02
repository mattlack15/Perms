package me.gravitinos.perms.spigot.command.admin;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ExecutionException;

public class CommandLoadToSQL extends GravSubCommand {

    public CommandLoadToSQL(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".admin";
    }

    @Override
    public String getDescription() {
        return "Saves all loaded groups and online player's data into SQL";
    }

    @Override
    public String getAlias() {
        return "loadtosql";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if (!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")) {
            return true;
        }
        SpigotPerms.instance.reloadConfig();

        if (PermsManager.instance.getImplementation().getConfigSettings().isUsingSQL()) {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Please switch the SQL setting in config to false, then try again");
            return true;
        }

        SQLHandler handler = new SQLHandler();
        if (!PermsManager.instance.connectSQL(handler, SpigotPerms.instance.getImpl().getConfigSettings())) {
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Please make sure that the credentials and host specified in SQL settings are correct");
            return true;
        }

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                PermsManager.instance.copyTo(handler).get();
                this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&aFinished &7Data Operation &aSuccessfully!");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&cError occurred while completing operation!");
            }
        });

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Working..");
        return true;
    }
}

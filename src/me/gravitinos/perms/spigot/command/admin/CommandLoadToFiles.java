package me.gravitinos.perms.spigot.command.admin;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.GravCommandPermissionable;
import me.gravitinos.perms.spigot.command.GravSubCommand;
import me.gravitinos.perms.spigot.file.SpigotFileDataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ExecutionException;

public class CommandLoadToFiles extends GravSubCommand {
    public CommandLoadToFiles(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".admin";
    }

    @Override
    public String getDescription() {
        return "Saves all loaded groups and online player's userdata to files";
    }

    @Override
    public String getAlias() {
        return "loadtofiles";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if (!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")) {
            return true;
        }
        if (PermsManager.instance.getImplementation().getConfigSettings().isUsingSQL()) {
            this.sendErrorMessage(sender, SpigotPerms.instance + "Please switch the SQL setting in config to true, then try again");
            return true;
        }

        SpigotFileDataManager handler = new SpigotFileDataManager();

        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                PermsManager.instance.copyTo(handler).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&aFinished &7Data Operation &aSuccessfully!");
        });

        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Working..");
        return true;
    }
}

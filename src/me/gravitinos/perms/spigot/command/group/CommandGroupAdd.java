package me.gravitinos.perms.spigot.command.group;

import me.gravitinos.perms.spigot.command.GravSubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandGroupAdd extends GravSubCommand {
    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getAlias() {
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        return false;
    }
}

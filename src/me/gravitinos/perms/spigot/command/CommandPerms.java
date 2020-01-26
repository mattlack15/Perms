package me.gravitinos.perms.spigot.command;

import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class CommandPerms extends GravCommand {
    @Override
    public ArrayList<String> getHelpMessage() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Main permissions command";
    }

    @Override
    public ArrayList<String> getAliases() {
        return new ArrayList<String>(){{add(SpigotPerms.commandName);}};
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".use";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        
    }
}

package me.gravitinos.perms.spigot.command;

import me.gravitinos.perms.spigot.gui.MenuMain;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandGUI extends GravSubCommand {
    public CommandGUI(GravCommandPermissionable parentCommand, String cmdPath) {
        super(parentCommand, cmdPath);
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Opens the Permissions GUI";
    }

    @Override
    public String getAlias() {
        return "gui";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        if(!(sender instanceof Player)){
            this.sendErrorMessage(sender, "You must be a player to execute this command!");
            return true;
        }
        Player player = (Player) sender;
        player.playSound(player.getLocation(), MenuMain.OPEN_USER_LIST_SOUND, 1f, 1f);
        new MenuMain(player).open(player);
        return true;
    }
}

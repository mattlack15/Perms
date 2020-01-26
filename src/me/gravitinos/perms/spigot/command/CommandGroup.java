package me.gravitinos.perms.spigot.command;

import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class CommandGroup extends GravSubCommand {
    public CommandGroup(String cmdPath) {
        super(cmdPath);
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".use";
    }

    @Override
    public String getDescription() {
        return "Gateway command to group info and group management commands";
    }

    @Override
    public String getAlias() {
        return "group";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        //Don't need to check permissions because the permission is the same as the parent command (CommandPerms)

        if (args.length > 0 && !args[0].equalsIgnoreCase("help")) {
            GravSubCommand subCommand = this.getSubCommand(args[0]);
            if (subCommand == null) {
                this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Unrecognized sub-command! Try &6/" + SpigotPerms.commandName + " help");
                return true;
            }
            this.callSubCommand(subCommand, sender, cmd, label, args);
        }

        //Literally the exact same functionality as CommandPerms
        if (args.length > 1 && !args[0].equalsIgnoreCase("help")) { //TODO add object args to callSubCommand for passing group object through or user object use Object... passedArgs
            GravSubCommand subCommand = this.getSubCommand(args[1]);
            if (subCommand == null) {
                this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Unrecognized sub-command! Try &6/" + SpigotPerms.commandName + " help");
                return true;
            }
            this.callSubCommand(subCommand, sender, cmd, label, args);
        } else {
            int page = 1;
            try {
                page = Integer.parseInt(args[1]);
            } catch (Exception ignored) { }
            ArrayList<String> helpMsgs = this.getHelpMessages(SpigotPerms.instance.getImpl().getConfigSettings().getHelpFormat(), page);
            helpMsgs.add(0, ChatColor.translateAlternateColorCodes('&', SpigotPerms.instance.getImpl().getConfigSettings().getHelpHeader().replace("<page>", page + "")));
            helpMsgs.add(ChatColor.translateAlternateColorCodes('&', SpigotPerms.instance.getImpl().getConfigSettings().getHelpFooter().replace("<page>", page + "")));

            helpMsgs.forEach(sender::sendMessage); //Send the messages
        }
        return true;

    }
}

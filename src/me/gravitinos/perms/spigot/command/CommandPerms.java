package me.gravitinos.perms.spigot.command;

import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class CommandPerms extends GravCommand {

    public CommandPerms(){
        this.addSubCommand(new CommandGroup(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandUser(this, this.getSubCommandCmdPath()));
    }

    @Override
    public String getDescription() {
        return "Main permissions command";
    }

    @Override
    public ArrayList<String> getAliases() {
        return new ArrayList<String>() {{
            add(SpigotPerms.commandName);
        }};
    }

    @Override
    public String getPermission() {
        return SpigotPerms.commandName + ".use";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!this.checkPermission(sender, SpigotPerms.pluginPrefix + "You do not have permission to use this command!")) {
            return true;
        }

        if (args.length > 0 && !args[0].equalsIgnoreCase("help")) {
            GravSubCommand subCommand = this.getSubCommand(args[0]);
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

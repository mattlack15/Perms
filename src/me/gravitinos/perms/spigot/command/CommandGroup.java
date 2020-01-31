package me.gravitinos.perms.spigot.command;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.group.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class CommandGroup extends GravSubCommand {
    public CommandGroup(GravCommandPermissionable parent, String cmdPath) {
        super(parent, cmdPath);

        this.addSubCommand(new CommandGroupAdd(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandGroupParents(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandGroupRemove(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandGroupSetDesc(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandGroupSetPrefix(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandGroupSetSuffix(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandGroupSetPriority(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandGroupCreate(this, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandGroupDelete(this, this.getSubCommandCmdPath()));
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object...passedArgs) {
        //Don't need to check permissions because the permission is the same as the parent command (CommandPerms)

        //Literally the exact same functionality as CommandPerms
        if (args.length > 1) {
            GravSubCommand subCommand = this.getSubCommand(args[1]);
            if (subCommand == null) {
                this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Unrecognized sub-command! Try &6/" + SpigotPerms.commandName + " help");
                return true;
            }

            Group group = GroupManager.instance.getGroup(args[0]);
            if(group == null){
                if(subCommand.getAlias().equalsIgnoreCase("create")){
                    this.callSubCommand(subCommand, 1, sender, cmd, label, args, args[0]);
                } else {
                    this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group not found!");
                }
                return true;
            }
            this.callSubCommand(subCommand, 1, sender, cmd, label, args, group);
        } else {

            if(args.length < 1){
                this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed!");
                return true;
            }

            //Display group info

            Group group = GroupManager.instance.getGroup(args[0]);
            if(group == null){
                this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Group not found!");
                return true;
            }

            //Not being used for error though
            sendErrorMessage(sender, "&3&lName &6> &7" + group.getName());
            sendErrorMessage(sender, "&3&lPrefix &6> &r" + group.getPrefix());
            sendErrorMessage(sender, "&3&lSuffix &6> &r" + group.getSuffix());
            sendErrorMessage(sender, "&3&lServer &6> &7" + group.getServerContext() + " (&e" + (GroupData.SERVER_GLOBAL.equals(group.getServerContext()) ? "Global&7)" : (GroupData.SERVER_LOCAL.equals(group.getServerContext()) ? "Local&7)" : "Foreign&7)")));
            sendErrorMessage(sender, "&3&lChat Colour &6> &r" + group.getChatColour() + "People in this group have this chat colour");
            sendErrorMessage(sender, "&3&lDescription &6> &r" + group.getDescription());
            sendErrorMessage(sender, "&3&lDefault Group &6> " + (group.equals(GroupManager.instance.getDefaultGroup()) ? "&aTrue" : "&cFalse"));
            sendErrorMessage(sender, "&3&lInheritances &6>");
            for(Inheritance inheritance : group.getInheritances()){
                boolean applies = inheritance.getContext().getServerName().equals(GroupData.SERVER_GLOBAL) || inheritance.getContext().getServerName().equals(GroupData.SERVER_LOCAL);
                sendErrorMessage(sender, "&7- &e" + inheritance.getParent() + (applies ? "" : "&cDoes not apply here"));
            }

        }
        return true;

    }
}

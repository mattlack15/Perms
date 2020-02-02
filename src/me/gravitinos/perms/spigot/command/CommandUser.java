package me.gravitinos.perms.spigot.command;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.command.user.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class CommandUser extends GravSubCommand {
    public CommandUser(GravCommandPermissionable parent, String cmdPath) {
        super(parent, cmdPath);
        this.addSubCommand(new CommandUserGroup(parent, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandUserAdd(parent, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandUserRemove(parent, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandUserSetPrefix(parent, this.getSubCommandCmdPath()));
        this.addSubCommand(new CommandUserList(parent, this.getSubCommandCmdPath()));
    }

    @Override
    public String getPermission() {
        return this.getParentCommand().getPermission();
    }

    @Override
    public String getDescription() {
        return "Gateway command to user info and user management commands";
    }

    @Override
    public String getAlias() {
        return "user";
    }

    @Override
    public String getArgumentString(){
        return "<user>";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args, Object... passedArgs) {
        //Don't need to check permissions because the permission is the same as the parent command (CommandPerms)

        //Literally the exact same functionality as CommandPerms
        SpigotPerms.instance.getImpl().getAsyncExecutor().execute(() -> {
            if (args.length > 1) {
                GravSubCommand subCommand = this.getSubCommand(args[1]);
                if (subCommand == null) {
                    this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "Unrecognized sub-command! Try &6/" + SpigotPerms.commandName + " help");
                    return;
                }

                //Get user
                Player p = Bukkit.getPlayer(args[0]);
                User user;
                if (p == null) {
                    this.sendErrorMessage(sender, "User is not online, attempting to temporarily load userdata");
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                    if (offlinePlayer == null) {
                        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "User not found!");
                        return;
                    }
                    try {
                        UserManager.instance.loadUser(offlinePlayer.getUniqueId(), offlinePlayer.getName()).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    user = UserManager.instance.getUser(offlinePlayer.getUniqueId());
                } else {
                    user = UserManager.instance.getUser(p.getUniqueId());
                }

                if (user == null) {
                    this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "User not found!");
                    return;
                }

                this.callSubCommand(subCommand, 1, sender, cmd, label, args, user);
            } else {

                if(args.length < 1){
                    this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "More arguments needed!");
                    return;
                }

                //Display user info

                Player p = Bukkit.getPlayer(args[0]);
                User user;
                if (p == null) {
                    this.sendErrorMessage(sender, "User is not online, attempting to temporarily load userdata");
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                    if (offlinePlayer == null) {
                        this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "User not found!");
                        return;
                    }
                    try {
                        UserManager.instance.loadUser(offlinePlayer.getUniqueId(), offlinePlayer.getName()).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    user = UserManager.instance.getUser(offlinePlayer.getUniqueId());
                } else {
                    user = UserManager.instance.getUser(p.getUniqueId());
                }

                if (user == null) {
                    this.sendErrorMessage(sender, SpigotPerms.pluginPrefix + "User not found!");
                    return;
                }

                //Not being used for error though
                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&6&lUser Data > &b" + user.getName());

                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&fName &6> &b" + user.getName());
                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&fUUID &6> &7" + user.getUniqueID());
                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&fPrefix &6> " + user.getPrefix());
                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&fSuffix &6> " + user.getSuffix());
                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&fDisplay Group &6> &7" + user.getDisplayGroup());

                sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&fInheritances (Groups) &6>");
                for (Inheritance inheritance : user.getInheritances()) {
                    boolean applies = inheritance.getContext().getServerName().equals(GroupData.SERVER_GLOBAL) || inheritance.getContext().getServerName().equals(GroupData.SERVER_LOCAL);
                    sendErrorMessage(sender, SpigotPerms.pluginPrefix + "&7- &e" + PermsManager.removeServerFromIdentifier(inheritance.getParent().getIdentifier()) + (applies ? "" : " &cDoes not apply here &7(&f" + inheritance.getContext().getServerName() + "&7)"));
                }

            }
        });
        return true;
    }
}

package me.gravitinos.perms.bungee;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.UserManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CommandReloadGroups extends Command {

    protected CommandReloadGroups() {
        super("ppbung");
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if(!commandSender.hasPermission("ranks.admin")){
            commandSender.sendMessage(ChatColor.RED + "You do not have permission to use that");
            return;
        }
        if(args.length < 1){
            commandSender.sendMessage("Must have argument, such as reloadgroups");
            return;
        }

        if(args[0].equalsIgnoreCase("reloadgroups")){
            CompletableFuture<Boolean> future = GroupManager.instance.reloadGroups();
            commandSender.sendMessage("Reloading groups...");
            PermsManager.instance.reloadGodUsers();
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                try {
                    future.get();
                    commandSender.sendMessage("Successfully reloaded all groups!");
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    commandSender.sendMessage("Error -> failed to reload groups!");
                }
            });
            return;
        }
        commandSender.sendMessage("Unrecognized subcommand!");
    }
}

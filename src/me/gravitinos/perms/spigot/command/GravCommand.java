package me.gravitinos.perms.spigot.command;
import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public abstract class GravCommand implements CommandExecutor {
	private ArrayList<GravSubCommand> subCommands = new ArrayList<GravSubCommand>();
	public GravCommand() {
		for(String a : this.getAliases()) {
			SpigotPerms.instance.getCommand(a).setExecutor(this);
		}
	}
	public void addSubCommand(GravSubCommand cmd) {
		this.subCommands.add(cmd);
	}
	public boolean callSubCommand(GravSubCommand cmd, CommandSender sender, Command cmd1, String label, String args[]) {
		String args1[] = new String[args.length-1];
		for(int i = 1; i < args.length; i++) {
			args1[i-1] = args[i];
		}
		return cmd.onCommand(sender, cmd1, label, args1);
	}

	public abstract ArrayList<String> getHelpMessage();

    protected boolean sendErrorMessage(CommandSender sender, String msg){
        sender.sendMessage(msg);
        return true;
    }

	public boolean isAlias(String alias) {
		for(String a : this.getAliases()) {
			if(a.equalsIgnoreCase(alias)) {
				return true;
			}
		}
		return false;
	}
	public boolean checkPermission(CommandSender sender, String noPermissionMessage){
		if(!sender.hasPermission(this.getPermission())){
			sender.sendMessage(noPermissionMessage);
			return false;
		}
		return true;
	}
	public GravSubCommand getSubCommand(String alias) {
		for(GravSubCommand cmds : this.subCommands) {
			if(cmds.getAlias().equalsIgnoreCase(alias)) {
				return cmds;
			}
		}
		return null;
	}
	public ArrayList<GravSubCommand> getSubCommands(){
		return this.subCommands;
	}
	public abstract String getDescription();
	public abstract ArrayList<String> getAliases();
	public abstract String getPermission();
}

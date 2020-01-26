package me.gravitinos.perms.spigot.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public abstract class GravSubCommand {
	private ArrayList<GravSubCommand> subCommands = new ArrayList<>();
	public abstract String getPermission();
	public abstract String getDescription();
	public abstract String getAlias();
	public abstract boolean onCommand(CommandSender sender, Command cmd, String label, String args[]);
	public boolean callSubCommand(GravSubCommand cmd, CommandSender sender, Command cmd1, String label, String args[]) {
		String args1[] = new String[args.length-1];
		for(int i = 1; i < args.length; i++) {
			args1[i-1] = args[i];
		}
		return cmd.onCommand(sender, cmd1, label, args1);
	}
	public GravSubCommand getSubCommand(String alias) {
		for(GravSubCommand cmds : this.subCommands) {
			if(cmds.getAlias().equalsIgnoreCase(alias)) {
				return cmds;
			}
		}
		return null;
	}

	protected boolean sendErrorMessage(CommandSender sender, String msg){
	    sender.sendMessage(msg);
	    return true;
    }

    public abstract ArrayList<String> getHelpMessage();

    public boolean checkPermission(CommandSender sender, String noPermissionMessage){
    	if(this.getPermission() == null) {
    		return true;
		}
		if(!sender.hasPermission(this.getPermission())){
			sender.sendMessage(noPermissionMessage);
			return false;
		}
		return true;
	}
	public void addSubCommand(GravSubCommand cmd) {
		this.subCommands.add(cmd);
	}
	public ArrayList<GravSubCommand> getSubCommands(){
		return this.subCommands;
	}
}

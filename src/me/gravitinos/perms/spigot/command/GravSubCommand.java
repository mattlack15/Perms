package me.gravitinos.perms.spigot.command;

import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public abstract class GravSubCommand {
	private ArrayList<GravSubCommand> subCommands = new ArrayList<>();
	public abstract String getPermission();
	public abstract String getDescription();
	public abstract String getAlias();

	private String cmdPath;

	public GravSubCommand(String cmdPath){
		this.cmdPath = cmdPath;
	}

	public abstract boolean onCommand(CommandSender sender, Command cmd, String label, String args[]);

	public boolean callSubCommand(GravSubCommand cmd, CommandSender sender, Command cmd1, String label, String[] args){
		return this.callSubCommand(cmd, 0, sender, cmd1, label, args);
	}
	public boolean callSubCommand(GravSubCommand cmd, int usedArgs, CommandSender sender, Command cmd1, String label, String[] args) {
		String args1[] = new String[args.length-1];
		for(int i = usedArgs+1; i < args.length; i++) {
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

    protected void setCmdPath(String path){
		this.cmdPath = path;
	}

	protected String getCmdPath(){
		return this.cmdPath;
	}

	protected String getSubCommandCmdPath(){
		return this.cmdPath + this.getAlias() + " ";
	}

	/**
	 * Gets help messages from a format with placeholders <cmd_name> <cmd_description> and <cmd_permission>
	 * @param format
	 * @param page
	 * @return
	 */
	public ArrayList<String> getHelpMessages(String format, int page){
		String helpFormat = SpigotPerms.instance.getImpl().getConfigSettings().getHelpFormat();
		ArrayList<String> helpMessages = new ArrayList<>();
		for(GravSubCommand subCommand : this.getSubCommands()){
			helpMessages.add(ChatColor.translateAlternateColorCodes('&', helpFormat.replace("<cmd_name>", this.getCmdPath() + subCommand.getAlias())
					.replace("<cmd_description>", subCommand.getDescription()).replace("<cmd_permission>", subCommand.getPermission())));
		}
		return helpMessages;
	}

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

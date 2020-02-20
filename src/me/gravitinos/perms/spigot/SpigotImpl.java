package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.PermsImplementation;
import me.gravitinos.perms.core.config.PermsConfiguration;
import me.gravitinos.perms.spigot.file.SpigotConf;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpigotImpl implements PermsImplementation {
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private SpigotConf conf = new SpigotConf();

    @Override
    public File getDataFolder() {
        return SpigotPerms.instance.getDataFolder();
    }

    @Override
    public Executor getAsyncExecutor() {
        return executorService;
    }

    @Override
    public Executor getSyncExecutor() {
        return command -> Bukkit.getScheduler().runTask(SpigotPerms.instance, command);
    }

    @Override
    public PermsConfiguration getConfigSettings() {
        return conf;
    }

    @Override
    public void runTaskTimerAsync(Runnable runnable, int delay, int repeat) {

    }

    @Override
    public void runTaskTimerSync(Runnable runnable, int delay, int repeat) {

    }

    @Override
    public String getStringSetting(String path) {
        return null;
    }

    @Override
    public boolean getBooleanSetting(String path) {
        return false;
    }

    @Override
    public ArrayList<String> getStringListSetting(String path) {
        return null;
    }

    @Override
    public int getIntSetting(String path) {
        return 0;
    }

    @Override
    public String getLog(int line) {
        return null;
    }

    @Override
    public void addToLog(String message) {
        for(Player p : Bukkit.getOnlinePlayers()){
            if(p.hasPermission(SpigotPerms.commandName + ".viewlog")){
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', SpigotPerms.pluginPrefix + " &c&lLOG: &f" + message));
            }
        }
    }

    @Override
    public void consoleLog(String message) {
        Bukkit.getConsoleSender().sendMessage("Perms: " + message);
    }
}

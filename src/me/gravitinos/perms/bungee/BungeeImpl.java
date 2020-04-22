package me.gravitinos.perms.bungee;

import me.gravitinos.perms.core.PermsImplementation;
import me.gravitinos.perms.core.config.PermsConfiguration;
import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BungeeImpl implements PermsImplementation {
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private PermsConfiguration settings = new BungeeConfigSettings();

    @Override
    public File getDataFolder() {
        return BungeePerms.instance.getDataFolder();
    }

    @Override
    public Executor getAsyncExecutor() {
        return executorService;
    }

    @Override
    public Executor getSyncExecutor() {
        return command -> BungeePerms.instance.getProxy().getScheduler().schedule(BungeePerms.instance, command,0, TimeUnit.MILLISECONDS);
    }

    @Override
    public PermsConfiguration getConfigSettings() {
        return settings;
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

    }

    @Override
    public void consoleLog(String message) {
        BungeePerms.instance.getLogger().info(message);
    }

    @Override
    public void sendDebugMessage(String message) {

    }
}

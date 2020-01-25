package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.PermsImplementation;
import me.gravitinos.perms.core.config.PermsConfiguration;
import me.gravitinos.perms.spigot.file.SpigotConf;
import org.bukkit.Bukkit;

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
    public String addToLog(String message) {
        return null;
    }
}

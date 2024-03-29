package me.gravitinos.perms.core;

import me.gravitinos.perms.core.config.PermsConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;

public interface PermsImplementation {

    File getDataFolder();
    Executor getAsyncExecutor();
    Executor getSyncExecutor();
    void scheduleRepeatingTaskAsync(Runnable runnable, int delay, int periodTicks);
    PermsConfiguration getConfigSettings();
    void runTaskTimerAsync(Runnable runnable, int delay, int repeat);
    void runTaskTimerSync(Runnable runnable, int delay, int repeat);
    String getStringSetting(String path);
    boolean getBooleanSetting(String path);
    ArrayList<String> getStringListSetting(String path);
    int getIntSetting(String path);
    String getLog(int line);
    void addToLog(String message);
    void consoleLog(String message);
    void sendDebugMessage(String message);
    String getPluginName();

}

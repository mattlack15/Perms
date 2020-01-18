package me.gravitinos.perms.core;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;

public interface PermsImplementation {

    File getDataFolder();
    Executor getAsyncExecutor();
    Executor getSyncExecutor();
    void runTaskTimerAsync(Runnable runnable, int delay, int repeat);
    void runTaskTimerSync(Runnable runnable, int delay, int repeat);
    String getStringSetting(String path);
    boolean getBooleanSetting(String path);
    ArrayList<String> getStringListSetting(String path);
    int getIntSetting(String path);

}

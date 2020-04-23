package me.gravitinos.perms.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.config.PermsConfiguration;
import me.gravitinos.perms.core.converter.Converter;
import me.gravitinos.perms.core.converter.converters.ConverterIdentifierToSubjectId;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.util.ReflectionUtils2;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class PermsManager {
    public static PermsManager instance = null;

    private GroupManager groupManager;
    private UserManager userManager;
    private DataManager dataManager;

    private PermsImplementation implementation;

    private Map<Integer, String> cachedServerIndex = new HashMap<>();

    public PermsManager(PermsImplementation implementation, @NotNull DataManager dataManager){
        instance = this;
        this.implementation = implementation;

        this.dataManager = dataManager;

        this.groupManager = new GroupManager(dataManager);
        this.userManager = new UserManager(dataManager);

        //Config checks
        if(implementation.getConfigSettings().getServerName().equals("")){
            implementation.getConfigSettings().setServerName("server_" + Math.round(Math.random() * 1000));
        }

        if(dataManager instanceof SQLHandler) {
            if (this.connectSQL((SQLHandler) dataManager, implementation.getConfigSettings())) {
                this.implementation.consoleLog(ChatColor.GREEN + "Connected to SQL successfully!");
            } else {
                return;
            }
        }

        //Converters (things that change formats from old versions to new formats of newer versions)
        //Please put them in order of oldest version to newest version
        runConverters(new ConverterIdentifierToSubjectId());

        try {
            this.groupManager.loadGroups().get(); //Load groups
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void runConverters(Converter... converters) {
        for(Converter converter : converters){
            if(converter.test()){
                getImplementation().addToLog("Running converter: " + converter.getName());
                getImplementation().consoleLog(getImplementation().getPluginName() + " Running converter: " + converter.getName());
                boolean success = false;
                try{
                    success = converter.convert();
                } catch (Exception e){
                    e.printStackTrace();
                }
                if(!success){
                    getImplementation().addToLog("Converting failed for: " + converter.getName());
                    getImplementation().consoleLog(getImplementation().getPluginName() + " Converting failed for: " + converter.getName());
                } else {
                    getImplementation().addToLog("Successfully converted");
                    getImplementation().consoleLog(getImplementation().getPluginName() + " Successfully converted");
                }
            }
        }
    }

    public CompletableFuture<Void> copyTo(DataManager manager){
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                manager.clearAllData().get();
                CompletableFuture<Void> future1 = userManager.saveTo(manager);
                CompletableFuture<Void> future2 = groupManager.saveTo(manager);
                future1.get();
                future2.get();
                future.complete(null);
            }catch(Exception e){
                future.complete(null);
                e.printStackTrace();
            }
        });
        return future;
    }

    private DataSource getHikariDataSource(String url, PermsConfiguration configuration) throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName(DriverManager.getDriver(url).getClass().getCanonicalName());
        config.setUsername(configuration.getSQLUsername());
        config.setPassword(configuration.getSQLPassword());
        config.setMaximumPoolSize(Runtime.getRuntime().availableProcessors()*2 + 1);

        return new HikariDataSource(config);
    }

    public boolean connectSQL(SQLHandler dataManager, PermsConfiguration config){
        try {
            dataManager.setDataSource(getHikariDataSource("jdbc:mysql://" + config.getSQLHost() + ":" + config.getSQLPort() + "/" + config.getSQLDatabase() + "?rewriteBatchedStatements=true", config));
            if(dataManager.getConnection() == null){
                this.implementation.addToLog(ChatColor.RED + "Unable to connect to SQL!");
                this.implementation.consoleLog(ChatColor.RED + "Unable to connect to SQL!");
                this.implementation.sendDebugMessage(ChatColor.RED + "Unable to connect to SQL!");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            this.implementation.addToLog(ChatColor.RED + "Unable to connect to SQL!");
            this.implementation.consoleLog(ChatColor.RED + "Unable to connect to SQL!");
            this.implementation.sendDebugMessage(ChatColor.RED + "Unable to connect to SQL!");
            return false;
        }
        try {
            dataManager.setup().get();
            cachedServerIndex = dataManager.getServerIndex().get();
            if(cachedServerIndex.containsKey(config.getServerId())){
                if(!cachedServerIndex.get(config.getServerId()).equals(config.getServerName())){
                    removeServerFromIndex(config.getServerId());
                    dataManager.putServerIndex(config.getServerId(), config.getServerName()).get();
                    cachedServerIndex = dataManager.getServerIndex().get();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            this.implementation.addToLog(ChatColor.RED + "Unable to setup SQL Database!");
            this.implementation.consoleLog(ChatColor.RED + "Unable to setup SQL Database!");
            this.implementation.sendDebugMessage(ChatColor.RED + "Unable to setup SQL Database!");
            return false;
        }
        return true;
    }


//    public static String addServerToIdentifier(String identifier, String server){
//        return server + SERVER_NAME_SEPERATOR + identifier;
//    }

    /**
     * Gets the server name from a server id
     * @param serverId Usually a number between 0 and 9999999, except for global which is usually blank
     * @return The server name
     */
    public String getServerName(String serverId){
        try {
            return this.cachedServerIndex.get(Integer.parseInt(serverId));
        } catch (Exception e){
            if(serverId.equals(GroupData.SERVER_GLOBAL)){
                return "";
            } else {
                return null;
            }
        }
    }

    public void removeServerFromIndex(int id){
        this.cachedServerIndex.remove(id);
        dataManager.removeServerIndex(id);
    }

    /**
     * Get server Id from Server name
     */
    public int getServerId(String serverName){
        AtomicInteger id = new AtomicInteger(-1);
        this.cachedServerIndex.forEach((m, p) -> {
            if(p.equals(serverName)){
                id.set(m);
            }
        });
        return id.get();
    }

    public Map<Integer, String> getCachedServerIndex(){
        return this.cachedServerIndex;
    }

    public PermsImplementation getImplementation() {
        return implementation;
    }
}

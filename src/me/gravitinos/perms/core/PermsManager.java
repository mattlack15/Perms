package me.gravitinos.perms.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.config.PermsConfiguration;
import me.gravitinos.perms.core.converter.Converter;
import me.gravitinos.perms.core.converter.converters.ConverterContext;
import me.gravitinos.perms.core.converter.converters.ConverterIdentifierToSubjectId;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.ladders.LadderManager;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.core.util.GravSerializable;
import me.gravitinos.perms.core.util.GravSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PermsManager {
    public static PermsManager instance = null;

    private GroupManager groupManager;
    private UserManager userManager;
    @Getter
    private LadderManager ladderManager;
    private DataManager dataManager;

    private PermsImplementation implementation;

    private Map<Integer, String> cachedServerIndex = new ConcurrentHashMap<>();

    public PermsManager(PermsImplementation implementation, @NotNull DataManager dataManager) {
        instance = this;
        this.implementation = implementation;

        this.dataManager = dataManager;

        //Config checks
        if (implementation.getConfigSettings().getServerName().equals("")) {
            implementation.getConfigSettings().setServerName("server_" + Math.round(Math.random() * 1000));
        }

        if (dataManager instanceof SQLHandler) {
            if (this.connectSQL((SQLHandler) dataManager, implementation.getConfigSettings())) {
                this.implementation.consoleLog(ChatColor.GREEN + "Connected to SQL successfully!");
            } else {
                return;
            }
        }

        this.groupManager = new GroupManager(dataManager);
        this.userManager = new UserManager(dataManager);
        this.ladderManager = new LadderManager(dataManager);

        try {
            if (!dataManager.testBackendConnection().get()) {
                this.implementation.consoleLog(ChatColor.RED + "UNABLE TO CONNECT TO BACKEND");
                return;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //Converters (things that change formats from old versions to new formats of newer versions)
        //Please put them in order of oldest version to newest version
        runConverters(new ConverterIdentifierToSubjectId(), new ConverterContext());

        try {
            cachedServerIndex = dataManager.getServerIndex().get();
            if (!cachedServerIndex.containsKey(implementation.getConfigSettings().getServerId()) || !cachedServerIndex.get(implementation.getConfigSettings().getServerId()).equals(implementation.getConfigSettings().getServerName())) {
                removeServerFromIndex(implementation.getConfigSettings().getServerId()).get();
                dataManager.putServerIndex(implementation.getConfigSettings().getServerId(), implementation.getConfigSettings().getServerName()).get();
                cachedServerIndex = dataManager.getServerIndex().get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Loading groups");
            this.groupManager.loadGroups().get(2, TimeUnit.SECONDS); //Load groups
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Loading ladders");
            this.ladderManager.loadLadders().get(2, TimeUnit.SECONDS); //Load rank ladders
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        getImplementation().scheduleRepeatingTaskAsync(dataManager::flushUpdateQueue, 10, 10);
    }

    public void shutdown() {
        if (this.dataManager != null)
            this.dataManager.shutdown();
    }

    public GravSerializer createGroupBackup() {
        GravSerializer serializer = new GravSerializer();
        List<Group> groups = GroupManager.instance.getLoadedGroups();
        serializer.writeInt(groups.size());
        groups.forEach(g -> {
            serializer.writeUUID(g.getSubjectId());
            serializer.writeString(g.getData().toString());
            List<PPermission> permissions = g.getPermissions().getPermissions();
            serializer.writeInt(permissions.size());
            for(PPermission permission : permissions) {
                serializer.writeString(permission.getPermission());
                serializer.writeString(permission.getContext().toString());
                serializer.writeUUID(permission.getPermissionIdentifier());
            }
            List<Inheritance> inheritances = g.getInheritances();
            serializer.writeInt(inheritances.size());
            for(Inheritance inheritance : g.getInheritances()) {
                serializer.writeUUID(inheritance.getParent().getSubjectId());
                serializer.writeString(inheritance.getContext().toString());
            }
        });
        return serializer;
    }

    private void runConverters(Converter... converters) {
        for (Converter converter : converters) {
            if (converter.test()) {
                getImplementation().addToLog("Running converter: " + converter.getName());
                getImplementation().consoleLog(getImplementation().getPluginName() + " Running converter: " + converter.getName());
                boolean success = false;
                try {
                    success = converter.convert();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!success) {
                    getImplementation().addToLog("Converting failed for: " + converter.getName());
                    getImplementation().consoleLog(getImplementation().getPluginName() + " Converting failed for: " + converter.getName());
                } else {
                    getImplementation().addToLog("Successfully converted");
                    getImplementation().consoleLog(getImplementation().getPluginName() + " Successfully converted");
                }
            }
        }
    }

    public CompletableFuture<Void> copyTo(DataManager manager) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                manager.clearAllData().get();
                CompletableFuture<Void> future1 = userManager.saveTo(manager);
                CompletableFuture<Void> future2 = groupManager.saveTo(manager);
                future1.get();
                future2.get();
                future.complete(null);
            } catch (Exception e) {
                future.complete(null);
                e.printStackTrace();
            }
        });
        return future;
    }

    private DataSource getHikariDataSource(String datasourceClass, PermsConfiguration configuration) throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName(datasourceClass);
        config.addDataSourceProperty("serverName", configuration.getSQLHost());
        config.addDataSourceProperty("port", configuration.getSQLPort());
        config.addDataSourceProperty("databaseName", configuration.getSQLDatabase());
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.setLeakDetectionThreshold(20 * 1000);
        config.setUsername(configuration.getSQLUsername());
        config.setPassword(configuration.getSQLPassword());
        config.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 2 + 1);

        return new HikariDataSource(config);
    }

    public boolean connectSQL(SQLHandler dataManager, PermsConfiguration config) {
        try {
            this.getImplementation().consoleLog("Database type: " + config.getDatabaseType());
            dataManager.setDataSource(getHikariDataSource(config.getDatabaseType().equalsIgnoreCase("mysql") ? "com.mysql.jdbc.jdbc2.optional.MysqlDataSource" : "org.mariadb.jdbc.MariaDbDataSource", config));
            if (dataManager.getConnection() == null) {
                this.implementation.addToLog(ChatColor.RED + "Unable to connect to SQL!");
                this.implementation.consoleLog(ChatColor.RED + "Unable to connect to SQL!");
                this.implementation.sendDebugMessage(ChatColor.RED + "Unable to connect to SQL!");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.implementation.addToLog(ChatColor.RED + "Unable to connect to SQL!");
            this.implementation.consoleLog(ChatColor.RED + "Unable to connect to SQL!");
            this.implementation.sendDebugMessage(ChatColor.RED + "Unable to connect to SQL!");
            return false;
        }

        try {
            DatabaseMetaData meta = dataManager.getConnection().getMetaData();
//            implementation.sendDebugMessage("Max user connections: &e" + (meta.getMaxConnections() != 0 ? meta.getMaxConnections() : "Unlimited"));
//            implementation.consoleLog("Max user connections: &e" + (meta.getMaxConnections() != 0 ? meta.getMaxConnections() : "Unlimited"));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            dataManager.setup().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return true;
    }


//    public static String addServerToIdentifier(String identifier, String server){
//        return server + SERVER_NAME_SEPERATOR + identifier;
//    }

    /**
     * Gets the server name from a server id
     *
     * @param serverId Usually a number between 0 and 9999999, except for global which is -1
     * @return The server name
     */
    public String getServerName(int serverId) {
        try {
            return this.cachedServerIndex.get(serverId);
        } catch (Exception e) {
            return null;
        }
    }

    public CompletableFuture<Void> removeServerFromIndex(int id) {
        this.cachedServerIndex.remove(id);
        return dataManager.removeServerIndex(id);
    }

    /**
     * Get server Id from Server name
     */
    public int getServerId(String serverName) {
        AtomicInteger id = new AtomicInteger(-1);
        this.cachedServerIndex.forEach((m, p) -> {
            if (p.equals(serverName)) {
                id.set(m);
            }
        });
        return id.get();
    }

    private List<String> cachedGodUsers = new ArrayList<>();

    public void reloadGodUsers(){
        cachedGodUsers = getImplementation().getConfigSettings().getGodUsers();
    }

    public List<String> getGodUsers(){
        return cachedGodUsers;
    }

    public UserManager getUserManager() {
        return this.userManager;
    }

    public GroupManager getGroupManager() {
        return this.groupManager;
    }

    public Map<Integer, String> getCachedServerIndex() {
        return this.cachedServerIndex;
    }

    public PermsImplementation getImplementation() {
        return implementation;
    }
}

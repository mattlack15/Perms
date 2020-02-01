package me.gravitinos.perms.core;

import me.gravitinos.perms.core.backend.DataManager;
import me.gravitinos.perms.core.backend.sql.SQLHandler;
import me.gravitinos.perms.core.config.PermsConfiguration;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.UserManager;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PermsManager {
    public static PermsManager instance = null;

    private GroupManager groupManager;
    private UserManager userManager;
    private DataManager dataManager;

    private PermsImplementation implementation;

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

        if(this.connectSQL((SQLHandler) dataManager, implementation.getConfigSettings())){
            this.implementation.consoleLog(ChatColor.GREEN + "Connected to SQL successfully!");
        }

        try {
            this.groupManager.loadGroups().get(); //Load groups
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Void> copyTo(DataManager manager){
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.getImplementation().getAsyncExecutor().execute(() -> {
            try {
                manager.clearAllData().get();
                userManager.saveTo(manager).get();
                groupManager.saveTo(manager).get();
                future.complete(null);
            }catch(Exception e){
                future.complete(null);
                e.printStackTrace();
            }
        });
        return future;
    }

    public boolean connectSQL(SQLHandler dataManager, PermsConfiguration config){
        try {
            if (!dataManager.startConnection("jdbc:mysql://" + config.getSQLHost() + ":" + config.getSQLPort() + "/" + config.getSQLDatabase() + "?testWhileIdle=true?rewriteBatchedStatements=true", config.getSQLUsername(), config.getSQLPassword())) {
                this.implementation.addToLog(ChatColor.RED + "Unable to connect to SQL!");
                this.implementation.consoleLog(ChatColor.RED + "Unable to connect to SQL!");
                return false;
            }
        } catch (SQLException e){
            e.printStackTrace();
            this.implementation.addToLog(ChatColor.RED + "Unable to connect to SQL!");
            this.implementation.consoleLog(ChatColor.RED + "Unable to connect to SQL!");
            return false;
        }
        try {
            dataManager.setup().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            this.implementation.addToLog(ChatColor.RED + "Unable to setup SQL Database!");
            this.implementation.consoleLog(ChatColor.RED + "Unable to setup SQL Database!");
            return false;
        }
        return true;
    }

    public PermsImplementation getImplementation() {
        return implementation;
    }
}

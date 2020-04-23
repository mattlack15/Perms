package me.gravitinos.perms.spigot.messaging.listeners;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.channel.MessageCallback;
import org.bukkit.entity.Player;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ListenerReloadAll implements MessageCallback {
    @Override
    public String getSubChannel() {
        return "perms::reloadall";
    }

    @Override
    public boolean test(Player receiver, DataInputStream input) throws IOException {
        return true;
    }

    @Override
    public boolean accept(Player receiver, DataInputStream input) throws IOException {
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            synchronized (ListenerReloadAll.class) {
                SpigotPerms.instance.reloadConfig();
                try {
                    GroupManager.instance.reloadGroups().get();
                    UserManager.instance.reloadUsers().get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        return false;
    }
}

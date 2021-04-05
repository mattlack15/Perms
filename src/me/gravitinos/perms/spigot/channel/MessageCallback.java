package me.gravitinos.perms.spigot.channel;

import org.bukkit.entity.Player;

import java.io.DataInputStream;
import java.io.IOException;

public interface MessageCallback {
    /**
     * Get the sub channel to listen through
     */
    String getSubChannel();
    boolean test(Player receiver, DataInputStream input) throws IOException;

    /**
     * Accepts the payload
     * @return Whether or not to remove this listener from the list of listeners
     */
    boolean accept(Player receiver, DataInputStream input) throws IOException;
}

package me.gravitinos.perms.spigot.messaging;

import me.gravitinos.perms.spigot.channel.MessageAgent;
import org.bukkit.entity.Player;

import java.io.DataOutputStream;
import java.io.IOException;

public class MessageLoadGroup implements MessageAgent {

    //TODO

    @Override
    public String getSubChannel() {
        return null;
    }

    @Override
    public void appendPayload(DataOutputStream outputStream) throws IOException {

    }

    @Override
    public Player getPlayer() {
        return null;
    }
}

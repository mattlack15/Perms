package me.gravitinos.perms.spigot.messaging;

import com.google.common.collect.Lists;
import me.gravitinos.perms.spigot.channel.MessageAgent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.DataOutputStream;
import java.io.IOException;

public class MessageReloadAll implements MessageAgent {

    @Override
    public String getSubChannel() {
        return "perms::reloadall";
    }

    @Override
    public void appendPayload(DataOutputStream outputStream) throws IOException {}

    @Override
    public Player getPlayer() {
        return Bukkit.getOnlinePlayers().size() > 0 ? Lists.newArrayList(Bukkit.getOnlinePlayers()).get(0) : null;
    }

    //Equals to make sure that these aren't spammed in the queue
    @Override
    public boolean equals(Object obj) {
        return obj instanceof MessageReloadAll;
    }
}

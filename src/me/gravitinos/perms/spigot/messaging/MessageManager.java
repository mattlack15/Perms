package me.gravitinos.perms.spigot.messaging;

import me.gravitinos.perms.spigot.channel.MessageAgent;
import me.gravitinos.perms.spigot.channel.ProxyComm;
import org.bukkit.Bukkit;

import java.util.ArrayList;

public class MessageManager {
    public static MessageManager instance;



    private ArrayList<MessageAgent> queue = new ArrayList<>();

    public MessageManager(){
        instance = this;
    }

    /**
     * Queues a message. If a message that has the same purpose is already queued, the message will not be added.
     * @param agent
     */
    public synchronized void queueMessage(MessageAgent agent){
        if(!queue.contains(agent))
            queue.add(agent);
    }

    public synchronized void flushQueue(){
        if(!Bukkit.getOnlinePlayers().isEmpty()) {
            for (MessageAgent agent : queue) {
                ProxyComm.instance.register(agent);
            }
            queue.clear();
        }
    }

}

package me.gravitinos.perms.spigot.messaging.listeners;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.ladders.LadderManager;
import me.gravitinos.perms.core.ladders.RankLadder;
import me.gravitinos.perms.spigot.channel.MessageCallback;
import org.bukkit.entity.Player;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public class ListenerReloadLadder implements MessageCallback {
    @Override
    public String getSubChannel() {
        return "perms::reloadladder";
    }

    @Override
    public boolean test(Player receiver, DataInputStream input) throws IOException {
        return true;
    }

    @Override
    public boolean accept(Player receiver, DataInputStream input) throws IOException {
        //Get the subject id
        String subjectId = input.readUTF();
        UUID id = UUID.fromString(subjectId);

        //Reload the subject, if it is loaded as a group or user
        PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
            if (LadderManager.instance.isLadderLoaded(id)) {
                RankLadder ladder = LadderManager.instance.getLadder(id);
                LadderManager.instance.unloadLadder(id);
                LadderManager.instance.loadLadder(id);
            }
        });

        return false;
    }
}

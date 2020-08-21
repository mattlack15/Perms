package me.gravitinos.perms.spigot.messaging.listeners;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.SubjectRef;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.channel.MessageCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public class ListenerReloadSubject implements MessageCallback {
    @Override
    public String getSubChannel() {
        return "perms::reloadsubject";
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
            if(UserManager.instance.isUserLoaded(id)){
                User user = UserManager.instance.getUser(id);
                String username = user.getName();
                UserManager.instance.loadUser(id, username);
            } else if(GroupManager.instance.isGroupExactLoaded(id)){
                GroupManager.instance.loadGroup(id, (s) -> new SubjectRef(GroupManager.instance.getGroupExact(s)));
            }
        });

        return false;
    }
}

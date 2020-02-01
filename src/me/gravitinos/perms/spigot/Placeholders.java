package me.gravitinos.perms.spigot;

import me.clip.placeholderapi.PlaceholderHook;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.UserManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Placeholders extends PlaceholderHook{
    @Override
    public String onPlaceholderRequest(Player p, String identifier) {

        //%<pluginname>_<identifier>%
        if(identifier.equalsIgnoreCase("prefix")){
            String dg = UserManager.instance.getUser(p.getUniqueId()).getDisplayGroup();
            Group displayGroup = GroupManager.instance.getGroup(dg);
            if(displayGroup == null){
                return "";
            }
            return ChatColor.translateAlternateColorCodes('&', displayGroup.getPrefix());
        } else if(identifier.equalsIgnoreCase("suffix")){
            String dg = UserManager.instance.getUser(p.getUniqueId()).getDisplayGroup();
            Group displayGroup = GroupManager.instance.getGroup(dg);
            if(displayGroup == null){
                return "";
            }
            return ChatColor.translateAlternateColorCodes('&', displayGroup.getSuffix());
        } else if(identifier.equalsIgnoreCase("displaygroup")){
            String dg = UserManager.instance.getUser(p.getUniqueId()).getDisplayGroup();
            Group displayGroup = GroupManager.instance.getGroup(dg);
            if(displayGroup == null){
                return "";
            }
            return displayGroup.getName();
        }
        return null;
    }
}

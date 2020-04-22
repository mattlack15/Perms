package me.gravitinos.perms.spigot;

import me.clip.placeholderapi.PlaceholderHook;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Placeholders extends PlaceholderHook{
    @Override
    public String onPlaceholderRequest(Player p, String identifier) {

        if(p == null || UserManager.instance == null){
            return "<--->";
        }
        User user = UserManager.instance.getUser(p.getUniqueId());
        if(user == null){
            UserManager.instance.loadUser(p.getUniqueId(), p.getName());
            return "<--->";
        }

        //%<pluginname>_<identifier>%
        if(identifier.equalsIgnoreCase("prefix")){
            Group displayGroup = GroupManager.instance.getGroupExact(UserManager.instance.getUser(p.getUniqueId()).getDisplayGroup());
            if(displayGroup == null){
                return "";
            }
            return ChatColor.translateAlternateColorCodes('&', displayGroup.getPrefix());
        } else if(identifier.equalsIgnoreCase("suffix")){
            Group displayGroup = GroupManager.instance.getGroupExact(UserManager.instance.getUser(p.getUniqueId()).getDisplayGroup());
            if(displayGroup == null){
                return "";
            }
            return ChatColor.translateAlternateColorCodes('&', displayGroup.getSuffix());
        } else if(identifier.equalsIgnoreCase("prefix2")){
            Group displayGroup = GroupManager.instance.getGroupExact(UserManager.instance.getUser(p.getUniqueId()).getDisplayGroup());
            if(displayGroup == null){
                return "";
            }
            String pref = ChatColor.translateAlternateColorCodes('&', displayGroup.getPrefix().replace("[", "").replace("]", ""));
            while(pref.length() > 2 && pref.charAt(pref.length()-2) == ChatColor.COLOR_CHAR){
                pref = new StringBuilder(pref).deleteCharAt(pref.length()-1).deleteCharAt(pref.length()-2).toString();
            }
            return pref;
        } else if(identifier.equalsIgnoreCase("suffix2")){
            Group displayGroup = GroupManager.instance.getGroupExact(UserManager.instance.getUser(p.getUniqueId()).getDisplayGroup());
            if(displayGroup == null){
                return "";
            }
            return ChatColor.translateAlternateColorCodes('&', displayGroup.getSuffix().replace("[", "").replace("]", ""));
        } else if(identifier.equalsIgnoreCase("displaygroup")){
            Group displayGroup = GroupManager.instance.getGroupExact(UserManager.instance.getUser(p.getUniqueId()).getDisplayGroup());
            if(displayGroup == null){
                return "";
            }
            return displayGroup.getName();
        }
        return null;
    }
}

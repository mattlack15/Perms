package me.gravitinos.perms.spigot.verbose;

import me.gravitinos.perms.core.util.ComponentUtil;
import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarFile;

public class VerboseController {
    public static VerboseController instance;

    private Map<UUID, String> enabled = new HashMap<>();

    public VerboseController() {
        instance = this;
    }

    public Map<UUID, String> getEnabled(){
        return this.enabled;
    }

    public void handlePermissionCheck(Player player, StackTraceElement stackTrackElement, String permission){
        for(UUID id : enabled.keySet()){
            if(Bukkit.getPlayer(id) == null){
                continue;
            }
            String[] filters = enabled.get(id).split(" ");

            boolean failedFilters = false;
            for(String filter : filters) {
                if(filter.startsWith("player:") && filter.length() > "player:".length()){
                    String playerName = filter.substring("player:".length());
                    if(!player.getName().equalsIgnoreCase(playerName)){
                        failedFilters = true;
                        break;
                    }
                } else if(filter.startsWith("contains:") && filter.length() > "contains:".length()){
                    String contains = filter.substring("contains:".length());
                    if(!permission.contains(contains)){
                        failedFilters = true;
                        break;
                    }
                }
            }
            if(failedFilters)
                continue;

            messagePlayer(Bukkit.getPlayer(id), stackTrackElement, permission);
        }
    }

    public void messagePlayer(Player player, StackTraceElement stackTraceElement, String permission){
        try {
            Class<?> clazz = Class.forName(stackTraceElement.getClassName());

            String pluginName = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
            if(pluginName.contains(".jar") && pluginName.contains(File.separator) && pluginName.lastIndexOf(File.separator) != pluginName.length()-1){
                pluginName = pluginName.substring(pluginName.lastIndexOf(File.separator)+1, pluginName.lastIndexOf(".jar"));
            }
            player.spigot().sendMessage(ComponentUtil.getHoverComponent(SpigotPerms.pluginPrefix + pluginName +
                    ChatColor.WHITE + " checked for " + ChatColor.RED + ChatColor.RED + permission, "&eAt: &7" + stackTraceElement.toString()));
        } catch (ClassNotFoundException ignored) {
        }
    }
}

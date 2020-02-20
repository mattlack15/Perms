package me.gravitinos.perms.spigot.util;

import org.bukkit.ChatColor;

public class UtilColour {
    public static String toColour(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}

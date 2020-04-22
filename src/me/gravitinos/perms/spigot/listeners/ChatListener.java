package me.gravitinos.perms.spigot.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserBuilder;
import me.gravitinos.perms.core.user.UserData;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPermissible;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.file.SpigotConf;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class ChatListener implements Listener {
    public static ChatListener instance;
    private Map<UUID, ArrayList<Consumer<String>>> chatInputHandlers = new HashMap<>();

    public ChatListener(){
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, SpigotPerms.instance);
    }

    public void addChatInputHandler(@NotNull UUID player, @NotNull Consumer<String> consumer){
        chatInputHandlers.computeIfAbsent(player, k -> new ArrayList<>());
        chatInputHandlers.get(player).add(consumer);
    }

    protected void clearChatInputHandler(@NotNull UUID player){
        this.chatInputHandlers.remove(player);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event){

        if(chatInputHandlers.containsKey(event.getPlayer().getUniqueId())){
            event.setCancelled(true);
            chatInputHandlers.get(event.getPlayer().getUniqueId()).forEach(c -> {
                try{
                    c.accept(event.getMessage());
                } catch(Exception e){
                    e.printStackTrace();
                }
            });
            chatInputHandlers.remove(event.getPlayer().getUniqueId());
            return;
        }

        //Check for setting
        if(!SpigotPerms.instance.getImpl().getConfigSettings().isUsingBuiltInChat()){
            return;
        }

        //If another plugin cancels this event
        if(event.isCancelled()){
            return;
        }

        //Get user
        User user = UserManager.instance.getUser(event.getPlayer().getUniqueId());

        if(user == null){
            try {
                UserManager.instance.loadUser(event.getPlayer().getUniqueId(), event.getPlayer().getName()).get();
                user = UserManager.instance.getUser(event.getPlayer().getUniqueId());
                if(user == null){
                    SpigotPerms.instance.getImpl().addToLog("Problem occurred while sending chat message for " + event.getPlayer().getName() + ", was unable to load user!");
                    SpigotPerms.instance.getImpl().consoleLog("Problem occurred while sending chat message for " + event.getPlayer().getName() + ", was unable to load user!");
                    SpigotPerms.instance.getImpl().sendDebugMessage("Problem occurred while sending chat message for &e" + event.getPlayer().getName() + "&7, was unable to load user!");
                    return;
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return;
            }
        }

        event.setCancelled(true);

        //Get displaygroup
        Group displayGroup = GroupManager.instance.getGroupExact(user.getDisplayGroup());
        if(displayGroup == null){
            displayGroup = GroupManager.instance.getDefaultGroup();
            user.addInheritance(displayGroup, Context.CONTEXT_SERVER_LOCAL);
        }


        //Get Chat components
        final ArrayList<String> chatFormat = SpigotConf.instance.getChatFormat();

        Group finalDisplayGroup = displayGroup;
        User finalUser = user;
        Bukkit.getOnlinePlayers().forEach(p -> {
            TextComponent mainComponent = getFormattedChatMessage(event.getPlayer(), event.getMessage(), chatFormat, finalDisplayGroup, finalUser, p);
            p.spigot().sendMessage(mainComponent);
        });

        TextComponent mainComponent = getFormattedChatMessage(event.getPlayer(), event.getMessage(), chatFormat, finalDisplayGroup, finalUser, null);
        SpigotPerms.instance.getImpl().consoleLog("CHAT: " + ChatColor.stripColor(mainComponent.toLegacyText()));

    }

    @NotNull
    public static TextComponent getFormattedChatMessage(@NotNull Player chattedPlayer, String message, ArrayList<String> chatFormat, Group displayGroup, User user, Player receiver) {
        boolean rel = receiver != null;

        ArrayList<TextComponent> components = new ArrayList<>();
        for (String part : chatFormat) {
            part = ChatColor.translateAlternateColorCodes('&', part);
            HoverEvent hoverEvent = null;
            ClickEvent clickEvent = null;

            //Has prefix in it
            boolean containsPrefix = part.contains("<prefix>");

            //Built-in placeholders
            String prefix = displayGroup.getPrefix();
            if (prefix == null || prefix.equals("")) {
                ArrayList<Group> groups = user.getGroupsInOrderOfPriority();
                for (Group group : groups) {
                    if (!group.serverContextAppliesToThisServer()) {
                        continue;
                    }
                    if (group.equals(displayGroup)) continue;
                    if (group.getPrefix() != null && !group.getPrefix().equals("")) {
                        prefix = group.getPrefix();
                    }
                }
            }

            String suffix = displayGroup.getSuffix();
            if (suffix == null || suffix.equals("")) {
                ArrayList<Group> groups = user.getGroupsInOrderOfPriority();
                for (Group group : groups) {
                    if (!group.serverContextAppliesToThisServer()) {
                        continue;
                    }
                    if (group.equals(displayGroup)) continue;
                    if (group.getSuffix() != null && !group.getSuffix().equals("")) {
                        suffix = group.getSuffix();
                    }
                }
            }

            prefix = toColour(prefix);
            suffix = toColour(suffix);

            part = part.replace("<username>", chattedPlayer.getName());
            part = part.replace("<uuid>", chattedPlayer.getUniqueId().toString());
            part = part.replace("<prefix>", prefix);
            part = part.replace("<suffix>", suffix);
            part = part.replace("<chatcolour>", toColour(displayGroup.getChatColour()));
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                part = PlaceholderAPI.setPlaceholders(chattedPlayer, part);
                if(rel)
                    part = PlaceholderAPI.setRelationalPlaceholders(chattedPlayer, receiver, part);
            }

            if (chattedPlayer.hasPermission("chat.placeholders")) {
                part = part.replace("<message>", chattedPlayer.hasPermission("chat.colour") ? message : toColour(message));
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    part = PlaceholderAPI.setPlaceholders(chattedPlayer, part);
                    if(rel)
                        part = PlaceholderAPI.setRelationalPlaceholders(chattedPlayer, receiver, part);
                }
            } else {
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    part = PlaceholderAPI.setPlaceholders(chattedPlayer, part);
                    if(rel)
                        part = PlaceholderAPI.setRelationalPlaceholders(chattedPlayer, receiver, part);
                }
                part = part.replace("<message>", chattedPlayer.hasPermission("chat.colour") ? message : toColour(message));
            }

            //Rank description
            if (containsPrefix) {
                String[] description = displayGroup.getDescription().split("\\\\n");

                TextComponent newLine = new TextComponent(ComponentSerializer.parse("{text: \"\n\"}"));

                TextComponent hover = new TextComponent(toColour(description[0]));

                for (int i = 1; i < description.length; i++) {
                    hover.addExtra(newLine);
                    hover.addExtra(new TextComponent(TextComponent.fromLegacyText(toColour(description[i]))));
                }

                ArrayList<TextComponent> hcomponents = new ArrayList<>();

                hcomponents.add(hover);
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hcomponents.toArray(new BaseComponent[0]));
            }

            //Click Events
            //TODO

            //Hover Events
            //TODO

            TextComponent comp = new TextComponent(TextComponent.fromLegacyText(part));
            comp.setHoverEvent(hoverEvent);
            comp.setClickEvent(clickEvent);
            components.add(comp);

        }

        TextComponent mainComponent = new TextComponent("");

        String lastColors = "";

        for (TextComponent comps : components) {
            mainComponent.addExtra(lastColors + comps);
            lastColors = ChatColor.getLastColors(comps.toLegacyText());
        }
        return mainComponent;
    }

    private static String toColour(String text){
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

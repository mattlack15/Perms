package me.gravitinos.perms.spigot.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
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
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
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

    @EventHandler(priority=EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event){

        if(event.isCancelled()){
            return;
        }

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

        //Get user
        User user = UserManager.instance.getUser(event.getPlayer().getUniqueId());

        event.setCancelled(true);

        //Get displaygroup
        String displayGroupName = user.getDisplayGroup();
        Group displayGroup = GroupManager.instance.getVisibleGroup(displayGroupName);
        if(displayGroup == null){
            displayGroup = GroupManager.instance.getDefaultGroup();
            user.addInheritance(displayGroup, Context.CONTEXT_SERVER_LOCAL);
        }

        //Get Chat components
        ArrayList<String> chatFormat = SpigotConf.instance.getChatFormat();
        ArrayList<TextComponent> components = new ArrayList<>();

        for(String part : chatFormat){
            part = ChatColor.translateAlternateColorCodes('&', part);
            HoverEvent hoverEvent = null;
            ClickEvent clickEvent = null;

            //Has prefix in it
            boolean containsPrefix = part.contains("<prefix>");

            //Built-in placeholders
            part = part.replace("<username>", event.getPlayer().getName());
            part = part.replace("<uuid>", event.getPlayer().getUniqueId().toString());
            part = part.replace("<prefix>", toColour(displayGroup.getPrefix()));
            part = part.replace("<chatcolour>", toColour(displayGroup.getChatColour()));

            if(event.getPlayer().hasPermission("chat.placeholders")) {
                part = part.replace("<message>", event.getMessage());
                if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    part = PlaceholderAPI.setPlaceholders(event.getPlayer(), part);
                }
            } else {
                if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    part = PlaceholderAPI.setPlaceholders(event.getPlayer(), part);
                }
                part = part.replace("<message>", event.getMessage());
            }

            //Rank description
            if(containsPrefix){
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

            TextComponent comp = new TextComponent(part);
            comp.setHoverEvent(hoverEvent);
            comp.setClickEvent(clickEvent);
            components.add(comp);

        }

        TextComponent mainComponent = new TextComponent("");

        for(TextComponent comps : components){
            mainComponent.addExtra(comps);
        }

        Bukkit.getOnlinePlayers().forEach(p -> {
            p.spigot().sendMessage(mainComponent);
        });

        SpigotPerms.instance.getImpl().consoleLog("CHAT: " + ChatColor.stripColor(mainComponent.toLegacyText()));

    }

    private static String toColour(String text){
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

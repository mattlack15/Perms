package me.gravitinos.perms.spigot.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.MutableContextSet;
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
import me.gravitinos.perms.spigot.util.ChatBuilder;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class ChatListener implements Listener {
    public static ChatListener instance;
    private Map<UUID, ArrayList<Consumer<String>>> chatInputHandlers = new HashMap<>();

    public ChatListener() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, SpigotPerms.instance);
    }

    public void addChatInputHandler(@NotNull UUID player, @NotNull Consumer<String> consumer) {
        chatInputHandlers.computeIfAbsent(player, k -> new ArrayList<>());
        chatInputHandlers.get(player).add(consumer);
    }

    protected void clearChatInputHandler(@NotNull UUID player) {
        this.chatInputHandlers.remove(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {

        if (chatInputHandlers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            chatInputHandlers.get(event.getPlayer().getUniqueId()).forEach(c -> {
                try {
                    c.accept(event.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            chatInputHandlers.remove(event.getPlayer().getUniqueId());
            return;
        }

        //Check for setting
        if (!SpigotPerms.instance.getImpl().getConfigSettings().isUsingBuiltInChat()) {
            return;
        }

        //If another plugin cancels this event
        if (event.isCancelled()) {
            return;
        }

        //Get user
        User user = UserManager.instance.getUser(event.getPlayer().getUniqueId());

        if (user == null) {
            try {
                UserManager.instance.loadUser(event.getPlayer().getUniqueId(), event.getPlayer().getName()).get();
                user = UserManager.instance.getUser(event.getPlayer().getUniqueId());
                if (user == null) {
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

        //Get display group
        Group displayGroup = GroupManager.instance.getGroupExact(user.getDisplayGroup());
        if (displayGroup == null) {
            displayGroup = GroupManager.instance.getDefaultGroup();
            user.addInheritance(displayGroup, new MutableContextSet(Context.CONTEXT_SERVER_LOCAL));
        }


        //Get Chat components
        final ArrayList<String> chatFormat = SpigotConf.instance.getChatFormat();

        Group finalDisplayGroup = displayGroup;
        User finalUser = user;

        boolean allowShowItem = SpigotPerms.instance.getConfig().getBoolean("chat-allow-show-item", false);

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<TextComponent> mainComponent = getFormattedChatMessage(event.getPlayer(), event.getMessage(), chatFormat, finalDisplayGroup, finalUser, allowShowItem, players);

        for (int i = 0, playersSize = players.size(); i < playersSize; i++) {
            Player player = players.get(i);
            player.spigot().sendMessage(mainComponent.get(i));
        }

        mainComponent = getFormattedChatMessage(event.getPlayer(), event.getMessage(), chatFormat, finalDisplayGroup, finalUser, allowShowItem, new ArrayList<>());
        SpigotPerms.instance.getImpl().consoleLog("CHAT: " + ChatColor.stripColor(mainComponent.get(0).toLegacyText()));

    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        chatInputHandlers.remove(event.getPlayer().getUniqueId());
    }

    @NotNull
    public static List<TextComponent> getFormattedChatMessage(@NotNull Player chattedPlayer, String message, ArrayList<String> chatFormat, Group displayGroup, User user, boolean allowShowItem, List<Player> receivers) {

        if (receivers.isEmpty())
            receivers.add(null);

        List<ChatBuilder> builderList = new ArrayList<>();
        receivers.forEach(r -> builderList.add(new ChatBuilder()));

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

            //Click Events
            boolean hasCustomClickEvent = false;
            String click = "";
            String lookFor = "<click='";
            if (part.contains(lookFor)) {
                hasCustomClickEvent = true;
                int index = part.indexOf(lookFor);
                index += lookFor.length();
                int end = part.indexOf("'", index);
                click = part.substring(index, end);
                click = click.replace("<username>", chattedPlayer.getName())
                        .replace("<prefix>", prefix);
                click = PlaceholderAPI.setPlaceholders(chattedPlayer, click);

                part = new StringBuilder(part).replace(index - lookFor.length(), end + 2, "").toString();
            }


            //Hover Events
            boolean hasCustomHoverEvent = false;
            String[] hover = new String[0];
            lookFor = "<hover='";
            if (part.contains(lookFor)) {
                hasCustomHoverEvent = true;
                int index = part.indexOf(lookFor);
                index += lookFor.length();
                int end = part.indexOf("'", index);
                String h = part.substring(index, end);
                h = h.replace("<username>", chattedPlayer.getName())
                        .replace("<prefix>", prefix);
                h = PlaceholderAPI.setPlaceholders(chattedPlayer, h);

                hover = h.split("\\\\n");

                part = new StringBuilder(part).replace(index - lookFor.length(), end + 2, "").toString();
            }

            part = part.replace("<username>", chattedPlayer.getName());
            part = part.replace("<uuid>", chattedPlayer.getUniqueId().toString());
            part = part.replace("<prefix>", prefix);
            part = part.replace("<suffix>", suffix);
            part = part.replace("<chatcolour>", toColour(displayGroup.getChatColour()));
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                part = PlaceholderAPI.setPlaceholders(chattedPlayer, part);
            }

            //Replacements in chat format
            part = part.replace(ChatColor.translateAlternateColorCodes('&', "&8[&f&f&7factionless&8] "), "");


            //Rank description
            if (containsPrefix) {
                String[] description = displayGroup.getDescription().split("\\\\n");

                TextComponent newLine = new TextComponent(ComponentSerializer.parse("{text: \"\n\"}"));

                TextComponent hover1 = new TextComponent(toColour(description[0]));

                for (int i = 1; i < description.length; i++) {
                    hover1.addExtra(newLine);
                    hover1.addExtra(new TextComponent(TextComponent.fromLegacyText(toColour(description[i]))));
                }

                ArrayList<TextComponent> hcomponents = new ArrayList<>();

                hcomponents.add(hover1);
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hcomponents.toArray(new BaseComponent[0]));
            }


            //Message

            String savedPart = part;
            for (int i = 0, receiversSize = receivers.size(); i < receiversSize; i++) {
                Player player = receivers.get(i);

                part = savedPart;

                if (chattedPlayer.hasPermission("chat.placeholders")) {
                    part = part.replace("<message>", chattedPlayer.hasPermission("chat.colour") ? message : toColour(message));
                    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                        part = PlaceholderAPI.setPlaceholders(chattedPlayer, part);
                        if (player != null)
                            part = PlaceholderAPI.setRelationalPlaceholders(chattedPlayer, player, part);
                    }
                } else {
                    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                        part = PlaceholderAPI.setPlaceholders(chattedPlayer, part);
                        if (player != null)
                            part = PlaceholderAPI.setRelationalPlaceholders(chattedPlayer, player, part);
                    }
                    part = part.replace("<message>", chattedPlayer.hasPermission("chat.colour") ? message : toColour(message));
                }

                String click0 = click;
                String[] hover0 = Arrays.copyOf(hover, hover.length);

                //Click
                if (hasCustomClickEvent) {
                    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                        click0 = PlaceholderAPI.setPlaceholders(chattedPlayer, click0);
                        if (player != null)
                            click0 = PlaceholderAPI.setRelationalPlaceholders(chattedPlayer, player, click0);
                    }
                    clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, click0);
                }

                //Hover
                if (hasCustomHoverEvent) {
                    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                        for (int j = 0, hover0Length = hover0.length; j < hover0Length; j++) {
                            hover0[i] = PlaceholderAPI.setPlaceholders(chattedPlayer, hover0[i]);
                            if (player != null)
                                hover0[i] = PlaceholderAPI.setRelationalPlaceholders(chattedPlayer, player, hover0[i]);
                        }
                    }
                    TextComponent newLine = new TextComponent(ComponentSerializer.parse("{text: \"\n\"}"));

                    TextComponent hover1 = new TextComponent(toColour(hover0[0]));

                    for (int i1 = 1; i1 < hover0.length; i1++) {
                        hover1.addExtra(newLine);
                        hover1.addExtra(new TextComponent(TextComponent.fromLegacyText(toColour(hover0[i1]))));
                    }

                    ArrayList<TextComponent> hoverComponents = new ArrayList<>();

                    hoverComponents.add(hover1);
                    hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents.toArray(new BaseComponent[0]));
                }

                if (allowShowItem) {
                    ItemStack stack = chattedPlayer.getInventory().getItemInMainHand();
                    if (stack != null && stack.getType() != Material.AIR) {

                        String look = "[item]";
                        int index = part.indexOf(look);
                        while (index != -1) {
                            String splitPart = part.substring(0, index);
                            builderList.get(i).addText(splitPart, hoverEvent, clickEvent);
                            part = ChatColor.getLastColors(splitPart) + part.substring(index + look.length());

                            builderList.get(i).addComponent(new TextComponent(ComponentSerializer.parse(itemToJson(stack))));
                            index = part.indexOf(look);
                        }
                    }
                }

                builderList.get(i).addText(part, hoverEvent, clickEvent);
            }
        }
        List<TextComponent> components = new ArrayList<>();
        builderList.forEach(b -> components.add(b.build()));
        return components;
    }

    private static String toColour(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String itemToJson(ItemStack hand) {
        String item;
        String hoverClose = "}";
        String valueClose = "}\"";
        String tagClose = "}";
        String displayClose = "";
        String finalClose = "}";
        String loreLine;
        if (hand.hasItemMeta()) {
            if (hand.getItemMeta().hasDisplayName()) {
                item = "{ text:\"" + ChatColor.WHITE + "[" + hand.getItemMeta().getDisplayName() + ChatColor.WHITE + "]\", " + "hoverEvent:{action:show_item, value:\"{id:" + hand.getType().toString().toLowerCase() + ", Count:1, tag:{";
            } else {
                item = "{ text:\"[" + formatMaterialName(hand.getType()) + "]\", " + "hoverEvent:{action:show_item, value:\"{id:" + hand.getType().toString().toLowerCase() + ", Count:1, tag:{";
            }

            Iterator<?> i$;
            if (hand.getItemMeta().hasEnchants()) {
                item = item + "ench:[{";

                Enchantment ench;
                for(i$ = hand.getItemMeta().getEnchants().keySet().iterator(); i$.hasNext(); item = item + "id:" + ench.getId() + ",lvl:" + hand.getItemMeta().getEnchants().get(ench) + "},") {
                    ench = (Enchantment)i$.next();
                }

                item = item + "],";
            }

            if(hand.getItemMeta().getItemFlags().contains(ItemFlag.HIDE_ENCHANTS)) {
                item = item + "HideFlags:1, ";
            }

            if (hand.getItemMeta().hasDisplayName() || hand.getItemMeta().hasLore()) {
                item = item + "display:{";
                displayClose = "}";
            }

            if (hand.getItemMeta().hasDisplayName()) {
                item = item + "Name:\\\"" + rainbow(hand.getItemMeta().getDisplayName()) + "\\\",";
            }

            if (hand.getItemMeta().hasLore()) {
                item = item + " Lore:[";

                for(i$ = hand.getItemMeta().getLore().iterator(); i$.hasNext(); item = item + "\\\"" + loreLine + "\\\",") {
                    loreLine = (String)i$.next();
                }

                item = item.substring(0, item.lastIndexOf(44));
                item = item + "]";
            }

            item = item + displayClose + tagClose + valueClose + hoverClose + finalClose;
        } else {
            item = "{ text:\"[" + formatMaterialName(hand.getType()) + "]\", " + "hoverEvent:{action:show_item, value:\"{id:" + hand.getType().toString().toLowerCase() + ", Count:" + hand.getAmount() + "}\"}}";
        }
        return item;
    }

    public static String rainbow(String input) {
        ChatColor currentColor = ChatColor.WHITE;

        StringBuilder newString = new StringBuilder();

        for(int i = 0; i < input.length(); ++i) {
            if (input.charAt(i) == 167) {
                char colorChar = input.charAt(i + 1);
                currentColor = ChatColor.getByChar(colorChar);
                ++i;
            } else {
                newString.append(currentColor).append(input.charAt(i));
            }
        }

        return newString.toString();
    }

    public static String formatMaterialName(Material mat) {
        if (!mat.name().contains("_")) {
            return mat.name().substring(0, 1).toLowerCase() + mat.name().substring(1).toLowerCase();
        } else {
            String newName;
            for(newName = mat.name(); newName.contains("_"); newName = newName.substring(0, 1).toUpperCase() + newName.substring(1, newName.indexOf("_")).toLowerCase() + " " + newName.substring(newName.indexOf("_") + 1, newName.indexOf("_") + 2).toUpperCase() + newName.substring(newName.indexOf("_") + 2, newName.length()).toLowerCase()) {
            }

            return newName;
        }
    }
}

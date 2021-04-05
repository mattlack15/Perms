package me.gravitinos.perms.spigot.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

public class ChatBuilder {
    private final TextComponent comp;

    private String lastColours = "";

    /**
     * Create a blank ChatBuilder
     */
    public ChatBuilder() {
        comp = new TextComponent();
    }

    /**
     * Create a ChatBuilder from some text
     *
     * @param text Text
     */
    public ChatBuilder(String text) {
        comp = fromString(text);
        this.lastColours = ChatColor.getLastColors(ChatColor.translateAlternateColorCodes('&', text));
    }

    /**
     * Convert a String to a colorized TextComponent
     *
     * @param text String
     * @return TextComponent
     */
    private static TextComponent fromString(String text) {
        TextComponent component = new TextComponent(
                TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&', text)
                )
        );
        component.getExtra().forEach(ChatBuilder::fixNonColours);
        return component;
    }

    private static void fixNonColours(BaseComponent component) {
        if(component.getColorRaw() != null) { //Has it's own colour
            if(component.isBoldRaw() == null) {
                component.setBold(false);
            }
            if(component.isObfuscatedRaw() == null) {
                component.setObfuscated(false);
            }
            if(component.isUnderlinedRaw() == null) {
                component.setUnderlined(false);
            }
            if(component.isStrikethroughRaw() == null) {
                component.setStrikethrough(false);
            }
            if(component.isItalicRaw() == null) {
                component.setItalic(false);
            }
        }
    }

    /**
     * Add a TextComponent to this
     *
     * @param component Component
     * @return this
     */
    public ChatBuilder addComponent(BaseComponent component) {

        BaseComponent wrapper = TextComponent.fromLegacyText(lastColours)[0];
        wrapper.addExtra(component);

        comp.addExtra(wrapper);

        lastColours = ChatColor.getLastColors(wrapper.toLegacyText());
        return this;
    }

    /**
     * Add some text
     *
     * @param str Text
     * @return this
     */
    public ChatBuilder addText(String str) {
        return addComponent(fromString(str));
    }

    /**
     * Add the contents of another ChatBuilder
     *
     * @param builder ChatBuilder
     * @return this
     */
    public ChatBuilder addBuilder(ChatBuilder builder) {
        return addComponent(builder.comp);
    }

    /**
     * Add some text with a ClickEvent
     *
     * @param str Text
     * @param e   ClickEvent
     * @return this
     */
    public ChatBuilder addText(String str, ClickEvent e) {
        TextComponent c = fromString(str);
        c.setClickEvent(e);
        return addComponent(c);
    }

    /**
     * Add some text with a HoverEvent
     *
     * @param str Text
     * @param e   HoverEvent
     * @return this
     */
    public ChatBuilder addText(String str, HoverEvent e) {
        TextComponent c = fromString(str);
        c.setHoverEvent(e);
        return addComponent(c);
    }

    /**
     * Add some text with a ClickEvent and HoverEvent
     *
     * @param str Text
     * @param e   HoverEvent
     * @param e2  ClickEvent
     * @return this
     */
    public ChatBuilder addText(String str, HoverEvent e, ClickEvent e2) {
        TextComponent c = fromString(str);
        c.setHoverEvent(e);
        c.setClickEvent(e2);
        return addComponent(c);
    }

    /**
     * Convert this ChatBuilder into a TextComponent for spigot
     *
     * @return TextComponent
     */
    public TextComponent build() {
        return comp;
    }

    /**
     * Send the TextComponent from this ChatBuilder to a player
     *
     * @param player Player
     */
    public void send(Player player) {
        player.spigot().sendMessage(build());
    }

    /**
     * Send the TextComponent from this ChatBuilder to multiple players
     *
     * @param players Collection of Player
     */
    public void sendAll(Collection<? extends Player> players) {
        for (Player player : players) {
            send(player);
        }
    }

    /**
     * Send the TextComponent from this ChatBuilder to multiple players
     *
     * @param players Array of Player
     */
    public void sendAll(Player... players) {
        for (Player player : players) {
            send(player);
        }
    }
}

package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.context.ServerContextType;
import me.gravitinos.perms.core.ladders.LadderManager;
import me.gravitinos.perms.core.ladders.RankLadder;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.listeners.ChatListener;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import me.gravitinos.perms.spigot.util.UtilColour;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuLadderList extends Menu {

    private boolean showForeign;

    private MenuElement backElement;

    public MenuLadderList(MenuElement backElement) {
        super("Rank Ladders", 6);
        this.backElement = backElement;
        this.setup();
    }

    public void setup() {
        List<RankLadder> ladders = LadderManager.instance.getLoadedLadders();
        if(!showForeign)
            ladders.removeIf(l -> !l.getContext().isSatisfiedBy(new MutableContextSet(Context.CONTEXT_SERVER_LOCAL)));
        this.setupActionableList(10, 9 * 5 - 1,  9 * 5, 9 * 6 - 1, (index) -> {
            if(index >= ladders.size())
                return null;
            RankLadder ladder = ladders.get(index);
            return new MenuElement(new ItemBuilder(Material.LADDER, 1).setName("&e&l" + ladder.getName())
            .addLore("&fContext: " + ServerContextType.getType(ladder.getContext()).getDisplay())
            .addLore("&fSize: &7" + ladder.getGroups().size()).build()).setClickHandler((e, i) -> new MenuLadder(ladder, getBackButton((e1, i1) -> {
                setup();
                open((Player) e1.getWhoClicked());
            })).open((Player) e.getWhoClicked()));
        }, 0);

        MenuElement create = new MenuElement(new ItemBuilder(Material.ANVIL, 1).setName("&a&lCreate")
        .addLore("&7Click to create a rank ladder").build()).setClickHandler((e, i) -> {
            Player p = (Player) e.getWhoClicked();
            if(!e.getWhoClicked().hasPermission(SpigotPerms.commandName + ".rankladders.create")){
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                return;
            }
            e.getWhoClicked().sendMessage(ChatColor.translateAlternateColorCodes('&', SpigotPerms.pluginPrefix + "Enter the rank ladder's name in chat:"));
            p.sendTitle("", UtilColour.toColour("&b&lEnter the rank ladder's name in Chat"));
            ChatListener.instance.addChatInputHandler(p.getUniqueId(), (s) -> {
                p.sendTitle("", "");

                RankLadder ladder = new RankLadder(UUID.randomUUID(), new ArrayList<>(), new MutableContextSet(), LadderManager.instance.getDataManager(), new ConcurrentHashMap<>());
                ladder.setName(s);
                ladder.setGodLocked(false);
                LadderManager.instance.addLadder(ladder);
                doInMainThread(() -> new MenuLadder(ladder, getBackButton((e1, i1) -> {
                    setup();
                    open(p);
                })).open(p));
            });
            e.getWhoClicked().closeInventory();
        });

        this.setElement(2, new MenuElement(new ItemBuilder(Material.INK_SACK, 1, showForeign ? (byte) 10 : (byte) 8).setName("&6&lShow Foreign")
                .addLore("&fCurrently: " + (showForeign ? "&aEnabled" : "&cDisabled")).build()).setClickHandler((e, i) -> {
            showForeign = !showForeign;
            setup();
        }));

        this.setElement(4, backElement);

        this.setElement(6, create);

        MenuManager.instance.invalidateInvsForMenu(this);
    }
}

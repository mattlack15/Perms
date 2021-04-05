package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.ladders.LadderManager;
import me.gravitinos.perms.core.ladders.RankLadder;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MenuLadder extends Menu {

    private MenuElement backElement;
    private RankLadder ladder;

    public MenuLadder(RankLadder ladder, MenuElement backElement) {
        super(ladder.getName(), 6);
        this.backElement = backElement;
        this.ladder = ladder;
        this.setup();
    }

    public void setupAddGroup(){
        this.setSize(5);
        this.setTitle("Add a Group");
        this.setAll(null);
        List<Group> groups = GroupManager.instance.getLoadedGroups();
        groups.removeIf(g -> !ladder.canAddGroup(g));
        this.setupActionableList(10, 9 * 4 - 1, 9 * 4, 9 * 5 - 1, (index) -> {
            if(index >= groups.size())
                return null;
            Group group = groups.get(index);
            return new MenuElement(new ItemBuilder(Material.getMaterial(group.getIconCombinedId() >> 4), 1, (byte) (group.getIconCombinedId() & 15)).setName("&e&l" + group.getName()).addLore("&7Click to add this group").build())
                    .setClickHandler((e, i) -> {
                        ladder.addGroup(group);
                        setup();
                    });
        }, 0);
        this.setElement(4, getBackButton((e, i) -> setup()));
        MenuManager.instance.invalidateInvsForMenu(this);
    }

    public void setup() {
        this.setTitle(ladder.getName());
        this.setSize(6);
        this.setAll(null);
        MenuElement godLock = new MenuElement(new ItemBuilder(Material.INK_SACK, 1, ladder.isGodLocked() ? (byte) 10 : (byte) 8).setName("&6&lGod Lock")
        .addLore("&fCurrently: " + (ladder.isGodLocked() ? "&aEnabled" : "&cDisabled")).build()).setClickHandler((e, i) -> {
            if(!PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                getElement(e.getSlot()).addTempLore(this, "&cYou must be a god user to use this!", 60);
                return;
            }
            ladder.setGodLocked(!ladder.isGodLocked());
            setup();
        });

        MenuElement addGroup = new MenuElement(new ItemBuilder(Material.ANVIL, 1).setName("&a&lAdd a Group").addLore("&7Click to add a group")
        .build()).setClickHandler((e, i) -> {
            if(ladder.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                return;
            }
            setupAddGroup();
        });

        MenuElement delete = new MenuElement(new ItemBuilder(Material.REDSTONE_BLOCK, 1).setName("&4&lDelete").addLore("&cClick to delete this Rank Ladder")
        .addLore("&cWARNING: &fNo confirmation is needed").build()).setClickHandler((e, i) -> {
            if(ladder.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                return;
            }
            LadderManager.instance.removeLadder(ladder.getId());
            this.backElement.getClickHandler().handleClick(e, i);
        });

        ladder.cleanupGroups();
        List<Group> groups = new ArrayList<>();
        ladder.getGroups().forEach(g -> {
            Group group = GroupManager.instance.getGroupExact(g);
            if(group != null)
                groups.add(group);
        });
        this.setupActionableList(19, 9 * 5 - 1, 9 * 5, 9 * 6 - 1, (index) -> {
            if(index >= groups.size())
                return null;
            Group group = groups.get(index);
            return new MenuElement(new ItemBuilder(Material.getMaterial(group.getIconCombinedId() >> 4), 1, (byte) (group.getIconCombinedId() & 15)).setName("&e&l" + group.getName())
            .addLore("&fPosition: " + groups.indexOf(group)).addLore("&e&lRight Click - &cRemove").build()).setClickHandler((e, i) -> {
                if(ladder.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
                if(e.getClick().isRightClick()){
                    ladder.removeGroup(group);
                    setup();
                }
            });
        }, 0);

        MenuElement editContext = new MenuElement(new ItemBuilder(Material.COMMAND, 1).setName("&c&lServers (Context)").addLore("&7Click to edit this ladder's context").build())
                .setClickHandler((e, i) -> {
                    if(ladder.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                        getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                        return;
                    }
                    new MenuContextEditor(ladder.getContext(), ladder::setContext, getBackButton((e1, i1) -> {
                        setup();
                        open((Player) e1.getWhoClicked());
                    })).open((Player) e.getWhoClicked());
                });

        this.setElement(0, editContext);
        this.setElement(2, godLock);
        this.setElement(4, this.backElement);
        this.setElement(6, addGroup);
        this.setElement(8, delete);

        MenuManager.instance.invalidateInvsForMenu(this);
    }

}


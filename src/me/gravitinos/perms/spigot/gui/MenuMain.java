package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class MenuMain extends Menu {

    //1.8.8
//    public static final Sound OPEN_USER_LIST_SOUND = Sound.CLICK;
//    public static final Sound OPEN_GROUP_LIST_SOUND = OPEN_USER_LIST_SOUND;
//    public static final Sound DING_SOUND = Sound.ORB_PICKUP;
//    public static final Sound ITEM_BREAK_SOUND = Sound.ITEM_BREAK;

    //1.12.2
    public static final Sound OPEN_USER_LIST_SOUND = matchSound("CLICK");
    public static final Sound OPEN_GROUP_LIST_SOUND = matchSound("CLICK");
    public static final Sound DING_SOUND = matchSound("ORB_PICKUP");
    public static final Sound ITEM_BREAK_SOUND = matchSound("ITEM_BREAK");

    public static Sound matchSound(String match) {
        for (Sound sounds : Sound.values()) {
            if (sounds.toString().contains(match)) {
                return sounds;
            }
        }
        return null;
    }

    public MenuMain(Player player) {
        super(SpigotPerms.instance.getName() + " by Gravitinos", 5);
        this.setup(player);
    }

    private void setup(Player player) {

        //Create the users button
        MenuElement users = new MenuElement(new ItemBuilder(Material.SKULL_ITEM, 1).setupAsSkull("Gravitinos").setName("&6&lUsers")
                .addLore("&7Manage/View users").build()).setClickHandler((e, i) -> {
            if (player.hasPermission(SpigotPerms.commandName + ".user.list")) {
                player.playSound(player.getLocation(), OPEN_USER_LIST_SOUND, 1f, 1f);
                new MenuUserList(player, 4, getBackButton(this)).open(player); //Open user list menu for the viewer
            } else {
                MenuElement clickedElement = this.getElement(e.getSlot());
                clickedElement.addTempLore(this, "&cYou do not have permission to access this!", 60);
            }
        });

        MenuElement groups = new MenuElement(new ItemBuilder(Material.BOOK, 1).setName("&6&lGroups")
                .addLore("&7Manage/View groups").build()).setClickHandler((e, i) -> {
            if (player.hasPermission(SpigotPerms.commandName + ".group.list")) {
                player.playSound(player.getLocation(), OPEN_GROUP_LIST_SOUND, 1f, 1f);
                new MenuGroupList(player, 4, false, getBackButton(this)).open(player);
            } else {
                MenuElement clickedElement = this.getElement(e.getSlot());
                clickedElement.addTempLore(this, "&cYou do not have permission to access this!", 60);
            }
        });

        MenuElement clearAllData = new MenuElement(new ItemBuilder(Material.REDSTONE_BLOCK, 1).setName("&4&lClear All Data").addLore("&7Clears all groups, users, and inheritances").build())
                .setClickHandler((e, i) -> {
                    GroupManager.instance.getDataManager().clearAllData();
                    this.getElement(e.getSlot()).addTempLore(this, "&aDone!", 60);
                });

        MenuElement configEditor = new MenuElement(new ItemBuilder(Material.COMMAND, 1).setName("&9&lConfiguration")
                .addLore("&7Click to edit supported configuration settings").build()).setClickHandler((e, i) -> {
            if (player.hasPermission(SpigotPerms.commandName + ".config")) {
                player.playSound(player.getLocation(), OPEN_GROUP_LIST_SOUND, 1f, 1f);
                new MenuConfig(getBackButton(this)).open(player);
            } else {
                MenuElement clickedElement = this.getElement(e.getSlot());
                clickedElement.addTempLore(this, "&cYou do not have permission to access this!", 60);
            }
        });

        MenuElement rankLadders = new MenuElement(new ItemBuilder(Material.LADDER, 1).setName("&6&lRank Ladders").addLore("&7Manage/View Rank Ladders")
                .build()).setClickHandler((e, i) -> {
            if (player.hasPermission(SpigotPerms.commandName + ".rankladders")) {
                player.playSound(player.getLocation(), OPEN_GROUP_LIST_SOUND, 1f, 1f);
                new MenuLadderList(getBackButton(this)).open((Player) e.getWhoClicked());
            } else {
                MenuElement clickedElement = this.getElement(e.getSlot());
                clickedElement.addTempLore(this, "&cYou do not have permission to access this!", 60);
            }
        });

        this.setElement(13, rankLadders);
        this.setElement(20, users);
        this.setElement(24, groups);
        this.setElement(31, configEditor);

    }

}

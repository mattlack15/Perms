package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.listeners.ChatListener;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.UtilColour;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;

public class MenuMain extends Menu {

    public static final Sound OPEN_USER_LIST_SOUND = Sound.UI_BUTTON_CLICK;
    public static final Sound OPEN_GROUP_LIST_SOUND = Sound.UI_BUTTON_CLICK;


    public MenuMain(Player player) {
        super(SpigotPerms.instance.getName() + " by Gravitinos", 6);
        this.setup(player);
    }
    private void setup(Player player){

        //Create the users button
        MenuElement users = new MenuElement(new ItemBuilder(Material.SKULL_ITEM, 1).setupAsSkull("Gravitinos").setName("&6&lUsers")
        .addLore("&7Manage/View users").build()).setClickHandler((e, i) -> {
            if(player.hasPermission(SpigotPerms.commandName + ".use")){
                player.playSound(player.getLocation(), OPEN_USER_LIST_SOUND, 1f, 1f);
                new MenuUserList(player, 4, getBackButton(this)).open(player); //Open user list menu for the viewer
            } else {
                MenuElement clickedElement = this.getElement(e.getSlot());
                clickedElement.addTempLore(this, "&cYou do not have permission to access this!", 60);
            }
        });

        MenuElement groups = new MenuElement(new ItemBuilder(Material.BOOK, 1).setName("&6&lGroups")
        .addLore("&7Manage/View groups").build()).setClickHandler((e, i) -> {
            if(player.hasPermission(SpigotPerms.commandName + ".use")){
                player.playSound(player.getLocation(), OPEN_GROUP_LIST_SOUND, 1f, 1f);
                new MenuGroupList(player, 4, false, getBackButton(this)).open(player);
            } else {
                MenuElement clickedElement = this.getElement(e.getSlot());
                clickedElement.addTempLore(this, "&cYou do not have permission to access this!", 60);
            }
        });

        this.setElement(20, users);
        this.setElement(24, groups);

    }

}

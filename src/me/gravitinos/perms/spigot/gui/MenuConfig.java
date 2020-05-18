package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class MenuConfig extends Menu {
    private MenuElement backButton;
    public MenuConfig(MenuElement backButton) {
        super("Configuration", 5);
        this.backButton = backButton;
        this.setup();
    }

    public void setup(){

        MenuElement editDefaultGroup = new MenuElement(new ItemBuilder(Material.NAME_TAG, 1).setName("&6&lDefault Group")
        .addLore("&fCurrently: " + PermsManager.instance.getImplementation().getConfigSettings().getDefaultGroup())
        .addLore("&7Click to change").build()).setClickHandler((e, i) -> {
            ArrayList<Group> loadedVisibleGroups = GroupManager.instance.getLoadedGroups();
            loadedVisibleGroups.removeIf(g -> !g.serverContextAppliesToThisServer());
            new UtilMenuActionableList("Select a Group", 3, (index) -> {
                if(!(index < loadedVisibleGroups.size()))
                    return null;

                Group group = loadedVisibleGroups.get(index);
                return new MenuElement(new ItemBuilder(Material.BOOK, 1).setName("&e" + group.getName()).addLore("&7Click to set this group as default").build()).setClickHandler((e1, i1) -> {
                    PermsManager.instance.getImplementation().getConfigSettings().setDefaultGroup(group.getName());
                    this.setup();
                    this.open((Player)e1.getWhoClicked());
                });
            }, getBackButton(this)).open((Player)e.getWhoClicked());
        });

        MenuElement chatEnabler = new MenuElement(new ItemBuilder(Material.SIGN, 1).setName("&6&lChat Handler")
        .addLore("&fCurrently: " + (PermsManager.instance.getImplementation().getConfigSettings().isUsingBuiltInChat() ? "&aEnabled" : "&cDisabled"))
                .addLore("&7Click to toggle").build()).setClickHandler((e, i) -> {
            PermsManager.instance.getImplementation().getConfigSettings().setUsingBuiltInChat(!PermsManager.instance.getImplementation().getConfigSettings().isUsingBuiltInChat());
            setup();
        });

        this.setElement(4, backButton);

        this.setElement(20, editDefaultGroup);
        this.setElement(22, chatEnabler);

        MenuManager.instance.invalidateInvsForMenu(this);
    }
}

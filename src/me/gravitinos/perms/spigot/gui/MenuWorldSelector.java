package me.gravitinos.perms.spigot.gui;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MenuWorldSelector extends UtilMenuActionableList {

    public MenuWorldSelector(Consumer<String> worldConsumer, MenuElement backButton) {
        super("Select World", 4);

        ArrayList<World> worlds = Lists.newArrayList(Bukkit.getWorlds());
        this.setElementSupplier((num) -> {
            num--;
            if(num == -1){
                return new MenuElement(new ItemBuilder(Material.LEAVES, 1).setName("&cALL WORLDS").addLore("&eThis means all worlds; global").addLore("&7Click to select").build())
                        .setClickHandler((e, i) -> {
                            worldConsumer.accept(Context.VAL_ALL);
                            this.getElement(e.getSlot()).addTempLore(this, "&aSelected! &fClick back to go back", 60);
                            MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
                        });
            }
            if(worlds.size() <= num){
                return null;
            }
            World world = worlds.get(num);

            return new MenuElement(new ItemBuilder(Material.GRASS, 1).setName("&f" + world.getName()).addLore("&7Click to select").build())
                    .setClickHandler((e, i) -> {
                        worldConsumer.accept(world.getName());
                        this.getElement(e.getSlot()).addTempLore(this, "&aSelected! &fClick back to go back", 60);
                        MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
                    });
        });

        this.setMargin(1);
        this.setBackButton(backButton);

        this.setupPage(0);
    }
}

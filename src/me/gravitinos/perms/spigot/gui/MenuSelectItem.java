package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MenuSelectItem extends Menu {
    private ArrayList<ItemStack> items;
    private Consumer<ItemStack> callback;
    private MenuElement backButton;
    public MenuSelectItem(String title, ArrayList<ItemStack> items, Consumer<ItemStack> callback, MenuElement backButton) {
        super(title, 5);
        this.items = items;
        this.backButton = backButton;
        this.callback = callback;
        this.setup();
    }

    public void setup() {
        this.setElement(4, backButton);
        this.setupActionableList(9, 9 * 4 - 1, 9 * 4, 9 * 5 - 1, (index) -> {
            if(index >= items.size())
                return null;

            return new MenuElement(items.get(index)).setClickHandler((e, i) -> callback.accept(items.get(index)));
        }, 0);
    }
}

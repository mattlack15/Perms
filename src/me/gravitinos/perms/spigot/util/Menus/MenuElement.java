package me.gravitinos.perms.spigot.util.Menus;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MenuElement {
    private boolean staticItem = true;
    private ItemStack stack;
    private int updateEvery = 2;
    private boolean doUpdates;
    private ClickHandler clickHandler = null;
    private UpdateHandler updateHandler = null;


    public MenuElement(ItemStack stack) {
        this.stack = stack;
    }

    //Getters

    public MenuElement setUpdateHandler(UpdateHandler updateHandler) {
        this.updateHandler = updateHandler;
        return this;
    }

    public MenuElement setClickHandler(ClickHandler clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public UpdateHandler getUpdateHandler() {
        return updateHandler;
    }

    public ClickHandler getClickHandler() {
        return clickHandler;
    }

    //Util
    public MenuElement setDoUpdates(boolean doUpdates) {
        this.doUpdates = doUpdates;
        return this;
    }

    public MenuElement setUpdateEvery(int updateEvery) {
        this.updateEvery = updateEvery;
        return this;
    }

    public int getUpdateEvery() {
        return updateEvery;
    }

    public boolean isDoingUpdates() {
        return doUpdates;
    }

    public boolean isStaticItem() {
        return staticItem;
    }

    public MenuElement setStaticItem(boolean staticItem) {
        this.staticItem = staticItem;
        return this;
    }

    public MenuElement setItem(ItemStack stack) {
        this.stack = stack;
        return this;
    }

    public ItemStack getItem() {
        return stack;
    }

    //Extra classes and interfaces

    public static interface UpdateHandler {
        public void handleUpdate(MenuElement element);
    }

    public static interface ClickHandler {
        public void handleClick(InventoryClickEvent event, InvInfo info);
    }
}

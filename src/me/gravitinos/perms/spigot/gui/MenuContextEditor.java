package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class MenuContextEditor extends Menu {
    private Context context;
    private Consumer<Context> contextConsumer;
    private MenuElement backButton;

    public MenuContextEditor(Context current, Consumer<Context> contextSetter, MenuElement backButton) {
        super("Context editor", 3);
        this.context = current;
        this.backButton = backButton;
        this.contextConsumer = contextSetter;
        this.setup();
    }

    public void setup(){
        MenuElement world = new MenuElement(new ItemBuilder(Material.ENDER_PORTAL_FRAME, 1).setName("&dWorld").addLore("&f" + (context.getWorldName().equals(Context.VAL_ALL) ? "&cALL WORLDS" : context.getWorldName())).addLore("&7Click to set world").build())
                .setClickHandler((e, i) -> new MenuWorldSelector((s) -> {
                    Context newContext = new Context(this.context.getServerName(), s);
                    this.contextConsumer.accept(newContext);
                    this.context = newContext;
                }, Menu.getBackButton(this)).open((Player) e.getWhoClicked()));

        String serverContext = this.context.getServerName().equals(GroupData.SERVER_LOCAL) ? "&aLOCAL" : (this.context.getServerName().equals(GroupData.SERVER_GLOBAL) ? "&cGLOBAL" : "&6" + context.getServerName());
        boolean canChangeServerContext = context.getServerName().equals(GroupData.SERVER_GLOBAL) || context.getServerName().equals(GroupData.SERVER_LOCAL);

        MenuElement server = new MenuElement(new ItemBuilder(Material.WATCH, 1).setName("&cServer").addLore(serverContext).addLore("&7Click to change").build())
                .setClickHandler((e, i) -> {
                    if(!canChangeServerContext){
                        this.getElement(e.getSlot()).addTempLore(this, "&cThis value can only be changed from the server &f" + context.getServerName(), 60);
                        MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
                        return;
                    }
                    Context newContext = new Context(this.context.getServerName().equals(GroupData.SERVER_GLOBAL) ? GroupData.SERVER_LOCAL : GroupData.SERVER_GLOBAL, this.context.getWorldName());
                    this.contextConsumer.accept(newContext);
                    this.context = newContext;
                    new MenuContextEditor(newContext, this.contextConsumer, this.backButton).open((Player) e.getWhoClicked());
                });

        this.setElement(11, world);
        this.setElement(15, server);
        this.setElement(4, this.backButton);
    }

}

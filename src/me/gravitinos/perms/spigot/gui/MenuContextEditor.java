package me.gravitinos.perms.spigot.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;
import java.util.function.Consumer;

public class MenuContextEditor extends Menu {
    private Context context;
    private Consumer<Context> contextConsumer;
    private MenuElement backButton;
    private int timeUnit;

    public MenuContextEditor(Context current, Consumer<Context> contextSetter, MenuElement backButton, int timeUnit) {
        super("Context editor", 3);
        this.context = current;
        this.backButton = backButton;
        this.contextConsumer = contextSetter;
        this.timeUnit = timeUnit;
        this.setup();
    }

    public MenuContextEditor(Context current, Consumer<Context> contextSetter, MenuElement backButton) {
        this(current, contextSetter, backButton, 0);
    }

    public void setup(){
        MenuElement world = new MenuElement(new ItemBuilder(Material.ENDER_PORTAL_FRAME, 1).setName("&dWorld").addLore("&f" + (context.getWorldName().equals(Context.VAL_ALL) ? "&cALL WORLDS" : context.getWorldName())).addLore("&7Click to set world").build())
                .setClickHandler((e, i) -> new MenuWorldSelector((s) -> {
                    Context newContext = new Context(this.context.getServer(), s, this.context.getBeforeTime());
                    this.contextConsumer.accept(newContext);
                    this.context = newContext;
                }, Menu.getBackButton(this)).open((Player) e.getWhoClicked()));

        String serverContext = this.context.getServer().equals(GroupData.SERVER_LOCAL) ? "&aLOCAL" : (this.context.getServer().equals(GroupData.SERVER_GLOBAL) ? "&cGLOBAL" : "&6" + context.getNameOfServer());
        boolean canChangeServerContext = context.getServer().equals(GroupData.SERVER_GLOBAL) || context.getServer().equals(GroupData.SERVER_LOCAL);

        MenuElement server = new MenuElement(new ItemBuilder(Material.COMPASS, 1).setName("&cServer").addLore(serverContext).addLore("&7Click to change").build())
                .setClickHandler((e, i) -> {
                    if(!canChangeServerContext){
                        this.getElement(e.getSlot()).addTempLore(this, "&cThis value can only be changed from the server &f" + context.getNameOfServer(), 60);
                        MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
                        return;
                    }
                    Context newContext = new Context(this.context.getServer().equals(GroupData.SERVER_GLOBAL) ? GroupData.SERVER_LOCAL : GroupData.SERVER_GLOBAL, this.context.getWorldName(), context.getBeforeTime());
                    this.contextConsumer.accept(newContext);
                    this.context = newContext;
                    new MenuContextEditor(newContext, this.contextConsumer, this.backButton).open((Player) e.getWhoClicked());
                });

        long expirSeconds = (context.getBeforeTime() - System.currentTimeMillis()) / 1000;
        String expirTime = expirSeconds > 60 ? expirSeconds > 3600 ? expirSeconds > 86400 ? (expirSeconds / 86400) + " days" :
                (expirSeconds / 3600) + " hours" : (expirSeconds / 60) + " minutes" : expirSeconds + " seconds";

        if(context.getBeforeTime() == 0){
            expirTime = "&cNever";
        }

        Map<String, Long> units = new HashMap<>();
        units.put("Seconds", 1000L);
        units.put("Minutes", 60000L);
        units.put("Hours", 3600000L);
        units.put("Days", 86400000L);
        units.put("Weeks", 604800000L);

        ArrayList<String> keys = Lists.newArrayList(units.keySet());
        keys.sort(Comparator.comparingLong(units::get));

        MenuElement expiration = new MenuElement(new ItemBuilder(Material.WATCH, 1).setName("&9Expiration")
            .addLore("&e" + expirTime)
            .addLore("&7Adding in time unit &c" + keys.get(timeUnit))
            .addLore("&7Left/Right click to increase/decrease time until expiration")
            .addLore("&7Shift Left/Right click to increase/decrease time unit")
            .addLore("&7Hover over, and press &eQ&7 to set to &cnever expire").build()).setClickHandler((e, i) -> {
                if(e.getClick().isShiftClick()) {
                    if (e.getClick().isLeftClick()) {
                        new MenuContextEditor(context, this.contextConsumer, this.backButton, timeUnit + 1 < keys.size() ? timeUnit + 1 : timeUnit).open((Player) e.getWhoClicked());
                    } else if (e.getClick().isRightClick()) {
                        new MenuContextEditor(context, this.contextConsumer, this.backButton, timeUnit - 1 >= 0 ? timeUnit - 1 : timeUnit).open((Player) e.getWhoClicked());
                    }
                } else {
                    long plus = context.getBeforeTime() == 0 ? System.currentTimeMillis() : 0;
                    if(e.getClick().isLeftClick()){
                        Context newContext = new Context(context.getServer(), context.getWorldName(), context.getBeforeTime() + units.get(keys.get(timeUnit)) + plus);
                        contextConsumer.accept(newContext);
                        this.context = newContext;
                        new MenuContextEditor(newContext, this.contextConsumer, this.backButton, timeUnit).open((Player) e.getWhoClicked());
                    } else if (e.getClick().isRightClick()) {
                        Context newContext = new Context(context.getServer(), context.getWorldName(), context.getBeforeTime() - units.get(keys.get(timeUnit)) + plus);
                        contextConsumer.accept(newContext);
                        this.context = newContext;
                        new MenuContextEditor(newContext, this.contextConsumer, this.backButton, timeUnit).open((Player) e.getWhoClicked());
                    } else if(e.getClick().equals(ClickType.DROP)){
                        Context newContext = new Context(context.getServer(), context.getWorldName());
                        contextConsumer.accept(newContext);
                        this.context = newContext;
                        new MenuContextEditor(newContext, this.contextConsumer, this.backButton, timeUnit).open((Player) e.getWhoClicked());
                    }
                }
        });

        this.setElement(11, world);
        this.setElement(13, expiration);
        this.setElement(15, server);
        this.setElement(4, this.backButton);
    }

}

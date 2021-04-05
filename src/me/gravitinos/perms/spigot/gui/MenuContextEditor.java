package me.gravitinos.perms.spigot.gui;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.context.ServerContextType;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;
import java.util.function.Consumer;

/**
 * Currently only supports editing servers
 */
public class MenuContextEditor extends Menu {
    private MutableContextSet contexts;
    private Consumer<ContextSet> contextConsumer;
    private MenuElement backButton;
    private int timeUnit;
    private String groupNameIfExists;

    public MenuContextEditor(ContextSet current, Consumer<ContextSet> contextSetter, MenuElement backButton, String groupNameIfExists, int timeUnit) {
        super("Context editor", 5);
        this.contexts = current.toMutable();
        this.backButton = backButton;
        this.contextConsumer = contextSetter;
        this.timeUnit = timeUnit;
        this.groupNameIfExists = groupNameIfExists;
        this.cleanContext();
        this.setup();
    }

    public MenuContextEditor(ContextSet current, Consumer<ContextSet> contextSetter, MenuElement backButton) {
        this(current, contextSetter, backButton, null, 0);
    }

    public MenuContextEditor(ContextSet current, Consumer<ContextSet> contextSetter, MenuElement backButton, String groupNameIfExists) {
        this(current, contextSetter, backButton, groupNameIfExists, 0);
    }

    private void cleanContext(){
        Iterator<Context> it = contexts.iterator();
        while(it.hasNext()){
            Context context = it.next();
            if(!context.getKey().equals(Context.SERVER_IDENTIFIER))
                continue;
            try {
                int id = Integer.parseInt(context.getValue());
                if(PermsManager.instance.getServerName(id) == null)
                    throw new IllegalArgumentException("");
            } catch(Exception e){
                it.remove();
            }
        }
    }

    public void setup(){

        MutableContextSet servers = contexts.filterByKey(Context.SERVER_IDENTIFIER);

        //List
        this.setupActionableList(10, 9 * 4 - 1,  9 * 4, 9 * 5 - 1, (index) -> {
            if(index >= servers.size())
                return null;
            Context context = servers.get(index);
            int id = Integer.parseInt(context.getValue());
            String serverName = PermsManager.instance.getServerName(id);

            return new MenuElement(new ItemBuilder(Material.COMMAND, 1).setName("&e&l" + serverName + (id == GroupData.SERVER_LOCAL ? " &a&lLOCAL" : ""))
            .addLore("&fRight Click - &cDelete").build()).setClickHandler((e, i) -> {
                contexts.removeContext(context);
                contextConsumer.accept(contexts);
                this.setup();
            });

        }, 0);


        //Expiration
        long expirSeconds = (contexts.getExpiration() - System.currentTimeMillis()) / 1000;
        String expirTime = expirSeconds > 60 ? expirSeconds > 3600 ? expirSeconds > 86400 ? (expirSeconds / 86400) + " days" :
                (expirSeconds / 3600) + " hours" : (expirSeconds / 60) + " minutes" : expirSeconds + " seconds";
        if(expirSeconds <= 0) {
            expirTime = "&4Expired";
        }

        if(contexts.getExpiration() == ContextSet.NO_EXPIRATION){
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
                        timeUnit = timeUnit + 1 < keys.size() ? timeUnit + 1 : timeUnit;
                        this.setup();
                    } else if (e.getClick().isRightClick()) {
                        timeUnit = timeUnit - 1 >= 0 ? timeUnit - 1 : timeUnit;
                        this.setup();
                    }
                } else {
                    long plus = contexts.getExpiration() == ContextSet.NO_EXPIRATION ? System.currentTimeMillis() : 0;
                    if(e.getClick().isLeftClick()){
                        contexts.setExpiration(contexts.getExpiration() + units.get(keys.get(timeUnit)) + plus);
                        contextConsumer.accept(contexts);
                        this.setup();
                    } else if (e.getClick().isRightClick()) {
                        contexts.setExpiration(contexts.getExpiration() - units.get(keys.get(timeUnit)) + plus);
                        contextConsumer.accept(contexts);
                        this.setup();
                    } else if(e.getClick().equals(ClickType.DROP)){
                        contexts.setExpiration(ContextSet.NO_EXPIRATION);
                        contextConsumer.accept(contexts);
                        this.setup();
                    }
                }
        });

        //Adding
        this.setElement(6, new MenuElement(new ItemBuilder(Material.ANVIL, 1).setName("&a&lAdd Server").addLore("&7Click to add a server to this context set").build())
        .setClickHandler((e, i) -> {
            List<String> serverNames = Lists.newArrayList(PermsManager.instance.getCachedServerIndex().values());
            new UtilMenuActionableList("Choose a Server", 3, (index) -> {
                if(index >= serverNames.size())
                    return null;
                String name = serverNames.get(index);
                int id = PermsManager.instance.getServerId(name);
                MutableContextSet newSet = new MutableContextSet(contexts);
                newSet.addContext(new Context(Context.SERVER_IDENTIFIER, Integer.toString(id)));
                if(groupNameIfExists != null &&
                        GroupManager.instance.canGroupContextCollideWithAnyLoaded(groupNameIfExists, newSet, contexts))
                    return new MenuElement(new ItemBuilder(Material.BEDROCK, 1).setName("&e&l" + name + "&f (&c&lUnavailable&f)")
                    .addLore("&7This server is unavailable for this context due to")
                    .addLore("&cVisibility Collisions").build());

                return new MenuElement(new ItemBuilder(Material.COMMAND, 1).setName("&e&l" + name)
                .addLore("&7Click to add this server to the context set").build()).setClickHandler((e1, i1) -> {
                    contexts.addContext(new Context(Context.SERVER_IDENTIFIER, Integer.toString(id)));
                    contextConsumer.accept(contexts);
                    this.setup();
                    open((Player)e1.getWhoClicked());
                });
            }, getBackButton(this)).open((Player)e.getWhoClicked());
        }));

        this.setElement(2, expiration);

        this.setElement(4, this.backButton);

        if(servers.size() == 0){
            this.setElement(3, new MenuElement(new ItemBuilder(Material.INK_SACK, 1, (byte)10).setName("&c&lStatus").addLore("&fCurrent: " + ServerContextType.getType(contexts).getDisplay())
            .addLore("&7Click to make &c&lGLOBAL").build()));
        } else {
            this.setElement(3, new MenuElement(new ItemBuilder(Material.INK_SACK, 1, (byte)8).setName("&c&lStatus").addLore("&fCurrent: " + ServerContextType.getType(contexts).getDisplay())
                    .addLore("&7Click to make &c&lGLOBAL").build()).setClickHandler((e, i) -> {
                contexts.removeContexts(Context.SERVER_IDENTIFIER);
                contextConsumer.accept(contexts);
                this.setup();
            }));
        }

        MenuManager.instance.invalidateInvsForMenu(this);
    }

}

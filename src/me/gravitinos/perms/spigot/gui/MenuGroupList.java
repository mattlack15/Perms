package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.context.ServerContextType;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupBuilder;
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
import me.gravitinos.perms.spigot.util.StringUtil;
import me.gravitinos.perms.spigot.util.UtilColour;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MenuGroupList extends UtilMenuActionableList {

    private Player p;

    private int listRows;

    private boolean listForeign;

    private String[] filters;

    private ArrayList<Group> loadedGroups = GroupManager.instance.getLoadedGroups();
    public MenuGroupList(Player p, int listRows, boolean listForeign, @NotNull MenuElement backButton, String... filters) {
        super("Groups", listRows);
        this.p = p;
        this.listForeign = listForeign;
        this.filters = filters;
        this.listRows = listRows;
        this.setBackButton(backButton);
        this.setMargin(1);
        this.setup();
    }
    public MenuGroupList(Player p, int listRows, boolean listForeign, @NotNull Menu back, String... filters){
        this(p, listRows, listForeign, Menu.getBackButton(back), filters);
    }

    public void setup(){

        ArrayList<Group> loadedGroups = GroupManager.instance.getLoadedGroups();
        loadedGroups.removeIf((g) -> {
            if(!listForeign && !g.serverContextAppliesToThisServer()){
                return true;
            }
            if(filters.length == 0){
                return false;
            }
            for (String filter : filters) {
                if (g.getName().toLowerCase().contains(filter.toLowerCase())) {
                    return false;
                }
            }
            return true;
        });

        this.setElementSupplier((num) -> {
            if(num >= loadedGroups.size()){
                return null;
            }
            Group group = loadedGroups.get(num);
            if(group == null){
                return null;
            }

            ItemBuilder builder = new ItemBuilder(Material.BOOK, 1);

            String name = group.getName();
            for(String filter : filters){
                int index = StringUtil.indexOfIgnoreCase(name, filter);
                if(index == -1){
                    continue;
                }
                String matched = name.substring(index, index + filter.length());
                name = name.replace(matched, "&6&n" + matched + "&e");
            }

            builder.setName("&e" + name);
            builder.addLore("&fServer Context: " + (ServerContextType.getType(group.getContext()).getDisplay()));
            builder.addLore("&fDefault Group: " + (GroupManager.instance.getDefaultGroup().equals(group) ? "&atrue" : "&cfalse"));
            builder.addLore("&fPriority: &a" + group.getPriority());
            builder.addLore("&fPrefix: " + group.getPrefix());
            builder.addLore("&fSuffix: " + group.getSuffix());
            builder.addLore("&fChat Colour: " + group.getChatColour() + "this");
            builder.addLore("&fDescription: ");
            for(String line : group.getDescription().split("\\\\n")){
                builder.addLore(" &f" + line);
            }
            builder.addLore("&fInheritances: ");
            for(Inheritance inheritance : group.getInheritances()){
                Subject parent = inheritance.getParent();
                if(parent instanceof Group){
                    builder.addLore("&7 - " + parent.getName());
                }
            }

            MenuElement element = new MenuElement(builder.build());
            element.setClickHandler((e, i) -> new MenuGroup(group, Menu.getBackButton(this).setClickHandler((e1, i1) -> new MenuGroupList((Player) e1.getWhoClicked(), this.listRows, this.listForeign, this.getBackButton(), this.filters).open((Player) e1.getWhoClicked()))).open((Player) e.getWhoClicked()));

            return element;
        });

        //Search element
        MenuElement searchElement = new MenuElement(new ItemBuilder(Material.NAME_TAG, 1).setName("&eSearch").addLore("&7Click to search")
                .addLore("&aRight click&7 to clear").build())
                .setClickHandler((e, i) -> {
                    if(e.getClick().equals(ClickType.RIGHT)){ // If it's a right click, then reopen with no filters
                       new MenuGroupList(p, this.listRows, listForeign, this.getBackButton()).open(p);
                        return;
                    }
                    ChatListener.instance.addChatInputHandler(p.getUniqueId(), (s) -> doInMainThread(() -> new MenuGroupList(p, listRows, listForeign, this.getBackButton(), s).setupPage(0).open(p)));
                    p.closeInventory();
                    p.sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "Enter search here:"));
                });
        if(filters.length > 0){
            ItemBuilder builder = new ItemBuilder(searchElement.getItem()).addLore("&cCurrent Search:");
            for(String s : filters){
                builder.addLore("&7 - &f\"" + s + "\"");
            }
            searchElement.setItem(builder.build());
        }

        //List Foreign Setting element
        Material mat = Material.INK_SACK;
        int data = listForeign ? 10 : 8; //if listForeign is true then the data makes the ink sack green, else it makes it gray
        MenuElement listForeignSetting = new MenuElement(new ItemBuilder(mat, 1, (byte) data).setName("&eShow Foreign Groups")
                .addLore(listForeign ? "&aEnabled" : "&cDisabled").build()).setClickHandler((e, i) -> new MenuGroupList(p, listRows, !listForeign, this.getBackButton(), filters).open(p)); //If setting up in constructor is removed later, change this

        //Create Group
        MenuElement createGroup = new MenuElement(new ItemBuilder(Material.ANVIL, 1).setName("&a&lCreate New Group").build())
                .setClickHandler((e, i) -> {
                    if(!e.getWhoClicked().hasPermission(SpigotPerms.commandName + ".group.create")){
                        this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                        return;
                    }
                    e.getWhoClicked().sendMessage(ChatColor.translateAlternateColorCodes('&', SpigotPerms.pluginPrefix + "Enter the group name in chat:"));
                    p.sendTitle("", UtilColour.toColour("&b&lEnter the group's name in Chat"),10, 600, 10);
                    ChatListener.instance.addChatInputHandler(p.getUniqueId(), (s) -> {
                        p.sendTitle("", "", 10, 10, 10);
                        Group group = new GroupBuilder(s).build();
                        GroupManager.instance.addGroup(group);
                        doInMainThread(() -> new MenuGroup(group, getBackButton(new MenuGroupList(p, listRows, listForeign, this.getBackButton(), filters))).open(p));
                    });
                    e.getWhoClicked().closeInventory();
                });

        //Add them to menu
        this.setElement(6, searchElement);
        this.setElement(listRows * 9 + 9 + 4, createGroup);
        this.setElement(2, listForeignSetting);

        this.setupPage(0);
    }

}

package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ServerContextType;
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
import me.gravitinos.perms.spigot.util.StringUtil;
import me.gravitinos.perms.spigot.util.UtilColour;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MenuUserList extends UtilMenuActionableList {

    private Player p;

    private int listRows;

    private String[] filters;

    private ArrayList<User> loadedUsers = UserManager.instance.getLoadedUsers();

    public MenuUserList(Player p, int listRows, @NotNull MenuElement backButton, String... filters) {
        super("Users", listRows);
        this.p = p;
        this.filters = filters;
        this.listRows = listRows;
        this.setBackButton(backButton);
        this.setup();
    }

    public MenuUserList(Player p, int listRows, @NotNull Menu back, String... filters) {
        this(p, listRows, Menu.getBackButton(back), filters);
    }

    private void setup() {

        this.setMargin(1);

        loadedUsers.removeIf((u) -> {
            if (filters.length == 0) {
                return false;
            }
            for (String filter : filters) {
                if (u.getName().toLowerCase().contains(filter.toLowerCase())) {
                    return false;
                }
            }
            return true;
        });

        this.setElementSupplier((num) -> {

            //Create user element/item
            if (num >= loadedUsers.size()) {
                return null;
            }
            User indexedUser = loadedUsers.get(num);
            if (indexedUser == null) {
                return null;
            }
            String name = indexedUser.getName();
            for (String filter : filters) {
                int index = StringUtil.indexOfIgnoreCase(name, filter);
                if (index == -1) {
                    continue;
                }
                String matched = name.substring(index, index + filter.length());
                name = name.replace(matched, "&6&n" + matched + "&e");
            }
            MenuElement userElement = new MenuElement(new ItemBuilder(Material.SKULL_ITEM, 1).setupAsSkull(indexedUser.getName()).setName("&e" + name).build());
            ItemBuilder builder = new ItemBuilder(userElement.getItem());
            if (GroupManager.instance.isGroupExactLoaded(indexedUser.getDisplayGroup()))
                builder.addLore("&fDisplay Group: &7" + GroupManager.instance.getGroupExact(indexedUser.getDisplayGroup()).getName());
            builder.addLore("&fPrefix: " + indexedUser.getPrefix());
            builder.addLore("&fSuffix: " + indexedUser.getSuffix());
            builder.addLore("&fGod User: " + (SpigotPerms.instance.getImpl().getConfigSettings().getGodUsers().contains(indexedUser.getName()) ? "&atrue" : "&cfalse"));
            builder.addLore("&fPermission Count: &7" + indexedUser.getOwnPermissions().getPermissions().size());
            builder.addLore("&fInheritances: ");
            for (Inheritance in : indexedUser.getInheritances()) {
                Subject<?> sub = in.getParent();
                if (sub instanceof Group && in.getContext().appliesToAny(Context.CONTEXT_SERVER_LOCAL)) {
                    builder.addLore("&7 - " + sub.getName() + " &e&l: &f" + ServerContextType.getType(in.getContext()).getDisplay());
                }
            }

            userElement.setItem(builder.build());

            userElement.setClickHandler((e, i) -> new MenuUser(indexedUser, Menu.getBackButton(this).setClickHandler((e1, i1) -> new MenuUserList((Player) e1.getWhoClicked(), this.listRows, this.getBackButton(), this.filters).open((Player) e1.getWhoClicked()))).open((Player) e.getWhoClicked()));

            return userElement;

        });

        //Search element
        MenuElement searchElement = new MenuElement(new ItemBuilder(Material.NAME_TAG, 1).setName("&eSearch").addLore("&7Click to search")
                .addLore("&aRight click&7 to clear").build())
                .setClickHandler((e, i) -> {
                    if (e.getClick().equals(ClickType.RIGHT)) { // If it's a right click, then reopen with no filters
                        new MenuUserList(p, listRows, this.getBackButton()).open(p);
                        return;
                    }
                    p.sendTitle("", UtilColour.toColour("&b&lEnter a search query in Chat"));
                    ChatListener.instance.addChatInputHandler(p.getUniqueId(), (s) -> doInMainThread(() -> {
                        p.sendTitle("", "");
                        new MenuUserList(p, listRows, this.getBackButton(), s).open(p);
                    }));
                    p.closeInventory();
                    p.sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "Enter search here:"));
                });
        if (filters.length > 0) {
            ItemBuilder builder = new ItemBuilder(searchElement.getItem()).addLore("&cCurrent Search:");
            for (String s : filters) {
                builder.addLore("&7 - &f\"" + s + "\"");
            }
            searchElement.setItem(builder.build());
        }
        this.setElement(6, searchElement);

        this.setElement(2, new MenuElement(new ItemBuilder(Material.EYE_OF_ENDER, 1).setName("&eLoad Offline User").addLore("&7Loads an offline user").build())
                .setClickHandler((e, i) -> {
                    ((Player)e.getWhoClicked()).sendTitle("", UtilColour.toColour("&b&lEnter their full username in Chat"));
                    ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> {
                        ((Player)e.getWhoClicked()).sendTitle("", "");
                        OfflinePlayer p = Bukkit.getOfflinePlayer(s);
                        e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "Loading..."));
                        if (p != null) {
                            try {
                                UserManager.instance.loadUser(p.getUniqueId(), p.getName()).get();
                            } catch (InterruptedException | ExecutionException ex) {
                                ex.printStackTrace();
                            }
                        }
                        new MenuUserList(this.p, this.listRows, this.getBackButton(), this.filters).open((Player) e.getWhoClicked());
                    }));
                    e.getWhoClicked().closeInventory();
                    e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter player name into chat"));
                }));

        this.setupPage(0);
    }
}

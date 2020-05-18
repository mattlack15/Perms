package me.gravitinos.perms.spigot.gui;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.util.TextUtil;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.listeners.ChatListener;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.StringUtil;
import me.gravitinos.perms.spigot.util.UtilColour;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuPermissionIndexer extends UtilMenuActionableList {

    public interface IndexPermissionSupplier {
        List<String> getCurrentPermissions();

        void addPermission(String permission);
    }

    private MenuElement backButton;
    private Map<String, List<String>> permissionIndex;
    private IndexPermissionSupplier supplier;

    public MenuPermissionIndexer(@NotNull Map<String, List<String>> permissionIndex, @NotNull IndexPermissionSupplier supplier, MenuElement backButton) {
        super("Permission Index", 4);
        this.backButton = backButton;
        this.permissionIndex = permissionIndex;
        this.supplier = supplier;
        this.setupPluginList(false);
    }

    /**
     * @param pluginFilter Whether the filters are plugin filters or permission filters
     * @param filters      Filters (optional)
     */
    public void setupPluginList(boolean pluginFilter, String... filters) {

        ArrayList<String> plugins1 = Lists.newArrayList(permissionIndex.keySet());
        ArrayList<String> plugins = new ArrayList<>();

        //Filter
        if (filters.length != 0) {
            for (String filter : filters) {
                for (String plugin : plugins1) {
                    if (pluginFilter) {
                        if (StringUtil.indexOfIgnoreCase(plugin, filter) != -1) {
                            plugins.add(plugin);
                        }
                    } else {
                        for (String perms : permissionIndex.get(plugin)) {
                            if (StringUtil.indexOfIgnoreCase(perms, filter) != -1) {
                                plugins.add(plugin);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            plugins.addAll(plugins1);
        }
        this.setElementSupplier((index) -> {

            if (!(index < plugins.size())) {
                return null;
            }
            String plugin = plugins.get(index);
            Plugin pluginObj = Bukkit.getPluginManager().getPlugin(plugin);
            PluginDescriptionFile descriptionFile = pluginObj != null ? pluginObj.getDescription() : null;
            ItemBuilder builder = new ItemBuilder(Material.COMMAND, 1)
                    .setName("&e" + plugin)
                    .addLore("&cKnown Permissions: &f" + permissionIndex.get(plugin).size());
            if (!pluginFilter && filters.length != 0) {
                List<String> permissions = Lists.newArrayList(permissionIndex.get(plugin));
                //Filter
                permissions.removeIf(p -> {
                    boolean remove = filters.length != 0;
                    for (String filter : filters) {
                        if (StringUtil.indexOfIgnoreCase(p, filter) != -1) {
                            remove = false;
                            break;
                        }
                    }
                    return remove;
                });

                builder.addLore("&cMatched Permissions: &e" + permissions.size());
            }

            if (descriptionFile != null && descriptionFile.getDescription() != null) {
                for (String line : TextUtil.splitIntoLines(descriptionFile.getDescription().replace("\n", ""), 30)) {
                    builder.addLore("&7" + line);
                }
            }

            MenuElement element = new MenuElement(builder.build());

            element.setClickHandler((e, i) -> {
                if (pluginFilter) {
                    this.savedPluginFilters = filters;
                    setupPermissionList(plugin);
                } else {
                    setupPermissionList(plugin, filters);
                }
            });

            return element;

        });

        this.setBackButton(this.backButton);

        MenuElement searchPluginElement = new MenuElement(new ItemBuilder(Material.NAME_TAG, 1).setName("&a&lSearch for a plugin")
                .addLore("&fCurrent search: &e" + (pluginFilter && filters.length != 0 ? filters[0] : "none"))
                .addLore("&aRight click &7to clear").build()).setClickHandler((e, i) -> {
            Player p = (Player) e.getWhoClicked();
            if (e.getClick().isLeftClick()) {
                p.closeInventory();
                p.sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "Enter plugin search here:"));
                ((Player)e.getWhoClicked()).sendTitle("", UtilColour.toColour("&b&lEnter search query in Chat"),10, 600, 10);
                ChatListener.instance.addChatInputHandler(p.getUniqueId(), (s) -> doInMainThread(() -> {
                    setupPluginList(true, s);
                    open(p);
                }));
            } else {
                savedPluginFilters = new String[0];
                setupPluginList(false);
            }
        });

        MenuElement searchPermissionElement = new MenuElement(new ItemBuilder(Material.NAME_TAG, 1).setName("&a&lSearch for a permission")
                .addLore("&fCurrent search: &e" + (!pluginFilter && filters.length != 0 ? filters[0] : "none"))
                .addLore("&aRight click &7to clear").build()).setClickHandler((e, i) -> {
            Player p = (Player) e.getWhoClicked();
            if (e.getClick().isLeftClick()) {
                p.closeInventory();
                p.sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "Enter part of a permission search here:"));
                ((Player)e.getWhoClicked()).sendTitle("", UtilColour.toColour("&b&lEnter search query in Chat"),10, 600, 10);
                ChatListener.instance.addChatInputHandler(p.getUniqueId(), (s) -> doInMainThread(() -> {
                    ((Player)e.getWhoClicked()).sendTitle("", "", 10, 10, 10);
                    setupPluginList(false, s);
                    open(p);
                }));
            } else {
                setupPluginList(false);
            }
        });

        this.setElement(2, searchPluginElement);
        this.setElement(6, searchPermissionElement);

        this.setupPage(0);

    }

    private String[] savedPluginFilters = new String[0];

    public void setupPermissionList(String plugin, String... filters) {
        List<String> currentPermissions = supplier.getCurrentPermissions();
        List<String> permissions = Lists.newArrayList(permissionIndex.get(plugin));
        Map<String, String> descriptions = new HashMap<>();

        for (Permission perms : Bukkit.getPluginManager().getPermissions()) {
            if (permissions.contains(perms.getName().toLowerCase())) {
                descriptions.put(perms.getName().toLowerCase(), perms.getDescription());
            }
        }

        //Filter
        permissions.removeIf(p -> {
            boolean remove = filters.length != 0;
            for (String filter : filters) {
                if (StringUtil.indexOfIgnoreCase(p, filter) != -1) {
                    remove = false;
                    break;
                }
            }
            return remove;
        });

        //Remove duplicates
        List<String> used = Lists.newArrayList();
        for(String perm : permissions){
            if(!used.contains(perm))
                used.add(perm);
        }
        permissions = used;

        List<String> finalPermissions = permissions;
        this.setElementSupplier((index -> {

            if (!(index < finalPermissions.size())) {
                return null;
            }

            String permission = finalPermissions.get(index);
            String description = descriptions.get(permission);

            final boolean[] alreadyHasPermission = {currentPermissions.contains(permission)};

            ItemBuilder builder = new ItemBuilder(alreadyHasPermission[0] ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, 1)
                    .setName((alreadyHasPermission[0] ? "&a" : "&e") + permission);

            if (description != null && !description.isEmpty()) {
                for (String line : TextUtil.splitIntoLines(description.replace("\n", ""), 30)) {
                    builder.addLore("&7" + line);
                }
            }

            if (alreadyHasPermission[0]) {
                builder.addGlow();
            }

            MenuElement element = new MenuElement(builder.build());
            element.setClickHandler((e, i) -> {
                if (!alreadyHasPermission[0]) {
                    supplier.addPermission(permission);

                    //Reload current permissions (bug fix)
                    currentPermissions.clear();
                    currentPermissions.addAll(supplier.getCurrentPermissions());

                    alreadyHasPermission[0] = true;
                    element.getItem().setType(Material.EMERALD_BLOCK);
                    element.setItem(new ItemBuilder(element.getItem()).setName("&a" + permission)
                            .addGlow().build());
                    element.addTempLore(this, "&7Permission added!", 60);
                    ((Player) e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), MenuMain.DING_SOUND, 0.8f, 1f);
                } else {
                    element.addTempLore(this, "&7Permission&c already&7 added!", 60);
                    ((Player) e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), MenuMain.ITEM_BREAK_SOUND, 0.8f, 1f);
                }
            });

            return element;

        }));

        setBackButton(getBackButton(this).setClickHandler((e, i) -> {
            if (filters.length == 0) {
                setupPluginList(savedPluginFilters.length != 0, savedPluginFilters);
            } else {
                setupPluginList(false, filters);
            }
        }));

        this.setupPage(0);

    }
}

package me.gravitinos.perms.spigot.gui;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.util.TextUtil;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
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

    public interface IndexPermissionSupplier{
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
        this.setupPluginList();
    }

    public void setupPluginList() {

        ArrayList<String> plugins = Lists.newArrayList(permissionIndex.keySet());
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

            if (descriptionFile != null && descriptionFile.getDescription() != null) {
                for (String line : TextUtil.splitIntoLines(descriptionFile.getDescription().replace("\n", ""), 30)) {
                    builder.addLore("&7" + line);
                }
            }

            MenuElement element = new MenuElement(builder.build());

            element.setClickHandler((e, i) -> setupPermissionList(plugin));

            return element;

        });

        this.setBackButton(this.backButton);

        this.setupPage(0);

    }

    public void setupPermissionList(String plugin) {
        List<String> currentPermissions = supplier.getCurrentPermissions();
        List<String> permissions = permissionIndex.get(plugin);
        Map<String, String> descriptions = new HashMap<>();

        for(Permission perms : Bukkit.getPluginManager().getPermissions()){
            if(permissions.contains(perms.getName().toLowerCase())){
                descriptions.put(perms.getName().toLowerCase(), perms.getDescription());
            }
        }

        this.setElementSupplier((index -> {

            if(permissions == null || !(index < permissions.size())){
                return null;
            }

            String permission = permissions.get(index);
            String description = descriptions.get(permission);

            final boolean[] alreadyHasPermission = {currentPermissions.contains(permission)};

            ItemBuilder builder = new ItemBuilder(alreadyHasPermission[0] ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, 1)
                    .setName((alreadyHasPermission[0] ? "&a" : "&e") + permission);

            if(description != null && !description.isEmpty()){
                for (String line : TextUtil.splitIntoLines(description.replace("\n", ""), 30)) {
                    builder.addLore("&7" + line);
                }
            }

            if(alreadyHasPermission[0]){
                builder.addGlow();
            }

            MenuElement element = new MenuElement(builder.build());
            element.setClickHandler((e, i) -> {
                if(!alreadyHasPermission[0]) {
                    supplier.addPermission(permission);
                    alreadyHasPermission[0] = true;
                    element.getItem().setType(Material.EMERALD_BLOCK);
                    element.setItem(new ItemBuilder(element.getItem()).setName("&a" + permission)
                    .addGlow().build());
                    element.addTempLore(this, "&7Permission added!", 60);
                    ((Player)e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), MenuMain.DING_SOUND, 0.8f, 1f);
                } else {
                    element.addTempLore(this, "&7Permission&c already&7 added!", 60);
                    ((Player)e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), MenuMain.ITEM_BREAK_SOUND, 0.8f, 1f);
                }
            });

            return element;

        }));

        setBackButton(getBackButton(this).setClickHandler((e, i) -> setupPluginList()));

        this.setupPage(0);

    }
}

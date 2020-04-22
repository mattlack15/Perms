package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MenuUser extends Menu {

    private User user;
    private MenuElement backElement;

    public MenuUser(User user, MenuElement backElement) {
        super("Manage " + user.getName(), 5);
        this.user = user;
        this.backElement = backElement;
        this.setup();
    }

    public void setup() {
        MenuElement deleteButton = (new MenuElement((new ItemBuilder(Material.REDSTONE_BLOCK, 1)).setName("&4&lDelete").addLore("&cClick to delete this user's player-data").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.user.delete")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                UserManager.instance.removeUser(this.user);
                this.backElement.getClickHandler().handleClick(e, i);
            }
        });
        MenuElement groupsEditor = (new MenuElement((new ItemBuilder(Material.BOOK, 1)).setName("&eGroups").addLore("&7Click to manage groups").build())).setClickHandler((e, i) -> {
            Player p = (Player)e.getWhoClicked();
            if (!p.hasPermission("ranks.user.managegroups")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                (new MenuGroupInheritanceEditor(this.user.getName() + " > Groups", new MenuGroupInheritanceEditor.GroupInheritanceEditorHandler() {
                    public CompletableFuture<Void> addGroup(Group group, Context context) {
                        return MenuUser.this.user.addInheritance(group, context);
                    }

                    public CompletableFuture<Void> removeGroup(Group group) {
                        return MenuUser.this.user.removeInheritance(group);
                    }

                    public Map<Group, Context> getGroups() {
                        Map groupContextMap = new HashMap();
                        MenuUser.this.user.getInheritances().forEach((i) -> {
                            if (i.getParent() instanceof Group) {
                                groupContextMap.put(i.getParent(), i.getContext());
                            }

                        });
                        return groupContextMap;
                    }
                }, Menu.getBackButton((Menu)this))).open(p, new Object[0]);
            }
        });
        MenuElement permissionsEditor = (new MenuElement((new ItemBuilder(Material.EMPTY_MAP, 1)).setName("&ePermissions").addLore("&7Click to manage permissions").build())).setClickHandler((e, i) -> {
            Player p = (Player)e.getWhoClicked();
            if (!p.hasPermission("ranks.user.managepermissions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                (new MenuPermissionEditor("Permissions for " + this.user.getName(), 4, new MenuPermissionEditor.PermissionEditorHandler() {
                    public CompletableFuture<Void> addPermission(PPermission p) {
                        return MenuUser.this.user.addOwnPermission(p);
                    }

                    public CompletableFuture<Void> addPermissions(ArrayList<PPermission> permissions) {
                        return MenuUser.this.user.addOwnPermissions(permissions);
                    }

                    public CompletableFuture<Void> removePermission(PPermission p) {
                        return MenuUser.this.user.removeOwnPermission(p);
                    }

                    public ImmutablePermissionList getPermissions() {
                        return MenuUser.this.user.getOwnPermissions();
                    }
                }, Menu.getBackButton(this))).open(p, new Object[0]);
            }
        });
        this.setElement(4, this.backElement);
        this.setElement(20, permissionsEditor);
        this.setElement(22, groupsEditor);
        this.setElement(24, deleteButton);
        MenuManager.instance.invalidateInvsForMenu(this);
    }
}

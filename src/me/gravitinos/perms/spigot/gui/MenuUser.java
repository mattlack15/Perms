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
        MenuElement permissionsEditor;
        MenuElement groupsEditor;

        groupsEditor = new MenuElement(new ItemBuilder(Material.BOOK, 1).setName("&eGroups").addLore("&7Click to manage groups").build()).setClickHandler((e, i) -> {
            Player p = (Player) e.getWhoClicked();
            if(!p.hasPermission(SpigotPerms.commandName + ".user.managegroups")){
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
                return;
            }

            new MenuGroupInheritanceEditor(user.getName() + " > Groups", new MenuGroupInheritanceEditor.GroupInheritanceEditorHandler() {
                @Override
                public CompletableFuture<Void> addGroup(Group group, Context context) {
                    return user.addInheritance(group, context);
                }

                @Override
                public CompletableFuture<Void> removeGroup(Group group) {
                    return user.removeInheritance(group);
                }

                @Override
                public Map<Group, Context> getGroups() {
                    Map<Group, Context> groupContextMap = new HashMap<>();
                    user.getInheritances().forEach(i -> {
                        if (i.getParent() instanceof Group){
                            groupContextMap.put((Group) i.getParent(), i.getContext());
                        }
                    });
                    return groupContextMap;
                }
            }, Menu.getBackButton(this)).open(p);

        });

        permissionsEditor = new MenuElement(new ItemBuilder(Material.EMPTY_MAP, 1).setName("&ePermissions").addLore("&7Click to manage permissions").build()).setClickHandler((e, i) -> {
            Player p = (Player) e.getWhoClicked();
            if(!p.hasPermission(SpigotPerms.commandName + ".user.managepermissions")){
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
                return;
            }
            new MenuPermissionEditor("Permissions for " + user.getName(), 4, new MenuPermissionEditor.PermissionEditorHandler() {
                @Override
                public CompletableFuture<Void> addPermission(PPermission p) {
                    return user.addOwnPermission(p);
                }

                @Override
                public CompletableFuture<Void> addPermissions(ArrayList<PPermission> permissions) {
                    return user.addOwnPermissions(permissions);
                }

                @Override
                public CompletableFuture<Void> removePermission(PPermission p) {
                    return user.removeOwnPermission(p);
                }

                @Override
                public ImmutablePermissionList getPermissions() {
                    return user.getOwnPermissions();
                }
            }, Menu.getBackButton(this)).open(p);
        });

        this.setElement(4, backElement);
        this.setElement(20, permissionsEditor);
        this.setElement(22, groupsEditor);
        MenuManager.instance.invalidateInvsForMenu(this);
    }
}

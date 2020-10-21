package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.ladders.LadderManager;
import me.gravitinos.perms.core.ladders.RankLadder;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    public void addGroup(Group group, ContextSet context) {
                        MenuUser.this.user.addInheritance(group, context);
                    }

                    public void removeGroup(Group group) {
                        MenuUser.this.user.removeInheritance(group);
                    }

                    @Override
                    public boolean isGodLocked() {
                        return false;
                    }

                    public Map<Group, ContextSet> getGroups() {
                        Map<Group, ContextSet> groupContextMap = new HashMap<>();
                        MenuUser.this.user.getInheritances().forEach((i) -> {
                            if (i.getParent() instanceof Group) {
                                groupContextMap.put((Group) i.getParent(), i.getContext());
                            }

                        });
                        return groupContextMap;
                    }
                }, Menu.getBackButton(this))).open(p);
            }
        });
        MenuElement permissionsEditor = (new MenuElement((new ItemBuilder(Material.EMPTY_MAP, 1)).setName("&ePermissions").addLore("&7Click to manage permissions").build())).setClickHandler((e, i) -> {
            Player p = (Player)e.getWhoClicked();
            if (!p.hasPermission("ranks.user.managepermissions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                (new MenuPermissionEditor("Permissions for " + this.user.getName(), 4, new MenuPermissionEditor.PermissionEditorHandler() {
                    public void addPermission(PPermission p) {
                        MenuUser.this.user.addPermission(p);
                    }

                    public void addPermissions(ArrayList<PPermission> permissions) {
                        MenuUser.this.user.addPermissions(permissions);
                    }

                    public void removePermission(PPermission p) {
                        MenuUser.this.user.removePermission(p);
                    }

                    public ImmutablePermissionList getPermissions() {
                        return MenuUser.this.user.getOwnPermissions();
                    }

                    @Override
                    public boolean isGodLocked() {
                        return false;
                    }
                }, Menu.getBackButton(this))).open(p, new Object[0]);
            }
        });

        MenuElement demote = new MenuElement(new ItemBuilder(Material.WOOL, 1, (byte) 14).setName("&cDemote").addLore("&7Click to demote this user")
        .addLore("&7in a certain rank ladder").build()).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.user.managegroups")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                List<RankLadder> ladders = LadderManager.instance.getLoadedLadders();
                new UtilMenuActionableList("Pick a rank ladder", 3, (index) -> {
                    if (index >= ladders.size())
                        return null;
                    RankLadder ladder = ladders.get(index);
                    return new MenuElement(new ItemBuilder(Material.LADDER, 1).setName("&e&l" + ladder.getName()).build()).setClickHandler((e1, i1) -> {
                        ladder.demote(user);
                        setup();
                        open((Player) e1.getWhoClicked());
                    });
                }, getBackButton(this)).open((Player) e.getWhoClicked());
            }
        });

        MenuElement promote = new MenuElement(new ItemBuilder(Material.WOOL, 1, (byte) 5).setName("&aPromote").addLore("&7Click to promote this user")
                .addLore("&7in a certain rank ladder").build()).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.user.managegroups")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                List<RankLadder> ladders = LadderManager.instance.getLoadedLadders();
                new UtilMenuActionableList("Pick a rank ladder", 3, (index) -> {
                    if (index >= ladders.size())
                        return null;
                    RankLadder ladder = ladders.get(index);
                    return new MenuElement(new ItemBuilder(Material.LADDER, 1).setName("&e&l" + ladder.getName()).build()).setClickHandler((e1, i1) -> {
                        ladder.promote(user);
                        setup();
                        open((Player) e1.getWhoClicked());
                    });
                }, getBackButton(this)).open((Player) e.getWhoClicked());
            }
        });

        this.setElement(39, demote);
        this.setElement(41, promote);
        this.setElement(4, this.backElement);
        this.setElement(20, permissionsEditor);
        this.setElement(22, groupsEditor);
        this.setElement(24, deleteButton);
        MenuManager.instance.invalidateInvsForMenu(this);
    }
}

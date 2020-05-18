package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.ServerContextType;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.listeners.ChatListener;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import me.gravitinos.perms.spigot.util.UtilColour;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MenuGroup extends Menu {

    private MenuElement backElement;
    private Group group;

    public MenuGroup(Group group, MenuElement backElement) {
        super(group.getName(), 6);
        this.backElement = backElement;
        this.group = group;
        this.setup();
    }

    public void setup() {
        String confirmationMessage = ChatColor.RED + "Click again to confirm";
        MenuElement deleteButton = (new MenuElement((new ItemBuilder(Material.REDSTONE_BLOCK, 1)).setName("&4&lDelete").addLore("&cClick to delete this group").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.delete")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                if(!new ItemBuilder(getElement(e.getSlot()).getItem()).getLore().contains(confirmationMessage)){
                    this.getElement(e.getSlot()).addTempLore(this, "&e", 60);
                    this.getElement(e.getSlot()).addTempLore(this, "&eAre you sure you want to do this?", 60);
                    this.getElement(e.getSlot()).addTempLore(this, confirmationMessage, 60);
                } else {
                    GroupManager.instance.removeGroup(this.group);
                    this.backElement.getClickHandler().handleClick(e, i);
                }
            }
        });
        MenuElement priorityChanger = (new MenuElement((new ItemBuilder(Material.EXP_BOTTLE, 1)).setName("&ePriority").addLore("&a" + this.group.getPriority()).addLore("&eLeft Click - &aIncrease").addLore("&eRight Click - &cDecrease").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                if (e.isRightClick()) {
                    this.group.setPriority(this.group.getPriority() - 1);
                } else if (e.isLeftClick()) {
                    this.group.setPriority(this.group.getPriority() + 1);
                }

                this.setup();
            }
        });
        MenuElement serverChanger = new MenuElement(new ItemBuilder(Material.ENDER_PORTAL_FRAME, 1).setName("&eServers")
                .addLore("&f" + ServerContextType.getType(group.getContext()).getDisplay()).build()).setClickHandler((e, i) -> new MenuContextEditor(group.getContext(), (c) -> group.setContext(c), getBackButton((e1, i1) -> {
            setup();
            open((Player)e1.getWhoClicked());
        }), group.getName()).open((Player)e.getWhoClicked()));
        MenuElement prefixChanger = (new MenuElement((new ItemBuilder(Material.NAME_TAG, 1)).setName("&ePrefix").addLore("&f" + this.group.getPrefix()).addLore(ChatColor.WHITE + "(" + this.group.getPrefix().replace(ChatColor.COLOR_CHAR + "", "&") + ChatColor.WHITE + ")", false).addLore("&7Click to change").addLore("&7Right Click to clear").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else if(e.getClick().equals(ClickType.RIGHT)) {
                this.group.setPrefix("");
                this.setup();
                this.open((Player)e.getWhoClicked());
            } else {
                ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                    this.group.setPrefix(s.replace("\"", "").replace("\'", ""));
                    this.setup();
                    this.open((Player)e.getWhoClicked());
                });
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new prefix into chat"));
            }
        });
        ItemBuilder builder = (new ItemBuilder(Material.PAPER, 1)).setName("&eDescription");
        String[] var11 = this.group.getDescription().split("\\\\n");
        int var12 = var11.length;

        for(int var13 = 0; var13 < var12; ++var13) {
            String line = var11[var13];
            builder.addLore(" &f" + line);
        }

        MenuElement descriptionChanger = (new MenuElement(builder.addLore("&7Click to change").addLore("&7Right Click to clear").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else if(e.getClick().equals(ClickType.RIGHT)) {
                this.group.setDescription("");
                this.setup();
                this.open((Player)e.getWhoClicked());
            } else {
                ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                    this.group.setDescription(s);
                    this.setup();
                    this.open((Player)e.getWhoClicked());
                });
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new description into chat (\\n means next line)"));
            }
        });
        MenuElement suffixChanger = (new MenuElement((new ItemBuilder(Material.POTATO_ITEM, 1)).setName("&eSuffix").addLore("&f" + this.group.getSuffix()).addLore("&7Click to change").addLore("&7Right Click to clear").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else if(e.getClick().equals(ClickType.RIGHT)) {
                this.group.setSuffix("");
                this.setup();
                this.open((Player)e.getWhoClicked());
            } else {
                ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                    this.group.setSuffix(s.replace("\"", "").replace("\'", ""));
                    this.setup();
                    this.open((Player)e.getWhoClicked());
                });
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new suffix into chat"));
            }
        });
        MenuElement chatColourChanger = (new MenuElement((new ItemBuilder(Material.BOOK_AND_QUILL, 1)).setName("&eChat Colour").addLore("&f" + this.group.getChatColour() + "this").addLore("&7Click to change").addLore("&7Right Click to clear").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else if(e.getClick().equals(ClickType.RIGHT)) {
                this.group.setSuffix("");
                this.setup();
                this.open((Player)e.getWhoClicked());
            } else {
                ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                    this.group.setChatColour(s);
                    this.setup();
                    this.open((Player)e.getWhoClicked());
                });
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new chat colour into chat"));
            }
        });
        MenuElement groupsEditor = (new MenuElement((new ItemBuilder(Material.BOOK, 1)).setName("&eParents").addLore("&7Click to manage parents (inheritance)").build())).setClickHandler((e, i) -> {
            Player p = (Player)e.getWhoClicked();
            if (!p.hasPermission("ranks.group.manageparents")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                (new MenuGroupInheritanceEditor(this.group.getName() + " > Groups", new MenuGroupInheritanceEditor.GroupInheritanceEditorHandler() {
                    public CompletableFuture<Void> addGroup(Group group1, ContextSet context) {
                        return MenuGroup.this.group.addInheritance(group1, context);
                    }

                    public CompletableFuture<Void> removeGroup(Group group1) {
                        return MenuGroup.this.group.removeInheritance(group1);
                    }

                    public Map<Group, ContextSet> getGroups() {
                        Map<Group, ContextSet> groupContextMap = new HashMap<>();
                        MenuGroup.this.group.getInheritances().forEach((i) -> {
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
            if (!p.hasPermission("ranks.group.managepermissions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                (new MenuPermissionEditor("Permissions for " + this.group.getName(), 4, new MenuPermissionEditor.PermissionEditorHandler() {
                    public CompletableFuture<Void> addPermission(PPermission p) {
                        return MenuGroup.this.group.addOwnPermission(p);
                    }

                    public CompletableFuture<Void> addPermissions(ArrayList<PPermission> permissions) {
                        return MenuGroup.this.group.addOwnPermissions(permissions);
                    }

                    public CompletableFuture<Void> removePermission(PPermission p) {
                        return MenuGroup.this.group.removeOwnPermission(p);
                    }

                    public ImmutablePermissionList getPermissions() {
                        return MenuGroup.this.group.getOwnPermissions();
                    }
                }, Menu.getBackButton(this))).open(p);
            }
        });
        this.setElement(4, this.backElement);
        this.setElement(6, deleteButton);
        this.setElement(19, permissionsEditor);
        this.setElement(21, groupsEditor);
        this.setElement(23, serverChanger);
        this.setElement(25, prefixChanger);
        this.setElement(37, suffixChanger);
        this.setElement(39, chatColourChanger);
        this.setElement(41, descriptionChanger);
        this.setElement(43, priorityChanger);
        MenuManager.instance.invalidateInvsForMenu(this);
    }
}

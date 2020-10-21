package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.ServerContextType;
import me.gravitinos.perms.core.group.Group;
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
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
            } else {
                if (group.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
                if (!new ItemBuilder(getElement(e.getSlot()).getItem()).getLore().contains(confirmationMessage)) {
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
                if (group.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
                if (e.isRightClick()) {
                    this.group.setPriority(this.group.getPriority() - 1);
                } else if (e.isLeftClick()) {
                    this.group.setPriority(this.group.getPriority() + 1);
                }

                this.setup();
            }
        });
        MenuElement serverChanger = new MenuElement(new ItemBuilder(Material.ENDER_PORTAL_FRAME, 1).setName("&eServers")
                .addLore("&f" + ServerContextType.getType(group.getContext()).getDisplay()).build()).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
            } else {
                if (group.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
                new MenuContextEditor(group.getContext(), (c) -> group.setContext(c), getBackButton((e1, i1) -> {
                    setup();
                    open((Player) e1.getWhoClicked());
                }), group.getName()).open((Player) e.getWhoClicked());
            }
        });
        MenuElement prefixChanger = (new MenuElement((new ItemBuilder(Material.NAME_TAG, 1)).setName("&ePrefix").addLore("&f" + this.group.getPrefix()).addLore(ChatColor.WHITE + "(" + this.group.getPrefix().replace(ChatColor.COLOR_CHAR + "", "&") + ChatColor.WHITE + ")", false).addLore("&7Click to change").addLore("&7Right Click to clear").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                return;
            } else {
                if (group.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
            }
            if (e.getClick().equals(ClickType.RIGHT)) {
                this.group.setPrefix("");
                this.setup();
                this.open((Player) e.getWhoClicked());
            } else {
                ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                    this.group.setPrefix(s.replace("\"", "").replace("\'", ""));
                    this.setup();
                    this.open((Player) e.getWhoClicked());
                });
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new prefix into chat"));
            }
        });
        ItemBuilder builder = (new ItemBuilder(Material.PAPER, 1)).setName("&eDescription");
        String[] var11 = this.group.getDescription().split("\\\\n");
        int var12 = var11.length;

        for (int var13 = 0; var13 < var12; ++var13) {
            String line = var11[var13];
            builder.addLore(" &f" + line);
        }

        MenuElement descriptionChanger = (new MenuElement(builder.addLore("&7Click to change").addLore("&7Right Click to clear").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                return;
            } else {
                if (group.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
            }
            if (e.getClick().equals(ClickType.RIGHT)) {
                this.group.setDescription("");
                this.setup();
                this.open((Player) e.getWhoClicked());
            } else {
                ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                    this.group.setDescription(s);
                    this.setup();
                    this.open((Player) e.getWhoClicked());
                });
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new description into chat (\\n means next line)"));
            }
        });
        MenuElement suffixChanger = (new MenuElement((new ItemBuilder(Material.POTATO_ITEM, 1)).setName("&eSuffix").addLore("&f" + this.group.getSuffix()).addLore("&7Click to change").addLore("&7Right Click to clear").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                return;
            } else {
                if (group.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
            }
            if (e.getClick().equals(ClickType.RIGHT)) {
                this.group.setSuffix("");
                this.setup();
                this.open((Player) e.getWhoClicked());
            } else {
                ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                    this.group.setSuffix(s.replace("\"", "").replace("\'", ""));
                    this.setup();
                    this.open((Player) e.getWhoClicked());
                });
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new suffix into chat"));
            }
        });
        MenuElement chatColourChanger = (new MenuElement((new ItemBuilder(Material.BOOK_AND_QUILL, 1)).setName("&eChat Colour").addLore("&f" + this.group.getChatColour() + "this").addLore("&7Click to change").addLore("&7Right Click to clear").build())).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                return;
            } else {
                if (group.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
            }
            if (e.getClick().equals(ClickType.RIGHT)) {
                this.group.setSuffix("");
                this.setup();
                this.open((Player) e.getWhoClicked());
            } else {
                ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                    this.group.setChatColour(s);
                    this.setup();
                    this.open((Player) e.getWhoClicked());
                });
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new chat colour into chat"));
            }
        });
        MenuElement groupsEditor = (new MenuElement((new ItemBuilder(Material.BOOK, 1)).setName("&eParents").addLore("&7Click to manage parents (inheritance)").build())).setClickHandler((e, i) -> {
            Player p = (Player) e.getWhoClicked();
            if (!p.hasPermission("ranks.group.manageparents")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                (new MenuGroupInheritanceEditor(this.group.getName() + " > Groups", new MenuGroupInheritanceEditor.GroupInheritanceEditorHandler() {
                    public void addGroup(Group group1, ContextSet context) {
                        MenuGroup.this.group.addInheritance(group1, context);
                    }

                    public void removeGroup(Group group1) {
                        MenuGroup.this.group.removeInheritance(group1);
                    }

                    @Override
                    public boolean isGodLocked() {
                        return group.isGodLocked();
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
            Player p = (Player) e.getWhoClicked();
            if (!p.hasPermission("ranks.group.managepermissions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
            } else {
                (new MenuPermissionEditor("Permissions for " + this.group.getName(), 4, new MenuPermissionEditor.PermissionEditorHandler() {
                    public void addPermission(PPermission p) {
                        MenuGroup.this.group.addPermission(p);
                    }

                    public void addPermissions(ArrayList<PPermission> permissions) {
                        MenuGroup.this.group.addOwnPermissions(permissions);
                    }

                    public void removePermission(PPermission p) {
                        MenuGroup.this.group.removePermission(p);
                    }

                    public ImmutablePermissionList getPermissions() {
                        return MenuGroup.this.group.getPermissions();
                    }

                    @Override
                    public boolean isGodLocked() {
                        return group.isGodLocked();
                    }
                }, Menu.getBackButton(this))).open(p);
            }
        });

        MenuElement godLock = new MenuElement(new ItemBuilder(Material.INK_SACK, 1, group.isGodLocked() ? (byte) 10 : (byte) 8).setName("&6&lGod Lock")
                .addLore("&fCurrently: " + (group.isGodLocked() ? "&aEnabled" : "&cDisabled")).build()).setClickHandler((e, i) -> {
            if(!PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                getElement(e.getSlot()).addTempLore(this, "&cYou must be a god user to use this!", 60);
                return;
            }
            group.setGodLocked(!group.isGodLocked());
            setup();
        });

        MenuElement iconEditor = new MenuElement(new ItemBuilder(Material.getMaterial(group.getIconCombinedId() >> 4), 1, (byte) (group.getIconCombinedId() & 15))
        .setName("&6&lIcon").addLore("&7Click to change").build()).setClickHandler((e, i) -> {
            if (!e.getWhoClicked().hasPermission("ranks.group.manageoptions")) {
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                return;
            } else {
                if (group.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
            }
            new MenuSelectItem("Choose Icon", selectableIcons, (item) -> {
                group.setIconCombinedId(item.getTypeId() << 4 | item.getData().getData());
                setup();
                open((Player) e.getWhoClicked());
            }, getBackButton(this)).open((Player) e.getWhoClicked());
        });

        this.setElement(1, iconEditor);
        this.setElement(2, godLock);
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

    private static ArrayList<ItemStack> selectableIcons = new ArrayList<>();

    static {
        selectableIcons.add(new ItemStack(Material.PAPER));
        selectableIcons.add(new ItemStack(Material.GRASS));
        selectableIcons.add(new ItemStack(Material.COBBLESTONE));
        selectableIcons.add(new ItemStack(Material.COAL_BLOCK));
        selectableIcons.add(new ItemStack(Material.COAL_ORE));
        selectableIcons.add(new ItemStack(Material.DIAMOND_ORE));
        selectableIcons.add(new ItemStack(Material.EMERALD_ORE));
        selectableIcons.add(new ItemStack(Material.IRON_ORE));
        selectableIcons.add(new ItemStack(Material.LAPIS_BLOCK));
        selectableIcons.add(new ItemStack(Material.GOLD_ORE));
        selectableIcons.add(new ItemStack(Material.QUARTZ));
        selectableIcons.add(new ItemStack(Material.NETHER_BRICK));
        selectableIcons.add(new ItemStack(Material.NETHERRACK));
        selectableIcons.add(new ItemStack(Material.SKULL_ITEM));
        selectableIcons.add(new ItemStack(Material.DIAMOND_BLOCK));
        selectableIcons.add(new ItemStack(Material.GOLD_BLOCK));
        selectableIcons.add(new ItemStack(Material.EMERALD_BLOCK));
        selectableIcons.add(new ItemStack(Material.BEDROCK));
        selectableIcons.add(new ItemStack(Material.GOLD_SWORD));
        selectableIcons.add(new ItemStack(Material.IRON_SWORD));
        selectableIcons.add(new ItemStack(Material.WOOD_SWORD));
        selectableIcons.add(new ItemStack(Material.DIAMOND_SWORD));
        selectableIcons.add(new ItemStack(Material.POTION));
        selectableIcons.add(new ItemStack(Material.BRICK));
        selectableIcons.add(new ItemStack(Material.BREWING_STAND_ITEM));
        selectableIcons.add(new ItemStack(Material.ENDER_PEARL));
        selectableIcons.add(new ItemStack(Material.EGG));
        selectableIcons.add(new ItemStack(Material.BOOK));
        selectableIcons.add(new ItemStack(Material.BOOK_AND_QUILL));
        selectableIcons.add(new ItemStack(Material.EYE_OF_ENDER));
        selectableIcons.add(new ItemStack(Material.BARRIER));
        for(int i = 0; i < 16; i++){
            selectableIcons.add(new ItemStack(Material.WOOL, 1, (byte) i));
            selectableIcons.add(new ItemStack(Material.STAINED_GLASS, 1, (byte) i));
            selectableIcons.add(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) i));
            selectableIcons.add(new ItemStack(Material.INK_SACK, 1, (byte) i));
        }

    }

}

package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.listeners.ChatListener;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import me.gravitinos.perms.spigot.util.UtilColour;
import org.bukkit.Material;
import org.bukkit.entity.Player;

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

    public void setup(){
        MenuElement permissionsEditor;
        MenuElement groupsEditor;
        MenuElement chatColourChanger;
        MenuElement priorityChanger;
        MenuElement prefixChanger;
        MenuElement suffixChanger;
        MenuElement serverChanger;

        serverChanger = new MenuElement(new ItemBuilder(Material.ENDER_PORTAL_FRAME, 1).setName("&eServer").addLore("&6" + (group.getServerContext().equals(GroupData.SERVER_GLOBAL) ? "&cGLOBAL" : (group.getServerContext().equals(GroupData.SERVER_LOCAL) ? "&aLOCAL" : group.getServerContext()))).addLore("&7Click to change").build())
                .setClickHandler((e, i) -> {
                    if(!group.getServerContext().equals(GroupData.SERVER_LOCAL) && !group.getServerContext().equals(GroupData.SERVER_GLOBAL)){
                        this.getElement(e.getSlot()).addTempLore(this, "&cCannot change foreign server context", 60); //Auto-invalidates element
                        return;
                    }
                    this.getElement(e.getSlot()).setItem(new ItemBuilder(this.getElement(e.getSlot()).getItem()).setLore(1,"&7Working...").build());
                    this.getElement(e.getSlot()).setClickHandler((e1, i1) -> {});
                    MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
                    String finalServer = group.getServerContext().equals(GroupData.SERVER_LOCAL) ? GroupData.SERVER_GLOBAL : GroupData.SERVER_LOCAL;
                    group.getDataManager().performOrderedOpAsync(() -> {
                        group.setServerContext(finalServer);
                        ArrayList<PPermission> perms = group.getOwnPermissions().getPermissions();
                        group.removeOwnPermissions(perms);
                        ArrayList<PPermission> newPerms = new ArrayList<>();
                        perms.forEach(p -> newPerms.add(new PPermission(p.getPermission(), new Context(finalServer, p.getContext().getWorldName()), p.getExpiry(), p.getPermissionIdentifier())));
                        group.addOwnPermissions(newPerms);
                        this.setup();
                        return null;
                    });
                });

        prefixChanger = new MenuElement(new ItemBuilder(Material.NAME_TAG, 1).setName("&ePrefix").addLore("&f" + group.getPrefix()).addLore("&7Click to change").build())
                .setClickHandler((e, i) -> {
                    ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                        group.setPrefix(s);
                        this.setup();
                        this.open((Player) e.getWhoClicked());
                    });
                    e.getWhoClicked().closeInventory();
                    e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new prefix into chat"));
                });

        suffixChanger = new MenuElement(new ItemBuilder(Material.POTATO_ITEM, 1).setName("&eSuffix").addLore("&f" + group.getSuffix()).addLore("&7Click to change").build())
                .setClickHandler((e, i) -> {
                    ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                        group.setSuffix(s);
                        this.setup();
                        this.open((Player) e.getWhoClicked());
                    });
                    e.getWhoClicked().closeInventory();
                    e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new suffix into chat"));
                });

        chatColourChanger = new MenuElement(new ItemBuilder(Material.BOOK_AND_QUILL, 1).setName("&eChat Colour").addLore("&f" + group.getChatColour() + "this").addLore("&7Click to change").build())
                .setClickHandler((e, i) -> {
                    ChatListener.instance.addChatInputHandler(e.getWhoClicked().getUniqueId(), (s) -> {
                        group.setChatColour(s);
                        this.setup();
                        this.open((Player) e.getWhoClicked());
                    });
                    e.getWhoClicked().closeInventory();
                    e.getWhoClicked().sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eEnter new chat colour into chat"));
                });

        groupsEditor = new MenuElement(new ItemBuilder(Material.BOOK, 1).setName("&eParents").addLore("&7Click to manage parents (inheritance)").build()).setClickHandler((e, i) -> {
            Player p = (Player) e.getWhoClicked();
            if(!p.hasPermission(SpigotPerms.commandName + ".group.manageparents")){
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
                return;
            }

            new MenuGroupInheritanceEditor(group.getName() + " > Groups", new MenuGroupInheritanceEditor.GroupInheritanceEditorHandler() {
                @Override
                public CompletableFuture<Void> addGroup(Group group1, Context context) {
                    return group.addInheritance(group1, context);
                }

                @Override
                public CompletableFuture<Void> removeGroup(Group group1) {
                    return group.removeInheritance(group1);
                }

                @Override
                public Map<Group, Context> getGroups() {
                    Map<Group, Context> groupContextMap = new HashMap<>();
                    group.getInheritances().forEach(i -> {
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
            if(!p.hasPermission(SpigotPerms.commandName + ".group.managepermissions")){
                this.getElement(e.getSlot()).addTempLore(this, "&cYou do not have access to this!", 60);
                MenuManager.instance.invalidateElementsInInvForMenu(this, e.getSlot());
                return;
            }
            new MenuPermissionEditor("Permissions for " + group.getName(), 4, new MenuPermissionEditor.PermissionEditorHandler() {
                @Override
                public CompletableFuture<Void> addPermission(PPermission p) {
                    return group.addOwnPermission(p);
                }

                @Override
                public CompletableFuture<Void> addPermissions(ArrayList<PPermission> permissions) {
                    return group.addOwnPermissions(permissions);
                }

                @Override
                public CompletableFuture<Void> removePermission(PPermission p) {
                    return group.removeOwnPermission(p);
                }

                @Override
                public ImmutablePermissionList getPermissions() {
                    return group.getOwnPermissions();
                }
            }, Menu.getBackButton(this)).open(p);
        });



        this.setElement(4, backElement);
        this.setElement(19, permissionsEditor);
        this.setElement(21, groupsEditor);
        this.setElement(23, serverChanger);
        this.setElement(25, prefixChanger);
        this.setElement(37, suffixChanger);
        this.setElement(39, chatColourChanger);
        MenuManager.instance.invalidateInvsForMenu(this);
    }
}

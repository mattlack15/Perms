package me.gravitinos.perms.spigot.gui;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.context.ServerContextType;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.listeners.ChatListener;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.StringUtil;
import me.gravitinos.perms.spigot.util.UtilBookInput;
import me.gravitinos.perms.spigot.util.UtilColour;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MenuPermissionEditor extends UtilMenuActionableList {

    public interface PermissionEditorHandler {
        void addPermission(PPermission p);

        void addPermissions(ArrayList<PPermission> permissions);

        void removePermission(PPermission p);

        ImmutablePermissionList getPermissions();

        boolean isGodLocked();
    }

    private PermissionEditorHandler handler;

    private MenuElement backElement;

    private String title;

    public MenuPermissionEditor(String title, int listRows, PermissionEditorHandler handler, @NotNull MenuElement backButton) {
        super(title, listRows);
        this.title = title;
        this.handler = handler;
        this.backElement = backButton;
        this.setupSeverSelector();
    }

    public void setupSeverSelector() {
        ArrayList<Integer> servers = Lists.newArrayList(PermsManager.instance.getCachedServerIndex().keySet());
        servers.remove((Integer)GroupData.SERVER_LOCAL);
        servers.add(0, GroupData.SERVER_LOCAL);
        servers.add(0, -1);

        this.setTitle("Select a Server");


        this.setElementSupplier((index -> {
            if (!(index < servers.size())) {
                return null;
            }

            String confirmMessage = ChatColor.RED + "Shift Click to confirm";

            final int server = servers.get(index);

            if(PermsManager.instance.getServerName(server) == null && server != -1)
                return null;

            MenuElement element = new MenuElement(new ItemBuilder(Material.COMMAND, 1).setName("&a" + (server == -1 ? "GLOBAL" : PermsManager.instance.getServerName(server)))
                    .addLore("&7Click to see permissions that apply to this server")
                    .addLore("&7Press Q&7 if this server doesn't exist to &cremove").addGlow().build());
            element.setClickHandler((e, i) -> {
                if (e.getClick().equals(ClickType.DROP)) {
                        if (server != -1) {
                            if(PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                                element.addTempLore(this, confirmMessage, 60);
                            } else {
                                element.addTempLore(this, "&cYou must be a God User to delete servers!", 60);
                            }
                        } else {
                            element.addTempLore(this, "&4Cannot delete GLOBAL", 60);
                        }
                } else if(e.getClick().equals(ClickType.SHIFT_LEFT)) {
                    if (new ItemBuilder(element.getItem()).getLore().contains(confirmMessage)) {
                        if (server != -1) {
                            PermsManager.instance.removeServerFromIndex(servers.get(index));
                            this.setupSeverSelector();
                        }
                    }
                } else {
                    this.setupPermissions(server, 0);
                }
            });

            return element;
        }));

        this.setBackButton(backElement);

        this.setupPage(0, true);
    }

    public void setupPermissions(int server, int page, String... filters) {

        this.setTitle(title);

        ArrayList<PPermission> permissions = handler.getPermissions().getPermissions();
        permissions.removeIf(p -> {
            boolean remove = !p.getContext().appliesToAny(new Context(Context.SERVER_IDENTIFIER, Integer.toString(server)) /* no server id will be -1 so global will work */) || (p.getContext().filterByKey(Context.SERVER_IDENTIFIER).size() == 0 && server != -1);
            if(!remove && filters.length != 0) {
                remove = true;
                for (String filter : filters) {
                    if (StringUtil.indexOfIgnoreCase(p.getPermission(), filter) != -1) {
                        remove = false;
                        break;
                    }
                }
            }
            return remove;
        });
        this.setElementSupplier((num) -> {
            if (permissions.size() <= num) {
                return null;
            }

            PPermission permission = permissions.get(num);

            long expirSeconds = (permission.getContext().getExpiration() - System.currentTimeMillis()) / 1000;
            String expirTime = expirSeconds > 60 ? expirSeconds > 3600 ? expirSeconds > 86400 ? (expirSeconds / 86400) + " days" :
                    (expirSeconds / 3600) + " hours" : (expirSeconds / 60) + " minutes" : expirSeconds + " seconds";

            if (permission.getContext().getExpiration() == ContextSet.NO_EXPIRATION) {
                expirTime = "&cNever";
            }

            MenuElement element = new MenuElement(new ItemBuilder(Material.EMPTY_MAP, 1).build());
            ItemBuilder builder = new ItemBuilder(element.getItem());
            builder.setName("&e" + permission.getPermission());
            builder.addLore("&fExpiry: &7" + expirTime);
            builder.addLore("&fIdentifier: &7" + permission.getPermissionIdentifier().toString().substring(0, 8) + "...");
            builder.addLore("&fContext - &eLeft click to edit&f:");
            builder.addLore("&f  Server: &7" + ServerContextType.getType(permission.getContext()).getDisplay());
            builder.addLore("&cRight click to &4delete");

            element.setItem(builder.build());
            element.setClickHandler((e, i) -> {
                if(handler.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    element.addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
                if (e.getClick().equals(ClickType.RIGHT)) {
                    handler.removePermission(permission);
                    this.setupPermissions(server, page);
                    return;
                }
                new MenuContextEditor(permission.getContext(), (c) -> SpigotPerms.instance.getImpl().getAsyncExecutor().execute(() -> {
                    permissions.remove(permission);
                    permissions.add(new PPermission(permission.getPermission(), c, permission.getPermissionIdentifier()));
                    this.setupPermissions(server, page);
                    handler.removePermission(permission);
                    handler.addPermission(new PPermission(permission.getPermission(), c, permission.getPermissionIdentifier()));
                }), Menu.getBackButton(this).setClickHandler((e1, i1) -> {
                    this.setupPermissions(server, page);
                    open((Player) e.getWhoClicked());
                })).open((Player) e.getWhoClicked());
            });

            return element;
        });

        //Add permission element
        this.setElement(6, new MenuElement(new ItemBuilder(Material.SIGN, 1).setName("&aAdd").addLore("&fLeft Click &7to add single permission")
                .addLore("&fRight Click &7to add permission through permission index")
                .addLore("&fShift Left Click &7to add multiple permissions through a book and quill (Copy/Paste)").build()).setClickHandler((e, i) -> {
            if(handler.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                return;
            }
            Player p = (Player) e.getWhoClicked();
            if (e.getClick().equals(ClickType.LEFT)) {
                p.closeInventory();
                p.sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "Enter the permission in chat (can include context after a space)"));
                p.sendTitle("", UtilColour.toColour("&b&lEnter a permission in Chat"));
                ChatListener.instance.addChatInputHandler(p.getUniqueId(), (s) -> {
                    p.sendTitle("", "");
                    String perm = s;
                    ContextSet context = server == -1 ? new MutableContextSet() : new MutableContextSet(new Context(Context.SERVER_IDENTIFIER, Integer.toString(server)));
                    handler.addPermission(new PPermission(perm, context));
                    this.setupPermissions(server, page);
                    Menu.doInMainThread(() -> this.open(p));
                });
            } else if (e.getClick().equals(ClickType.SHIFT_LEFT)) {
                ItemStack stack = new ItemStack(Material.BOOK_AND_QUILL, 1);
                BookMeta meta = (BookMeta) stack.getItemMeta();
                meta.addPage(UtilColour.toColour("&0Enter permissions on the next page separated by a comma and a space like this perm1, perm2, perm3\n&4Be advised, you cannot enter contexts!"));
                meta.setAuthor("Gravitinos");
                meta.setTitle("Permissions");
                stack.setItemMeta(meta);
                p.closeInventory();
                new BukkitRunnable() {
                    public void run() {
                        p.sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "&eOpen the book in your hand"));
                        UtilBookInput.getBookInput(p, stack, (m) -> {
                            if (m.getPageCount() < 2) {
                                return;
                            }
                            ArrayList<String> toAdd = new ArrayList<>();
                            ArrayList<String> pages = Lists.newArrayList(m.getPages());
                            for (int i1 = 1; i1 < pages.size(); i1++) {
                                String page = pages.get(i1);
                                page = page.replace("\n", "").replace(ChatColor.BLACK + "", "");
                                String[] perms = page.split(", ");
                                toAdd.addAll(Arrays.asList(perms));
                            }
                            ArrayList<PPermission> permsToAdd = new ArrayList<>();
                            toAdd.forEach(s -> permsToAdd.add(new PPermission(s, server == -1 ? new MutableContextSet() : new MutableContextSet(new Context(Context.SERVER_IDENTIFIER, Integer.toString(server))))));
                            handler.addPermissions(permsToAdd);
                            permissions.clear();
                            permissions.addAll(handler.getPermissions().getPermissions());
                            setupPermissions(server, page);
                            Menu.doInMainThread(() -> MenuPermissionEditor.this.open(p));
                        });
                    }
                }.runTaskLater(SpigotPerms.instance, 1);
            } else if (e.getClick().equals(ClickType.RIGHT)) {
                new MenuPermissionIndexer(SpigotPerms.instance.getPermissionIndex(), new MenuPermissionIndexer.IndexPermissionSupplier() {
                    @Override
                    public List<String> getCurrentPermissions() {
                        List<String> currentPermissions = new ArrayList<>();
                        handler.getPermissions().forEach(p -> {
                            if (p.getContext().appliesToAny(new Context(Context.SERVER_IDENTIFIER, Integer.toString(server)))) {
                                currentPermissions.add(p.getPermission());
                            }
                        });
                        return currentPermissions;
                    }

                    @Override
                    public void addPermission(String permission) {
                        handler.addPermission(new PPermission(permission, server == -1 ? new MutableContextSet() : new MutableContextSet(new Context(Context.SERVER_IDENTIFIER, Integer.toString(server)))));
                    }
                }, getBackButton(this).setClickHandler((e1, i1) -> {
                    setupPermissions(server, page);
                    open((Player) e1.getWhoClicked());
                })).open((Player) e.getWhoClicked());
            }
        }));

        MenuElement searchElement = new MenuElement(new ItemBuilder(Material.NAME_TAG, 1).setName("&a&lSearch for a permission")
                .addLore("&fCurrent search: &e" + (filters.length != 0 ? filters[0] : "none"))
                .addLore("&aRight click &7to clear").build()).setClickHandler((e, i) -> {
                    Player p = (Player) e.getWhoClicked();
                    if(e.getClick().isLeftClick()) {
                        p.closeInventory();
                        p.sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "Enter part of a permission search here:"));
                        p.sendTitle("", UtilColour.toColour("&b&lEnter part of a permission in Chat"));
                        ChatListener.instance.addChatInputHandler(p.getUniqueId(), (s) -> doInMainThread(() -> {
                            p.sendTitle("", "");
                            setupPermissions(server, page, s);
                            open(p);
                        }));
                    } else {
                        setupPermissions(server, page);
                    }
        });

        this.setElement(2, searchElement);

        this.setBackButton(getBackButton((e, i) -> setupSeverSelector()));

        this.setupPage(page);
    }

}

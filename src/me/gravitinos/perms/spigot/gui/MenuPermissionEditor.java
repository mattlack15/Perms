package me.gravitinos.perms.spigot.gui;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsImplementation;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.subject.ImmutablePermissionList;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.listeners.ChatListener;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import me.gravitinos.perms.spigot.util.UtilBookInput;
import me.gravitinos.perms.spigot.util.UtilColour;
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
        CompletableFuture<Void> addPermission(PPermission p);

        CompletableFuture<Void> addPermissions(ArrayList<PPermission> permissions);

        CompletableFuture<Void> removePermission(PPermission p);

        ImmutablePermissionList getPermissions();
    }

    private PermissionEditorHandler handler;

    private MenuElement backElement;

    public MenuPermissionEditor(String title, int listRows, PermissionEditorHandler handler, @NotNull MenuElement backButton) {
        super(title, listRows);
        this.handler = handler;
        this.backElement = backButton;
        this.setupSeverSelector();
    }

    public void setupSeverSelector() {
        ArrayList<String> servers = Lists.newArrayList(PermsManager.instance.getCachedServerIndex().values());
        servers.add(GroupData.SERVER_GLOBAL);


        this.setElementSupplier((index -> {
            if (!(index < servers.size())) {
                return null;
            }

            String confirmMessage = ChatColor.RED + "Shift Click to confirm";

            final String server = servers.get(index);

            MenuElement element = new MenuElement(new ItemBuilder(Material.COMMAND, 1).setName("&a" + (server.equals(GroupData.SERVER_GLOBAL) ? "GLOBAL" : server))
                    .addLore("&7Click to see permissions that apply to this server")
                    .addLore("&7Press Q&7 if this server doesn't exist to &cremove").addGlow().build());
            element.setClickHandler((e, i) -> {
                if (e.getClick().equals(ClickType.DROP)) {
                        if (!server.equals(GroupData.SERVER_GLOBAL)) {
                            element.addTempLore(this, confirmMessage, 60);
                        } else {
                            element.addTempLore(this, "&4Cannot delete GLOBAL", 60);
                        }
                } else if(e.getClick().equals(ClickType.SHIFT_LEFT)) {
                    if (new ItemBuilder(element.getItem()).getLore().contains(confirmMessage)) {
                        if (!server.equals(GroupData.SERVER_GLOBAL)) {
                            PermsManager.instance.removeServerFromIndex(PermsManager.instance.getServerId(servers.get(index)));
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

    public void setupPermissions(String server, int page) {
        String serverId = Integer.toString(PermsManager.instance.getServerId(server));
        ArrayList<PPermission> permissions = handler.getPermissions().getPermissions();
        permissions.removeIf(p -> !p.getContext().getServer().equals((serverId.equals("-1") ? "" : serverId)));
        this.setElementSupplier((num) -> {
            if (permissions.size() <= num) {
                return null;
            }

            PPermission permission = permissions.get(num);

            long expirSeconds = (permission.getContext().getBeforeTime() - System.currentTimeMillis()) / 1000;
            String expirTime = expirSeconds > 60 ? expirSeconds > 3600 ? expirSeconds > 86400 ? (expirSeconds / 86400) + " days" :
                    (expirSeconds / 3600) + " hours" : (expirSeconds / 60) + " minutes" : expirSeconds + " seconds";

            if (permission.getContext().getBeforeTime() == 0) {
                expirTime = "&cNever";
            }

            MenuElement element = new MenuElement(new ItemBuilder(Material.EMPTY_MAP, 1).build());
            ItemBuilder builder = new ItemBuilder(element.getItem());
            builder.setName("&e" + permission.getPermission());
            builder.addLore("&fExpiry: &7" + expirTime);
            builder.addLore("&fIdentifier: &7" + permission.getPermissionIdentifier().toString().substring(0, 8) + "...");
            builder.addLore("&fContext - &eLeft click to edit&f:");
            builder.addLore("&f  World: &7" + permission.getContext().getWorldName());
            String serverName = permission.getContext().getServer();
            builder.addLore("&f  Server: &7" + (serverName.equals(GroupData.SERVER_LOCAL) ? "&alocal" : (serverName.equals(GroupData.SERVER_GLOBAL) ? "&cglobal" : "&6" + PermsManager.instance.getServerName(serverName))));
            builder.addLore("&cRight click to &4delete");

            element.setItem(builder.build());
            element.setClickHandler((e, i) -> {
                if (e.getClick().equals(ClickType.RIGHT)) {
                    handler.removePermission(permission);
                    this.setupPermissions(server, page);
                    return;
                }
                new MenuContextEditor(permission.getContext(), (c) -> SpigotPerms.instance.getImpl().getAsyncExecutor().execute(() -> {
                    try {
                        permissions.remove(permission);
                        permissions.add(new PPermission(permission.getPermission(), c, permission.getPermissionIdentifier()));
                        this.setupPermissions(server, page);
                        handler.removePermission(permission).get();
                        handler.addPermission(new PPermission(permission.getPermission(), c, permission.getPermissionIdentifier()));
                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace();
                    }
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
            Player p = (Player) e.getWhoClicked();
            if (e.getClick().equals(ClickType.LEFT)) {
                ChatListener.instance.addChatInputHandler(p.getUniqueId(), (s) -> {
                    String[] spaceSplit = s.split(" ");
                    String perm;
                    Context context = new Context(serverId, Context.VAL_ALL);
                    if (spaceSplit.length > 1) {
                        perm = spaceSplit[0];
                        context = Context.fromString(s.substring(perm.length()));
                    } else if (spaceSplit.length == 1) {
                        perm = spaceSplit[0];
                    } else {
                        perm = " ";
                    }
                    handler.addPermission(new PPermission(perm, context));
                    this.setupPermissions(server, page);
                    Menu.doInMainThread(() -> this.open(p));
                });
                p.closeInventory();
                p.sendMessage(UtilColour.toColour(SpigotPerms.pluginPrefix + "Enter the permission in chat (can include context after a space)"));
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
                            new UtilMenuActionableList("Select Server Context", 1, (num) -> {
                                if (num == 0) {
                                    return new MenuElement(new ItemBuilder(Material.EMPTY_MAP, 1).setName("&aLocal").addLore("&7Click to add all permissions as &alocal&7 permissions").build())
                                            .setClickHandler((e1, i1) -> {
                                                ArrayList<PPermission> permsToAdd = new ArrayList<>();
                                                toAdd.forEach(s -> permsToAdd.add(new PPermission(s, new Context(serverId, Context.VAL_ALL))));
                                                handler.addPermissions(permsToAdd);
                                                permissions.clear();
                                                permissions.addAll(handler.getPermissions().getPermissions());
                                                setupPermissions(server, page);
                                                Menu.doInMainThread(() -> MenuPermissionEditor.this.open(p));
                                            });
                                } else if (num == 4) {
                                    return new MenuElement(new ItemBuilder(Material.MAP, 1).setName("&bGlobal").addLore("&7Click to add all permissions as &cglobal&7 permissions").build())
                                            .setClickHandler((e1, i1) -> {
                                                ArrayList<PPermission> permsToAdd = new ArrayList<>();
                                                toAdd.forEach(s -> permsToAdd.add(new PPermission(s, new Context(serverId, Context.VAL_ALL))));
                                                handler.addPermissions(permsToAdd);
                                                permissions.clear();
                                                permissions.addAll(handler.getPermissions().getPermissions());
                                                setupPermissions(server, page);
                                                Menu.doInMainThread(() -> MenuPermissionEditor.this.open(p));
                                            });
                                }
                                return null;
                            }, (MenuElement) null).setMargin(2).setupPage(0, true).open(p);
                        });
                    }
                }.runTaskLater(SpigotPerms.instance, 1);
            } else if (e.getClick().equals(ClickType.RIGHT)) {
                new MenuPermissionIndexer(SpigotPerms.instance.getPermissionIndex(), new MenuPermissionIndexer.IndexPermissionSupplier() {
                    @Override
                    public List<String> getCurrentPermissions() {
                        List<String> currentPermissions = new ArrayList<>();
                        handler.getPermissions().forEach(p -> {
                            if (p.getContext().getServer().equals(GroupData.SERVER_LOCAL) ||
                                    p.getContext().getServer().equals(GroupData.SERVER_GLOBAL)) {
                                currentPermissions.add(p.getPermission());
                            }
                        });
                        return currentPermissions;
                    }

                    @Override
                    public void addPermission(String permission) {
                        handler.addPermission(new PPermission(permission, new Context(serverId, Context.VAL_ALL)));
                    }
                }, getBackButton(this).setClickHandler((e1, i1) -> {
                    setupPermissions(server, page);
                    open((Player) e1.getWhoClicked());
                })).open((Player) e.getWhoClicked());
            }
        }));

        this.setBackButton(getBackButton((e, i) -> setupSeverSelector()));

        this.setupPage(page);
    }

}

package me.gravitinos.perms.spigot.gui;

import com.google.common.collect.Lists;
import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.ServerContextType;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.Subject;
import me.gravitinos.perms.spigot.SpigotPerms;
import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MenuGroupInheritanceEditor extends UtilMenuActionableList {

    public interface GroupInheritanceEditorHandler{
        void addGroup(Group group, ContextSet context);
        void removeGroup(Group group);
        boolean isGodLocked();
        Map<Group, ContextSet> getGroups();
    }

    private GroupInheritanceEditorHandler handler;

    public MenuGroupInheritanceEditor(@NotNull String title, @NotNull GroupInheritanceEditorHandler handler, MenuElement backButton) {
        super(title, 4);
        this.handler = handler;
        this.setBackButton(backButton);
        this.setMargin(1);
        this.setup();
    }

    public void setup(){

        Map<Group, ContextSet> contexts = handler.getGroups();

        ArrayList<Group> groups = Lists.newArrayList(contexts.keySet());

        this.setElementSupplier((num) -> {

            if(num >= groups.size()){
                return null;
            }

            Group group = groups.get(num);

            long expirSeconds = (contexts.get(group).getExpiration() - System.currentTimeMillis()) / 1000;
            String expirTime = expirSeconds > 60 ? expirSeconds > 3600 ? expirSeconds > 86400 ? (expirSeconds / 86400) + " days" :
                    (expirSeconds / 3600) + " hours" : (expirSeconds / 60) + " minutes" : expirSeconds + " seconds";

            if(contexts.get(group).getExpiration() == -1){
                expirTime = "&cNever";
            }

            ItemBuilder builder = new ItemBuilder(Material.getMaterial(group.getIconCombinedId() >> 4), 1, (byte) (group.getIconCombinedId() & 15));
            builder.setName("&e" + group.getName());
            builder.addLore("&9Expiration: &f" + expirTime);
            builder.addLore("&6Inheritance Context: &6" + ServerContextType.getType(contexts.get(group)).getDisplay());
            builder.addLore("&fGroup Server Context: &6" + ServerContextType.getType(group.getContext()).getDisplay());
            builder.addLore("&fDefault Group: " + (GroupManager.instance.getDefaultGroup().equals(group) ? "&atrue" : "&cfalse"));
            builder.addLore("&fPriority: &a" + group.getPriority());
            builder.addLore("&fPrefix: " + group.getPrefix());
            builder.addLore("&fSuffix: " + group.getSuffix());
            builder.addLore("&fChat Colour: " + group.getChatColour() + "this");
            builder.addLore("&fDescription: ");
            for(String line : group.getDescription().split("\\\\n")){
                builder.addLore(" &f" + line);
            }
            builder.addLore("&fInheritances: ");
            for(Inheritance inheritance : group.getInheritances()){
                Subject<?> parent = inheritance.getParent();
                if(parent instanceof Group){
                    builder.addLore("&7 - " + ((Group) parent).getName());
                }
            }

            builder.addLore("");
            builder.addLore("&eRight Click &f- &cRemove");
            builder.addLore("&eLeft Click &f- &6Edit Context");
            builder.addLore("&eShift Left Click &f- &dOpen Group");

            return new MenuElement(builder.build()).setClickHandler((e, i) -> {
                if(handler.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                    getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                    return;
                }
                if(e.getClick().isRightClick()){
                    handler.removeGroup(group);
                    groups.clear();
                    contexts.clear();
                    contexts.putAll(handler.getGroups());
                    groups.addAll(contexts.keySet());
                    this.setupPage(this.getCurrentPage());
                } else if(e.getClick().isShiftClick() && e.getClick().isLeftClick()){
                    new MenuGroup(group, Menu.getBackButton(this)).open((Player) e.getWhoClicked());
                } else {
                    new MenuContextEditor(contexts.get(group), (c) -> SpigotPerms.instance.getImpl().getAsyncExecutor().execute(() -> {
                        contexts.put(group, c);
                        this.setupPage(this.getCurrentPage());
                        handler.removeGroup(group);
                        handler.addGroup(group, c);
                    }), Menu.getBackButton(this)).open((Player) e.getWhoClicked());
                }
            });
        });

        this.setElement(6,new MenuElement(new ItemBuilder(Material.ANVIL, 1).setName("&aAdd").addLore("&7Add a group").build())
        .setClickHandler((e, i) -> {
            ArrayList<Group> groupList = GroupManager.instance.getLoadedGroups();
            groupList.removeIf(g -> !g.serverContextAppliesToThisServer());
            if(handler.isGodLocked() && !PermsManager.instance.getGodUsers().contains(e.getWhoClicked().getName())) {
                getElement(e.getSlot()).addTempLore(this, "&cThis subject is &4God Locked!", 60);
                return;
            }
            new UtilMenuActionableList("Add Group", 4, (num) -> {
                if(num >= groupList.size()){
                    return null;
                }

                Group g = groupList.get(num);
                return new MenuElement(new ItemBuilder(Material.BOOK, 1)
                .setName("&e" + g.getName())
                .addLore("&fPriority: &a" + g.getPriority())
                .addLore("&fPrefix: " + g.getPrefix())
                .addLore("&fSuffix: " + g.getSuffix()).build())
                        .setClickHandler((e1, i1) -> {
                            handler.addGroup(g, g.getContext());
                            GroupManager.instance.eliminateInheritanceMistakes();
                            groups.clear();
                            contexts.clear();
                            contexts.putAll(handler.getGroups());
                            groups.addAll(contexts.keySet());
                            this.setupPage(this.getCurrentPage());
                            this.open((Player) e1.getWhoClicked());
                        });

            }, Menu.getBackButton(this)).open((Player) e.getWhoClicked());
        }));

        this.setupPage(0);

    }
}

package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.group.Group;
import me.gravitinos.perms.core.group.GroupManager;
import me.gravitinos.perms.core.subject.Inheritance;
import me.gravitinos.perms.core.subject.PPermission;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;

import java.util.ArrayList;

public class PermsVault extends Permission {
    @Override
    public String getName() {
        return SpigotPerms.instance.getName();
    }

    @Override
    public boolean isEnabled() {
        return SpigotPerms.instance.isEnabled();
    }


    @Override
    public boolean hasSuperPermsCompat() {
        return false;
    }

    //s1 -> playername s2 -> permission
    @Override
    public boolean playerHas(String s, String s1, String s2) {
        if(SpigotPerms.getCurrentInstance() == null)
            return false;
        if(Bukkit.getPlayer(s1) != null){
            User user = SpigotPerms.getCurrentInstance().getManager().getUserManager().getUser(Bukkit.getPlayer(s1).getUniqueId());
            if(user == null){
                return false;
            } else {
                return user.hasOwnPermission(s2, new MutableContextSet(Context.CONTEXT_SERVER_LOCAL));
            }
        }
        return false;
    }

    @Override
    public boolean playerAdd(String s, String s1, String s2) {
        if(SpigotPerms.getCurrentInstance() == null)
            return false;
        if(Bukkit.getPlayer(s1) != null){
            User user = SpigotPerms.getCurrentInstance().getManager().getUserManager().getUser(Bukkit.getPlayer(s1).getUniqueId());
            if(user == null){
                return false;
            } else {
                user.addPermission(new PPermission(s2));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean playerRemove(String s, String s1, String s2) {
        if(SpigotPerms.getCurrentInstance() == null)
            return false;
        if(Bukkit.getPlayer(s1) != null){
            User user = SpigotPerms.getCurrentInstance().getManager().getUserManager().getUser(Bukkit.getPlayer(s1).getUniqueId());
            if(user == null){
                return false;
            } else {
                user.removePermission(s2);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean groupHas(String s, String s1, String s2) {
        if(SpigotPerms.getCurrentInstance() == null)
            return false;
        Group group = GroupManager.instance.getVisibleGroup(s1);
        if(group == null){
            return false;
        }
        return group.hasOwnPermission(s2, new MutableContextSet(Context.CONTEXT_SERVER_LOCAL));
    }

    @Override
    public boolean groupAdd(String s, String s1, String s2) {
        if(SpigotPerms.getCurrentInstance() == null)
            return false;
        Group group = GroupManager.instance.getVisibleGroup(s1);
        if(group == null){
            return false;
        }
        group.addPermission(new PPermission(s2));
        return true;
    }

    @Override
    public boolean groupRemove(String s, String s1, String s2) {
        if(SpigotPerms.getCurrentInstance() == null)
            return false;
        Group group = GroupManager.instance.getVisibleGroup(s1);
        if(group == null){
            return false;
        }
        group.removePermission(s2);
        return true;
    }

    @Override
    public boolean playerInGroup(String s, String s1, String s2) {
        if(SpigotPerms.getCurrentInstance() == null)
            return false;
        if(Bukkit.getPlayer(s1) != null){
            User user = SpigotPerms.getCurrentInstance().getManager().getUserManager().getUser(Bukkit.getPlayer(s1).getUniqueId());
            if(user == null){
                return false;
            } else {
                try {
                    for (Inheritance i : user.getInheritances()) {
                        Group groups = (Group) i.getParent();
                        if (groups.getName().equals(s2)) {
                            return true;
                        }
                    }
                } catch(Exception e){
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean playerAddGroup(String s, String s1, String s2) {
        if(SpigotPerms.getCurrentInstance() == null)
            return false;
        if(Bukkit.getPlayer(s1) != null){
            User user = SpigotPerms.getCurrentInstance().getManager().getUserManager().getUser(Bukkit.getPlayer(s1).getUniqueId());
            if(user == null){
                return false;
            } else {
                Group g = GroupManager.instance.getVisibleGroup(s2);
                if(g == null){
                    return false;
                }
                user.addInheritance(g, new MutableContextSet(Context.CONTEXT_SERVER_LOCAL));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean playerRemoveGroup(String s, String s1, String s2) {
        if(SpigotPerms.getCurrentInstance() == null)
            return false;
        if(Bukkit.getPlayer(s1) != null){
            User user = SpigotPerms.getCurrentInstance().getManager().getUserManager().getUser(Bukkit.getPlayer(s1).getUniqueId());
            if(user == null){
                return false;
            } else {
                Group g = GroupManager.instance.getVisibleGroup(s2);
                if(g == null){
                    return false;
                }
                user.removeInheritance(g);
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getPlayerGroups(String s, String s1) {
        if(SpigotPerms.getCurrentInstance() == null)
            return new String[0];
        if(Bukkit.getPlayer(s1) != null){
            User user = SpigotPerms.getCurrentInstance().getManager().getUserManager().getUser(Bukkit.getPlayer(s1).getUniqueId());
            if(user == null){
                return new String[0];
            } else {
                try {
                    ArrayList<String> out = new ArrayList<>();
                    user.getInheritances().forEach(i -> out.add(((Group) i.getParent()).getName()));
                    return out.toArray(new String[0]);
                } catch(Exception e){
                    return new String[0];
                }
            }
        }
        return new String[0];
    }

    @Override
    public String getPrimaryGroup(String s, String s1) {
        if(SpigotPerms.getCurrentInstance() == null)
            return "";

        if(Bukkit.getPlayer(s1) != null){
            User user = SpigotPerms.getCurrentInstance().getManager().getUserManager().getUser(Bukkit.getPlayer(s1).getUniqueId());
            if(user == null){
                return null;
            } else {
                Group g = GroupManager.instance.getGroupExact(user.getDisplayGroup());
                if(g != null)
                    return g.getName();
            }
        }
        return null;
    }

    @Override
    public String[] getGroups() {
        if(SpigotPerms.getCurrentInstance() == null)
            return new String[0];
        ArrayList<String> groups = new ArrayList<>();
        GroupManager.instance.getLoadedGroups().forEach(g -> groups.add(g.getName()));
        return groups.toArray(new String[0]);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }
}

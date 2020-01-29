package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.util.Injector;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.ServerOperator;

import java.util.ArrayList;

public class SpigotPermissible extends PermissibleBase {
    private final Player player;
    public SpigotPermissible(final Player p) {
        super(new ServerOperator() {
            @Override
            public boolean isOp() {
                return p.isOp();
            }

            @Override
            public void setOp(boolean arg0) {
                p.setOp(arg0);
            }

        });
        this.player = p;
    }

    @Override
    public boolean isPermissionSet(String p) {
        return this.hasPermission(p);
    }
    @Override
    public boolean isPermissionSet(Permission p) {
        return this.hasPermission(p);
    }
    @Override
    public boolean hasPermission(String requ) {
        User user = UserManager.instance.getUser(player.getUniqueId());
        if(SpigotPerms.instance.getManager().getImplementation().getConfigSettings().getGodUsers().contains(player.getName())) {
            return true;
        }
        if(user == null){
            return false;
        }

        Context context = new Context(SpigotPerms.instance.getImpl().getConfigSettings().getServerName(), player.getWorld().getName());

        if(player.isOp()) {
            if(user.hasOwnPermission("-op", context)) {
                return false;
            }
            return true;
        }
        ArrayList<String> perms = new ArrayList<>();
        user.getOwnPermissions().forEach(p -> {
            if(p.getContext().applies(context)) { //Check context
                perms.add(p.getPermission());
            }
        });
        if(perms.contains("-" + requ)) {
            return false;
        }
        for (String perm : perms) {
            boolean value = true;
            if (perm.startsWith("-")) {
                perm = perm.substring(1);
                value = false;
            }
            if (perm.equals("*") || perm.equalsIgnoreCase(requ)) {
                return value;
            }
            if (perm.endsWith("*") && requ.startsWith(perm.substring(0, perm.length() - 1))) {
                return value;
            }
        }
        return false;
    }
    @Override
    public boolean hasPermission(Permission requ) {
        User user = UserManager.instance.getUser(player.getUniqueId());
        if(SpigotPerms.instance.getManager().getImplementation().getConfigSettings().getGodUsers().contains(player.getName())) {
            return true;
        }
        if(user == null){
            return false;
        }
        Context context = new Context(SpigotPerms.instance.getImpl().getConfigSettings().getServerName(), player.getWorld().getName());

        if(player.isOp()) {
            if(user.hasOwnPermission("-op", context)) {
                return false;
            }
            return true;
        }
        ArrayList<String> perms = new ArrayList<>();
        user.getOwnPermissions().forEach(p -> {
            if (p.getContext().applies(context)) { //Check context
                perms.add(p.getPermission());
            }
        });
        if(perms.contains("-" + requ.getName())) {
            return false;
        }
        for (String perm : perms) {
            boolean value = true;
            if (perm.startsWith("-")) {
                perm = perm.substring(1);
                value = false;
            }
            if (perm.equals("*") || perm.equalsIgnoreCase(requ.getName())) {
                return value;
            }
            if (perm.endsWith("*") && requ.getName().startsWith(perm.substring(0, perm.length() - 1))) {
                return value;
            }
        }
        return false;
    }

    public static Permissible inject(Player p){
        return Injector.inject(p, new SpigotPermissible(p));
    }
    public static Permissible uninject(Player p){
        return Injector.uninject(p);
    }
}

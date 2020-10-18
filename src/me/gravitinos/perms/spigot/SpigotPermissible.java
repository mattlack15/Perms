package me.gravitinos.perms.spigot;

import me.gravitinos.perms.core.PermsManager;
import me.gravitinos.perms.core.context.Context;
import me.gravitinos.perms.core.context.ContextSet;
import me.gravitinos.perms.core.context.MutableContextSet;
import me.gravitinos.perms.core.group.GroupData;
import me.gravitinos.perms.core.user.User;
import me.gravitinos.perms.core.user.UserManager;
import me.gravitinos.perms.spigot.util.Injector;
import me.gravitinos.perms.spigot.verbose.VerboseController;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
        long nano = System.nanoTime();

        try {

            //Verbose
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            if (stackTraceElements.length > 1) {
                VerboseController.instance.handlePermissionCheck(player, stackTraceElements[3], requ);
            }

            //Permission Index
            PermsManager.instance.getImplementation().getAsyncExecutor().execute(() -> SpigotPerms.instance.addPermissionToIndex(requ));

            User user = UserManager.instance.getUser(player.getUniqueId());

            if(PermsManager.instance.getGodUsers().contains(player.getName())) {
                return true;
            }
            if (user == null) {
                return false;
            }

            ContextSet context = new MutableContextSet(Context.CONTEXT_SERVER_LOCAL, new Context(Context.WORLD_IDENTIFIER, player.getWorld().getName()));

            ArrayList<String> perms = new ArrayList<>();
            user.getAllPermissions(context).forEach(p -> perms.add(p.getPermission()));

            if (perms.contains("-" + requ)) {
                return false;
            }
            boolean returnValue = false;
            for (String perm : perms) {
                boolean value = true;
                while (perm.startsWith("-")) {
                    perm = perm.substring(1);
                    value = !value;
                }
                if(perm.length() == 0) {
                    continue;
                }
                if (perm.equals("*") || perm.equalsIgnoreCase(requ)) {
                    if(!value)
                        return false;
                    returnValue = true;
                    continue;
                }
                if (perm.endsWith("*") && requ.startsWith(perm.substring(0, perm.length() - 1))) {
                    if(!value)
                        return false;
                    returnValue = true;
                }
            }
            return returnValue;
        } finally {
            nano = System.nanoTime() - nano;
        }
    }
    @Override
    public boolean hasPermission(Permission requ) {
        return this.hasPermission(requ.getName());
    }

    public static Permissible inject(Player p){
        return Injector.inject(p, new SpigotPermissible(p));
    }
    public static Permissible uninject(Player p){
        return Injector.uninject(p);
    }
}

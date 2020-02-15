package me.gravitinos.perms.spigot.util;

import java.lang.reflect.Method;
import java.util.*;

import me.gravitinos.perms.spigot.SpigotPerms;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import me.gravitinos.perms.spigot.util.ReflectionUtils2.PackageType;
import org.jetbrains.annotations.NotNull;

/**
     * Create a "Virtual" book gui that doesn't require the user to have a book in their hand.
     * Requires ReflectionUtil class.
     * Built for Minecraft 1.9
     * @author Jed
     *
     */
    public class UtilBookInput implements Listener {
        private static boolean initialised = false;
        private static Method getHandle;
        private static Method openBook;

        public static interface BookInputHandler{
            void handle(BookMeta bookMeta);
        }

        private static Map<UUID, BookInputHandler> handlers = new HashMap<>();
        private static Map<UUID, ItemStack> mainHand = new HashMap<>();

        static {
            try {
                getHandle = ReflectionUtils2.getMethod("CraftPlayer", PackageType.CRAFTBUKKIT_ENTITY, "getHandle");
                openBook = ReflectionUtils2.getMethod("EntityPlayer", PackageType.MINECRAFT_SERVER, "a", PackageType.MINECRAFT_SERVER.getClass("ItemStack"), PackageType.MINECRAFT_SERVER.getClass("EnumHand"));
                initialised = true;
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
                Bukkit.getServer().getLogger().warning("Cannot force open book!");
                initialised = false;
            }
            new UtilBookInput();
        }

        private UtilBookInput(){
            Bukkit.getPluginManager().registerEvents(this, SpigotPerms.instance);
        }

        public static void getBookInput(@NotNull Player p, @NotNull ItemStack book, @NotNull BookInputHandler handler){

            handlers.put(p.getUniqueId(), handler);
            mainHand.put(p.getUniqueId(), p.getItemInHand());

            p.setItemInHand(book);
        }

        @EventHandler
        public void onBookEdit(PlayerEditBookEvent event){
            if(handlers.containsKey(event.getPlayer().getUniqueId())){
                handlers.get(event.getPlayer().getUniqueId()).handle(event.getNewBookMeta());
                event.getPlayer().setItemInHand(mainHand.get(event.getPlayer().getUniqueId()));
                mainHand.remove(event.getPlayer().getUniqueId());
                handlers.remove(event.getPlayer().getUniqueId());
            }
        }

        //--------------------------------------------------------------------------------------------------------------

        public static boolean isInitialised(){
            return initialised;
        }
        /**
         * Open a "Virtual" Book ItemStack.
         * @param i Book ItemStack.
         * @param p Player that will open the book.
         * @return
         */
        public static boolean openBook(ItemStack i, Player p) {
            if (!initialised) return false;
            ItemStack held = p.getInventory().getItemInMainHand();
            try {
                p.getInventory().setItemInMainHand(i);
                sendPacket(i, p);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
                initialised = false;
            }
            p.getInventory().setItemInMainHand(held);
            return initialised;
        }

        private static void sendPacket(ItemStack i, Player p) throws ReflectiveOperationException {
            Object entityplayer = getHandle.invoke(p);
            Class<?> enumHand = PackageType.MINECRAFT_SERVER.getClass("EnumHand");
            Object[] enumArray = enumHand.getEnumConstants();
            openBook.invoke(entityplayer, getItemStack(i), enumArray[0]);
        }

        public static Object getItemStack(ItemStack item) {
            try {
                Method asNMSCopy = ReflectionUtils2.getMethod(PackageType.CRAFTBUKKIT_INVENTORY.getClass("CraftItemStack"), "asNMSCopy", ItemStack.class);
                return asNMSCopy.invoke(PackageType.CRAFTBUKKIT_INVENTORY.getClass("CraftItemStack"), item);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Set the pages of the book in JSON format.
         * @param metadata BookMeta of the Book ItemStack.
         * @param pages Each page to be added to the book.
         */
        @SuppressWarnings("unchecked")
        public static void setPages(BookMeta metadata, List<String> pages) {
            List<Object> p;
            Object page;
            try {
                p = (List<Object>) ReflectionUtils2.getField(PackageType.CRAFTBUKKIT_INVENTORY.getClass("CraftMetaBook"), true, "pages").get(metadata);
                for (String text : pages) {
                    page = ReflectionUtils2.invokeMethod(ReflectionUtils2.PackageType.MINECRAFT_SERVER.getClass("IChatBaseComponent$ChatSerializer").newInstance(), "a", text);
                    p.add(page);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}

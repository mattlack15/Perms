package me.gravitinos.perms.spigot.util.Menus;

import org.bukkit.inventory.Inventory;

import java.util.ArrayList;

public class InvInfo {
    private Inventory currentInv;
    private Menu currentMenu;
    private ArrayList<Object> data;

    public InvInfo(Inventory cInv, Menu cMenu){
        this.data = new ArrayList<>();
        this.currentMenu = cMenu;
        this.currentInv = cInv;
    }
    public InvInfo(Inventory cInv, Menu cMenu, ArrayList<Object> data){
        this.currentInv = cInv;
        this.currentMenu = cMenu;
        this.data = data;
    }

    public Menu getCurrentMenu(){
        return this.currentMenu;
    }

    public void setCurrentMenu(Menu currentMenu) {
        this.currentMenu = currentMenu;
    }

    public ArrayList<Object> getData() {
        return data;
    }

    public void setCurrentInv(Inventory currentInv) {
        this.currentInv = currentInv;
    }
    public Inventory getCurrentInv(){
        return this.currentInv;
    }


}

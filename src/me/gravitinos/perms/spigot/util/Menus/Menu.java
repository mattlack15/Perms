package me.gravitinos.perms.spigot.util.Menus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class Menu {
    //Value vars
    private String title;

    private MenuElement.ClickHandler defaultClickHandler = null;

    //Storage vars
    private Map<Integer, MenuElement> elements = new HashMap<>();

    //Construction
    public Menu(String title, int size){
        this.title = title;
        this.setSize(size);
    }

    //private
    private Inventory buildInventory(){
        Inventory inv = Bukkit.createInventory(null, this.getSize(), title);
        for(int i = 0; i < this.getSize(); i++){
            MenuElement e = this.getElement(i);
            if(e == null){
                continue;
            }
            inv.setItem(i, e.getItem());
        }
        return inv;
    }

    public void setDefaultClickHandler(MenuElement.ClickHandler handler){
        this.defaultClickHandler = handler;
    }

    public MenuElement.ClickHandler getDefaultClickHandler() {
        return defaultClickHandler;
    }

    //open
    public void open(Player p, ArrayList<Object> data){
        Inventory inv = this.buildInventory();
        InvInfo info = new InvInfo(inv, this, data);
        MenuManager.instance.setInfo(p.getUniqueId(), info);
        p.openInventory(inv);
    }
    public void open(Player p){
        this.open(p, new ArrayList<>());
    }

    //Util
    public int getSize(){
        return this.elements.size();
    }
    public void setSize(int size){
        if(elements.size() > size){
            for(int i = elements.size()-1; i != size-1; i--){ // for -1 change if doesn't work but you did this briefly in your head (approx. 3m of thinking)
                elements.remove(i);
            }
        } else if(elements.size() < size){
            for(int i = elements.size(); i != size; i++){
                elements.put(i, null);
            }
        }
        if(size != this.elements.size()){
            System.out.println("Problem making size adjustment in Menus -> Menu -> setSize(int size)");
        }
    }

    //Element management

    public void setElement(int position, MenuElement e){
        if(position >= this.elements.size()){
            return;
        }
        this.elements.put(position, e);
    }

    public MenuElement getElement(int slot){
        return this.elements.get(slot);
    }

    public void fillElement(MenuElement e){
        for(int i = 0; i < elements.size(); i++){
            if(this.getElement(i) == null){
                this.setElement(i, e);
            }
        }
    }

    public void setRow(int row, MenuElement e){

        if(row * 9 + 9 > this.elements.size()){
            return;
        }

        int startingPos = row * 9;

        for(int i = startingPos; i < startingPos + 9; i++){
            this.setElement(i, e);
        }

    }

    public void setAll(MenuElement e){
        for(int i = 0; i < elements.size(); i++){
            this.setElement(i, e);
        }
    }

    public Map<Integer, MenuElement> getElements() {
        return elements;
    }
}

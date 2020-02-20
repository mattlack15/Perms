package me.gravitinos.perms.spigot.gui;

import me.gravitinos.perms.spigot.util.ItemBuilder;
import me.gravitinos.perms.spigot.util.Menus.Menu;
import me.gravitinos.perms.spigot.util.Menus.MenuElement;
import me.gravitinos.perms.spigot.util.Menus.MenuManager;
import org.jetbrains.annotations.NotNull;

public class UtilMenuActionableList extends Menu {
    public interface MenuElementSupplier {
        MenuElement getElement(int index);
    }

    private MenuElementSupplier supplier;
    private MenuElement backButton;
    private MenuElement filler;
    private int margin = 0;

    private int listRows = 0;

    private int currentPage = 0;

    /**
     * This constructor will automatically setup page 0
     * @param title Title
     * @param listRows Number of list rows
     * @param supplier Element supplier
     * @param backButton Back button
     */
    public UtilMenuActionableList(String title, int listRows, @NotNull MenuElementSupplier supplier, MenuElement backButton) {
        this(title, listRows);
        this.listRows = listRows;
        this.supplier = supplier;
        this.backButton = backButton;
        this.setupPage(this.currentPage);
    }

    public UtilMenuActionableList(String title, int listRows, @NotNull MenuElementSupplier supplier, @NotNull Menu backMenu) {
        this(title, listRows, supplier, Menu.getBackButton(backMenu));
    }

    public UtilMenuActionableList(String title, int listRows){
        super(title, listRows + 2);
        this.listRows = listRows;
    }

    public UtilMenuActionableList setBackButton(MenuElement element) {
        this.backButton = element;
        return this;
    }

    public UtilMenuActionableList setElementSupplier(@NotNull MenuElementSupplier supplier){
        this.supplier = supplier;
        return this;
    }

    public UtilMenuActionableList setBackButton(@NotNull Menu backMenu) {
        this.backButton = Menu.getBackButton(backMenu);
        return this;
    }

    public MenuElement getBackButton(){
        return this.backButton;
    }

    public MenuElement getFiller() {
        return filler;
    }

    public MenuElementSupplier getSupplier() {
        return supplier;
    }

    public UtilMenuActionableList setFiller(MenuElement e){
        this.filler = e;
        return this;
    }

    public UtilMenuActionableList setMargin(int margin){
        this.margin = margin;
        return this;
    }
    public int getMargin(){
        return this.margin;
    }

    public int getListRows(){
        return this.listRows;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public UtilMenuActionableList setupPage(int page){
        return this.setupPage(page, false);
    }

    /**
     * Sets up this list on a specific page
     *
     * @param page The page to setup
     * @param clearNonListElements Whether to set all non-list slots to null
     */
    public UtilMenuActionableList setupPage(int page, boolean clearNonListElements) {
        int elementRows = this.getRows() - 2;
        int elementCols = 9 - margin*2;
        int pageBuffer = page * elementRows * elementCols;
        int startPos = 9 + margin;
        int endPos = (elementRows + 1) * 9 - 1 - margin;
        int elementNum = 0;
        if(clearNonListElements) {
            this.setAll(null);
        }
        for (int pos = startPos; pos <= endPos; pos++) {
            int col = this.getColumn(pos);
            if(col < margin || col > 8-margin) { //8 because its 9-margin-1 so that simplifies to 8-margin
                continue;
            }
            this.setElement(pos, supplier.getElement(elementNum + pageBuffer));
            elementNum++;
        }

        //Back button
        this.setElement(4, backButton);

        //Prev page
        if (page > 0) {
            this.setElement(endPos + 1, new MenuElement(new ItemBuilder(Menu.PAGE_CONTROL_ITEM, 1).setName("&f&lPrevious Page").build()).setStaticItem(true)
                    .setClickHandler((e, i) -> this.setupPage(page - 1)));
        } else {
            this.setElement(endPos + 1, null);
        }

        //Next page
        if (supplier.getElement(endPos + pageBuffer + 1) != null && supplier.getElement(endPos + pageBuffer + 1).getItem() != null) {
            this.setElement(endPos + 9, new MenuElement(new ItemBuilder(Menu.PAGE_CONTROL_ITEM, 1).setName("&f&lNext Page").build()).setStaticItem(true)
                    .setClickHandler((e, i) -> this.setupPage(page + 1)));
        } else {
            this.setElement(endPos + 9, null);
        }

        this.fillElement(filler);

        this.currentPage = page;

        MenuManager.instance.invalidateInvsForMenu(this);
        return this;
    }

    //Util
    private int getColumn(int pos){
        return pos % 9;
    }
}

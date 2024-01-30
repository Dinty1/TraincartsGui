package com.dnamaster10.tcgui.objects;

import com.dnamaster10.tcgui.util.gui.GuiBuilder;
import com.dnamaster10.tcgui.util.database.LinkerAccessor;
import com.dnamaster10.tcgui.util.database.databaseobjects.LinkerDatabaseObject;
import com.dnamaster10.tcgui.util.database.databaseobjects.TicketDatabaseObject;
import com.dnamaster10.tcgui.util.database.GuiAccessor;
import com.dnamaster10.tcgui.util.database.TicketAccessor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EditGui extends Gui {
    @Override
    public void open() {
        //Method must be run synchronous
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try {
                generate();
            } catch (SQLException e) {
                removeCursorItemAndClose();
                getPlugin().reportSqlError(getPlayer(), e.toString());
            }
            Bukkit.getScheduler().runTask(getPlugin(), () -> getPlayer().openInventory(getInventory()));
        });
    }

    @Override
    protected void generate() throws SQLException {
        GuiBuilder builder = new GuiBuilder(getGuiName(), getPage(), getDisplayName());
        builder.addTickets();
        builder.addLinkers();
        if (getPage() > 0) {
            builder.addPrevPageButton();
        }
        builder.addNextPageButton();
        setInventory(builder.getInventory());
    }

    @Override
    public void nextPage() {
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            //Save the current page
            save();

            //Increment the current page
            setPage(getPage() + 1);
            removeCursorItem();
            open();
        });
    }

    @Override
    public void prevPage() {
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            //Save the current page
            save();

            //Set the new page
            if (getPage() > 0) {
                setPage(getPage() - 1);
            }
            removeCursorItem();
            open();
        });
    }

    @Override
    public void handleClick(InventoryClickEvent event, List<ItemStack> items) {
        //Check if clicked item is a page button
        for (ItemStack item : items) {
            String buttonType = getButtonType(item);
            if (buttonType == null) {
                continue;
            }
            switch (buttonType) {
                case "next_page" -> {
                    nextPage();
                    return;
                }
                case "prev_page" -> {
                    prevPage();
                    return;
                }
            }
        }
    }

    public void save() {
        //Method must be run asynchronously
        //Saves the current gui page
        //Build an arraylist of ticket and linker database objects to be put in the database

        List<TicketDatabaseObject> ticketList = new ArrayList<>();
        NamespacedKey tcKey = new NamespacedKey(getPlugin(), "tc_name");
        NamespacedKey priceKey = new NamespacedKey(getPlugin(), "price");

        List<LinkerDatabaseObject> linkerList = new ArrayList<>();
        NamespacedKey guiKey = new NamespacedKey(getPlugin(), "gui");

        for (int i = 0; i < getInventory().getSize() - 9; i++) {
            //For every possible ticket slot get the item in that slot
            ItemStack item = getInventory().getItem(i);
            if (item == null) {
                //If there isn't an item, continue to the next slot
                continue;
            }
            //Check if item is a ticket or linker
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            if (meta.getPersistentDataContainer().has(tcKey, PersistentDataType.STRING)) {
                //Item is a ticket, handle ticket save
                String tcName = meta.getPersistentDataContainer().get(tcKey, PersistentDataType.STRING);
                String displayName = meta.getDisplayName();

                //Check if display name is too long
                if (displayName.length() > 25) {
                    continue;
                }

                //Check if price is set, if not, set price to 0
                int price = 0;
                if (meta.getPersistentDataContainer().has(priceKey, PersistentDataType.INTEGER)) {
                    price = meta.getPersistentDataContainer().get(priceKey, PersistentDataType.INTEGER);
                }

                TicketDatabaseObject ticket = new TicketDatabaseObject(i, tcName, displayName, ChatColor.stripColor(displayName), price);
                ticketList.add(ticket);
            }
            else if (meta.getPersistentDataContainer().has(guiKey, PersistentDataType.INTEGER)) {
                //Item is a linker, handler linker save
                int linkedGuiId = meta.getPersistentDataContainer().get(guiKey, PersistentDataType.INTEGER);
                String displayName = meta.getDisplayName();

                //Check if display name is too long
                if (displayName.length() > 25) {
                    continue;
                }

                LinkerDatabaseObject linker = new LinkerDatabaseObject(i, linkedGuiId, displayName, ChatColor.stripColor(displayName));
                linkerList.add(linker);
            }
            //Otherwise, item is not a savable / tcgui item. Ignore it to remove it
        }
        //With an array list of tickets and linkers, we can save the data to the database
        //First, delete all existing tickets and linkers for this gui and page
        try {
            TicketAccessor ticketAccessor = new TicketAccessor();
            GuiAccessor guiAccessor = new GuiAccessor();
            LinkerAccessor linkerAccessor = new LinkerAccessor();

            ticketAccessor.deleteTicketsByGuiIdPageId(getGuiId(), getPage());
            linkerAccessor.deleteLinkersByGuiIdPageId(getGuiId(), getPage());

            //Add the tickets to the database
            ticketAccessor.addTickets(getGuiId(), getPage(), ticketList);

            //Add the linkers to the database
            linkerAccessor.addLinkers(getGuiId(), getPage(), linkerList);
        } catch (SQLException e) {
            removeCursorItemAndClose();
            getPlugin().reportSqlError(e.toString());
        }
    }

    public EditGui(String guiName, int page, Player p) throws SQLException {
        //Should be called from an asynchronous thread
        GuiAccessor guiAccessor = new GuiAccessor();
        String displayName = "Editing: " + guiAccessor.getColouredGuiDisplayName(guiName);
        int guiId = guiAccessor.getGuiIdByName(guiName);

        setGuiName(guiName);
        setDisplayName(displayName);
        setGuiId(guiId);
        setPage(page);
        setPlayer(p);
    }
    public EditGui(String guiName, Player p) throws SQLException {
        this(guiName, 0, p);
    }
}
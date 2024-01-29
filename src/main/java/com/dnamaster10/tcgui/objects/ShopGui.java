package com.dnamaster10.tcgui.objects;

import com.dnamaster10.tcgui.util.gui.GuiBuilder;
import com.dnamaster10.tcgui.util.Traincarts;
import com.dnamaster10.tcgui.util.database.GuiAccessor;
import com.dnamaster10.tcgui.util.gui.LastGui;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ShopGui extends Gui {
    @Override
    public void open() {
        //Method should be run synchronous
        if (Bukkit.isPrimaryThread()) {
            getPlayer().openInventory(getInventory());
            return;
        }
        Bukkit.getScheduler().runTask(getPlugin(), () -> getPlayer().openInventory(getInventory()));
    }

    @Override
    public void nextPage() {
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try {
                //Check if any other pages exist above this one
                GuiAccessor guiAccessor = new GuiAccessor();
                int guiId = guiAccessor.getGuiIdByName(getGuiName());
                int maxPage = guiAccessor.getTotalPages(guiId);
                if (getPage() + 1 > maxPage) {
                    removeCursorItem();
                    return;
                }
                //Increment page
                setPage(getPage() + 1);

                //Build new inventory
                generateGui();
                removeCursorItem();
            } catch (SQLException e) {
                getPlayer().closeInventory();
                getPlugin().reportSqlError(getPlayer(), e.toString());
            }
        });
    }

    @Override
    public void prevPage() {
        //Check that any pages exist before this oen
        if (getPage() - 1 < 0) {
            setPage(0);
            removeCursorItem();
            return;
        }
        setPage(getPage() - 1);
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try {
                //Build the new page
                generateGui();
                removeCursorItem();
            } catch (SQLException e) {
                getPlayer().closeInventory();
                getPlugin().reportSqlError(getPlayer(), e.toString());
            }
        });
    }
    public void handleLink(ItemStack button) {
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            //First get the destination page
            ItemMeta meta = button.getItemMeta();
            assert meta!= null;
            NamespacedKey key = new NamespacedKey(getPlugin(), "gui");
            int linkedGuiId = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

            String destGuiName = null;
            //Check the destination gui exists and get the gui name
            try {
                GuiAccessor guiAccessor = new GuiAccessor();
                if (!guiAccessor.checkGuiById(linkedGuiId)) {
                    removeCursorItem();
                    return;
                }
                destGuiName = guiAccessor.getGuiNameById(linkedGuiId);
            } catch (SQLException e) {
                getPlugin().reportSqlError(getPlayer(), e.toString());
            }

            //Add the current gui info to the previous gui stack
            getPlugin().getGuiManager().addPrevGui(getGuiName(), getPage(), getPlayer());

            //Now change the current gui to the new gui
            setPage(0);
            setGuiName(destGuiName);

            try {
                generateGui();
                removeCursorItem();
            }
            catch (SQLException e) {
                getPlugin().reportSqlError(getPlayer(), e.toString());
            }
        });
    }
    private void back() {
        //Check that there is a previous gui that the player was on
        if (!getPlugin().getGuiManager().checkPrevGui(getPlayer())) {
            //If not, remove the button from their cursor
            removeCursorItem();
            return;
        }
        //If there is a previous gui, get the name of the last gui
        LastGui lastGui = getPlugin().getGuiManager().getPrevGui(getPlayer());
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            try {
                //Check that the gui exists
                GuiAccessor guiAccessor = new GuiAccessor();
                if (!guiAccessor.checkGuiByName(lastGui.getLastGuiName())) {
                    getPlayer().sendMessage(ChatColor.RED + "Previous gui does not exist");
                    removeCursorItem();
                    return;
                }
                //Remove the item from cursor
                removeCursorItem();

                //Next, we need to generate the new gui.
                setGuiName(lastGui.getLastGuiName());
                setPage(lastGui.getLastPageNum());

                generateGui();
            }
            catch (SQLException e) {
                getPlayer().closeInventory();
                getPlugin().reportSqlError(getPlayer(), e.toString());
            }
        });
    }
    private void search() {
        getPlayer().sendMessage(ChatColor.AQUA + "|");
        TextComponent message = new TextComponent(ChatColor.AQUA + "| >>>Click me to search!<<<");
        message.setBold(true);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tcgui gui search " + getGuiName() + " "));
        getPlayer().spigot().sendMessage(message);
        getPlayer().sendMessage(ChatColor.AQUA + "|");
        removeCursorItemAndClose();
    }
    private void handleTicketClick(ItemStack ticket) {
        //Get ticket tc name
        NamespacedKey key = new NamespacedKey(getPlugin(), "tc_name");
        String tcName = Objects.requireNonNull(ticket.getItemMeta()).getPersistentDataContainer().get(key, PersistentDataType.STRING);

        //Check that the tc ticket exists
        if (!Traincarts.checkTicket(tcName)) {
            removeCursorItem();
            return;
        }
        //Give the ticket to the player
        removeCursorItem();
        Traincarts.giveTicketItem(tcName, 0, getPlayer());
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {}, 1L);
        getPlayer().closeInventory();
    }

    @Override
    public void handleClick(InventoryClickEvent event, List<ItemStack> items) {
        //Check if player interacted with a button
        for (ItemStack item : items) {
            String buttonType = getButtonType(item);
            if (buttonType == null) {
                continue;
            }
            switch (buttonType) {
                case "ticket" -> {
                    handleTicketClick(item);
                    return;
                }
                case "prev_page" -> {
                    prevPage();
                    return;
                }
                case "next_page" -> {
                    nextPage();
                    return;
                }
                case "linker" -> {
                    handleLink(item);
                    return;
                }
                case "back" -> {
                    back();
                    return;
                }
                case "search" -> {
                    search();
                    return;
                }
            }
        }
    }
    private void generateGui() throws SQLException {
        //Adds tickets, buttons etc based on gui name

        //Build tickets
        GuiBuilder builder = new GuiBuilder(getGuiName(), getPage());
        builder.addTickets();
        builder.addLinkers();

        //Check if there are any more pages
        GuiAccessor accessor = new GuiAccessor();
        int guiId = accessor.getGuiIdByName(getGuiName());

        if (accessor.getTotalPages(guiId) > getPage()) {
            builder.addNextPageButton();
        }
        if (getPage() > 0) {
            builder.addPrevPageButton();
        }

        //Check if back button is needed
        if (getPlugin().getGuiManager().checkPrevGui(getPlayer())) {
            builder.addBackButton();
        }

        builder.addSearchButton();
        updateNewInventory(builder.getInventory());
    }

    public ShopGui(String guiName, Player p, int page) throws SQLException {
        //Should be called from async thread
        //Instantiate gui
        GuiAccessor guiAccessor = new GuiAccessor();
        String displayName = guiAccessor.getColouredGuiDisplayName(guiName);
        setInventory(Bukkit.getServer().createInventory(p, 54, ChatColor.translateAlternateColorCodes('&', displayName)));

        //Set the owner
        setPlayer(p);

        //Set page
        setPage(page);

        //Set the gui
        setGuiName(guiName);
        generateGui();
    }
    public ShopGui(String guiName, Player p) throws SQLException {
        this(guiName, p, 0);
    }
}

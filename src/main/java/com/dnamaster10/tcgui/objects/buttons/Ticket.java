package com.dnamaster10.tcgui.objects.buttons;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static com.dnamaster10.tcgui.TraincartsGui.getPlugin;
import static com.dnamaster10.tcgui.objects.buttons.DataKeys.*;

public class Ticket extends Button {
    public void giveToPlayer(Player p) {
        p.getInventory().addItem(item);
    }
    public Ticket(String tcName, String displayName, int price) {

        //Create item
        item = new ItemStack(Material.PAPER, 1);

        //Set data
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(displayName);

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        dataContainer.set(BUTTON_TYPE, PersistentDataType.STRING, "ticket");
        dataContainer.set(TC_TICKET_NAME, PersistentDataType.STRING, tcName);
        dataContainer.set(TICKET_PRICE, PersistentDataType.INTEGER, price);

        item.setItemMeta(meta);
    }
}

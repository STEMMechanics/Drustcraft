package com.stemcraft.listeners;

import com.stemcraft.SMItem;
import com.stemcraft.SMListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class SMInventoryListener extends SMListener {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (inventory instanceof PlayerInventory) {
            return;
        }

        // We do this here and not on an inventory click/move event as they
        // are not captured for bedrock clients
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);

            if (item != null && SMItem.destroyOnDrop(item)) {
                inventory.setItem(i, null);
            }
        }
    }
}

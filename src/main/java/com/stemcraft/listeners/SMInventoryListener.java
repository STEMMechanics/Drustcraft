package com.stemcraft.listeners;

import com.stemcraft.SMItem;
import com.stemcraft.SMListener;
import com.stemcraft.STEMCraft;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class SMInventoryListener extends SMListener {
    private final NamespacedKey repairCostKey = new NamespacedKey(STEMCraft.getPlugin(), "RepairCost");

    /**
     * When the player clicks an inventory
     * @param event The event
     */
    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory inventory = event.getInventory();

        /*
            remove the 'RepairCost' NBT from an item to reset its
            repair cost
         */
        if(inventory instanceof AnvilInventory) {
            ItemStack item = event.getCurrentItem();
            if(item != null && !item.getType().isAir()) {
                ItemMeta meta = item.getItemMeta();
                if(meta != null) {
                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    if(container.has(repairCostKey, PersistentDataType.INTEGER)) {
                        container.remove(repairCostKey);
                        item.setItemMeta(meta);
                        event.setCurrentItem(item);
                    }
                }
            }
        }
    }

    /**
     * When the player closes an inventory
     * @param event The event
     */
    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        /*
            remove itemstacks from the non-player inventory if they
            were transferred by the player and the item contains
            the 'destroy-on-drop' flag. This is to capture bedrock
            players as they do not support the inventory click/move
            event
         */
        if (!(inventory instanceof PlayerInventory)) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);

                if (item != null && SMItem.destroyOnDrop(item)) {
                    inventory.setItem(i, null);
                }
            }
        }
    }
}

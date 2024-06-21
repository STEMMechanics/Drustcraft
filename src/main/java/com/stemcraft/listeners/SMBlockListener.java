package com.stemcraft.listeners;

import com.stemcraft.SMListener;
import com.stemcraft.SMPlayer;
import com.stemcraft.STEMCraft;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class SMBlockListener extends SMListener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        final ItemStack is = event.getItemInHand();

        final SMPlayer player = STEMCraft.getPlayer(event.getPlayer());
//        if(player.hasUnlimited(is) && player.getBase().getGameMode() == GameMode.SURVIVAL) {
//            STEMCraft.runLater(() -> {
//                if (is != null && is.getType() != null && !SMUtilsMaterial.isAir(is.getType())) {
//                    final ItemStack cloneIs = is.clone();
//                    cloneIs.setAmount(1);
//                    Inventories.addItem(user.getBase(), cloneIs);
//                    user.getBase().updateInventory();
//                }
//            });
//        }
    }
}

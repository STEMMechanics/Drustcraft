package com.stemcraft.listeners;

import com.stemcraft.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SMBlockListener extends SMListener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        List<SMRegion> regions = SMRegion.findRegions(event.getBlock().getLocation());
        for(SMRegion region : regions) {
            if(region.handleBlockPlace(event.getBlock(), event.getPlayer())) {
                event.setCancelled(true);
                return;
            }
        }



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

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        List<SMRegion> regions = SMRegion.findRegions(event.getBlock().getLocation());
        for(SMRegion region : regions) {
            if(region.handleBlockBreak(event.getBlock(), event.getPlayer())) {
                event.setCancelled(true);
                return;
            }
        }


        final Block block = event.getBlock();

        if(block.getType() == Material.LODESTONE || block.getType() == Material.END_PORTAL_FRAME) {
//                SMConfig.getStringList("waystones.worlds").contains(block.getWorld().getName())) {

        }
    }
}

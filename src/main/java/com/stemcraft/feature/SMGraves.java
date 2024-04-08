package com.stemcraft.feature;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import com.stemcraft.STEMCraft;
import com.stemcraft.core.SMCommon;
import com.stemcraft.core.SMDatabase;
import com.stemcraft.core.SMFeature;
import com.stemcraft.core.config.SMConfig;
import com.stemcraft.core.event.SMEvent;
import org.bukkit.inventory.ItemStack;

public class SMGraves extends SMFeature {
    private final List<World> worlds = new ArrayList<>();

    @Override
    protected Boolean onEnable() {
        SMDatabase.runMigration("240113000000_RemoveGravestoneTable", () -> {
            SMDatabase.prepareStatement(
                            "DROP TABLE IF EXISTS graves")
                    .executeUpdate();
        });

        List<String> worldsList = SMConfig.main().getStringList("graves.worlds");
        worldsList.forEach(worldName -> {
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                STEMCraft.info("Adding world " + worldName + " to the list of worlds for graves");
                worlds.add(world);
            }
        });

        SMEvent.register(PlayerDeathEvent.class, ctx -> {
            if (ctx.event.getEventName().equalsIgnoreCase("playerdeathevent")) {
                Player player = ctx.event.getEntity();

                if (player.getGameMode() != GameMode.SURVIVAL || !worlds.contains(player.getLocation().getWorld())) {
                    STEMCraft.info("Grave skipped as player " + player.getName() + " is not in survival game mode (" + player.getGameMode() + "), or is not in the list of worlds for graves (" + player.getLocation().getWorld().getName() + ")");
                    return;
                }

                Location signLocation = SMCommon.findSafeLocation(player.getLocation(), 16);
                if (signLocation == null) {
                    STEMCraft.info("No suitable location was found near the player for a grave sign");
                    return;
                }

                Block block = signLocation.getBlock();
                block.setType(Material.OAK_SIGN);
                Sign signState = (Sign) block.getState();
                org.bukkit.block.data.type.Sign signData = (org.bukkit.block.data.type.Sign) signState.getBlockData();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm a");
                LocalDateTime now = LocalDateTime.now();
                String formattedDateTime = now.format(formatter);

                signState.setLine(0, player.getName());
                signState.setLine(1, "----");
                signState.setLine(2, "died here on");
                signState.setLine(3, formattedDateTime);

                // Calculate yaw to make the sign face the player's location
                Location playerLocation = player.getLocation();

                // Check if signLocation is the same block as playerLocation
                if (playerLocation.getBlock().getLocation().equals(signLocation.getBlock().getLocation())) {
                    // The player and sign are in the same block, set the sign rotation to match the player
                    BlockFace blockFace = SMCommon.getClosestBlockFace(playerLocation.getYaw(), true);
                    signData.setRotation(blockFace);
                } else {
                    // The player and sign are in different blocks, calculate the yaw and set the rotation accordingly
                    double deltaX = playerLocation.getX() - signLocation.getX();
                    double deltaZ = playerLocation.getZ() - signLocation.getZ();
                    double yaw = Math.atan2(deltaZ, deltaX) * (180 / Math.PI) - 90;

                    // Calculate the closest BlockFace
                    BlockFace blockFace = SMCommon.getClosestBlockFace(yaw, true);

                    // Check the blocks around the sign and set the rotation to face the first air block found
                    BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
                    for (BlockFace face : faces) {
                        if (block.getRelative(face).getType() == Material.AIR) {
                            blockFace = face;
                            break;
                        }
                    }

                    // Set the sign rotation using BlockFace
                    signData.setRotation(blockFace);
                }

                // Update the sign state
                signState.setBlockData(signData);
                signState.update();

                List<ItemStack> drops = new ArrayList<>(ctx.event.getDrops());
                Iterator<ItemStack> iterator = drops.iterator();
                while (iterator.hasNext()) {
                    Location graveLocation = SMCommon.findSafeLocation(signLocation, 64);
                    if (graveLocation == null) {
                        STEMCraft.info("No suitable location was found near the player for a grave chest");
                        break;
                    }

                    Block barrelBlock = graveLocation.getBlock();
                    barrelBlock.setType(Material.BARREL);
                    Barrel barrelState = (Barrel) barrelBlock.getState();

                    Directional directional = (Directional) barrelState.getBlockData();
                    directional.setFacing(BlockFace.UP);
                    barrelState.setBlockData(directional);
                    barrelState.update(true); // Update the block state to reflect changes

                    Inventory barrelInventory = barrelState.getInventory();

                    while (iterator.hasNext()) {
                        ItemStack item = iterator.next();
                        HashMap<Integer, ItemStack> notStored = barrelInventory.addItem(item);
                        if (notStored.isEmpty()) {
                            iterator.remove();
                        } else {
                            break;
                        }
                    }

                    STEMCraft.info("Placed grave chest at " + graveLocation + " for " + player.getName() + " with " + barrelInventory.getSize() + " items");
                }

                ctx.event.getDrops().clear();
                if(!drops.isEmpty()) {
                    ctx.event.getDrops().addAll(drops);
                }
            }
        });

        return true;
    }
}

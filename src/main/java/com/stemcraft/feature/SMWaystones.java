package com.stemcraft.feature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import com.stemcraft.core.*;
import com.stemcraft.core.config.SMConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import com.stemcraft.STEMCraft;
import com.stemcraft.core.event.SMEvent;

public class SMWaystones extends SMFeature {

    private int maxDistance = 1000;
    private List<String> waystoneTypes = new ArrayList<>();
    private final List<World> worlds = new ArrayList<>();
    private final Map<Location, String> waystoneCache = new HashMap<>();
    private final Map<Location, Location> teleportCache = new HashMap<>();

    @Override
    protected Boolean onEnable() {
        // Add database migration
        SMDatabase.runMigration("230615131000_CreateWaystonesTable", () -> {
            SMDatabase.prepareStatement(
            "CREATE TABLE IF NOT EXISTS waystones (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "world TEXT NOT NULL," +
                "x INT NOT NULL," +
                "y INT NOT NULL," +
                "z INT NOT NULL," +
                "under_block TEXT NOT NULL)").executeUpdate();
        });

        // Get Max Distance
        maxDistance = SMConfig.main().getInt("waystones.max-distance", 1000);

        // Load worlds supporting waystones
        List<String> worldsList = SMConfig.main().getStringList("waystones.worlds");
        worldsList.forEach(worldName -> {
            World world = Bukkit.getServer().getWorld(worldName);
            if (world != null) {
                STEMCraft.info("Adding world " + worldName + " to the list of worlds for waystones");
                worlds.add(world);
            }
        });

        // Load waystone blocks
        waystoneTypes = SMConfig.main().getStringList("waystones.blocks");

        // Load waystone cache
        waystoneCache.clear();
        try {
            PreparedStatement statement = SMDatabase.prepareStatement("SELECT * FROM waystones");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String worldName = resultSet.getString("world");
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");
                String underBlock = resultSet.getString("under_block");
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);

                waystoneCache.put(location, underBlock);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /* BlockBreakEvent */
        SMEvent.register(BlockBreakEvent.class, ctx -> {
            Block block = ctx.event.getBlock();

            if (!worlds.contains(block.getLocation().getWorld())) {
                return;
            }

            block = isValidWaystone(block);

            if(block != null) {
                removeWaystone(block);
            }
        });

        /* BlockPlaceEvent */
        SMEvent.register(BlockPlaceEvent.class, ctx -> {
            Block block = ctx.event.getBlock();

            if (!worlds.contains(block.getLocation().getWorld())) {
                return;
            }

            block = isValidWaystone(block);

            if(block != null) {
                insertWaystone(block);
            }
        });

        /* PlayerInteractEvent */
        SMEvent.register(PlayerInteractEvent.class, ctx -> {
            Player player = ctx.event.getPlayer();
            Block clickedBlock = ctx.event.getClickedBlock();

            STEMCraft.runOnce("waystone-" + player.getName(), 5L, () -> {
                if (clickedBlock == null) {
                    return;
                }

                if (!worlds.contains(clickedBlock.getLocation().getWorld())) {
                    return;
                }

                if(player.getGameMode() == GameMode.SURVIVAL &&
                        ctx.event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                        clickedBlock.getType() == Material.LODESTONE
                ) {
                    player.getInventory().getItemInMainHand();
                    if (player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                        if (this.doesWaystoneExist(clickedBlock.getLocation())) {
                            this.teleportToNearestWaystone(clickedBlock.getLocation(), player);
                        }
                    }
                }
            });
        });

        /* EntityExplodeEvent */
        SMEvent.register(EntityExplodeEvent.class, ctx -> {
            if (!worlds.contains(ctx.event.getLocation().getWorld())) {
                return;
            }

            List<Location> locations = ctx.event.blockList().stream()
                .map(Block::getLocation)
                .collect(Collectors.toList());

            removeWaystoneList(locations);
        });

        /* BlockPistonExtendEvent */
        SMEvent.register(BlockPistonExtendEvent.class, ctx -> {
            List<Location> blockLocations = getPistonBlockLocations(ctx.event.getBlocks(), ctx.event.getDirection());

            updateWaystoneList(blockLocations);
        });

        /* BlockPistonRetractEvent */
        SMEvent.register(BlockPistonRetractEvent.class, ctx -> {
            List<Location> blockLocations = getPistonBlockLocations(ctx.event.getBlocks(), ctx.event.getDirection());

            updateWaystoneList(blockLocations);
        });

        return true;
    }

    /**
     * Include the blocks before and after the list of blocks in the direction of the piston
     */
    private List<Location> getPistonBlockLocations(List<Block> blocks, BlockFace direction) {
        List<Location> extendedLocations = new ArrayList<>();
        
        for (Block block : blocks) {
            extendedLocations.add(block.getLocation());

            Block target = block.getRelative(direction.getOppositeFace());
            extendedLocations.add(target.getLocation()); // Block before the move

            target = block.getRelative(direction);
            extendedLocations.add(target.getLocation()); // Block before after move
        }

        return extendedLocations;
    }

    /**
     * Is the location a valid waystone. Returns the lodestone block.
     * @param block The block to check
     * @return The lodestone block if it is a valid waystone, otherwise null
     */
    private Block isValidWaystone(Block block, Boolean absolute) {
        if (block.getType() == Material.LODESTONE) {
            Block blockBelow = block.getRelative(BlockFace.DOWN);
            if(waystoneTypes.contains(blockBelow.getType().name())) {
                return block;
            }
        } else if(!absolute && waystoneTypes.contains(block.getType().name())) {
            Block blockAbove = block.getRelative(BlockFace.UP);
            if(blockAbove.getType() == Material.LODESTONE) {
                return blockAbove;
            }
        }

        return null;
    }

    private Block isValidWaystone(Block block) {
        return isValidWaystone(block, false);
    }

    /**
     * Remove a Waystone
     * @param block The block to remove
     * @param silent Should the removal be silent
     */
    private void removeWaystone(Block block, Boolean silent) {
        if(!waystoneCache.containsKey(block.getLocation())) {
            return;
        }

        waystoneCache.remove(block.getLocation());
        teleportCache.clear();

        try {
            PreparedStatement statement = SMDatabase.prepareStatement(
                    "DELETE FROM waystones WHERE world = ? AND x = ? AND y = ? AND z = ?"
            );
            statement.setString(1, block.getWorld().getName());
            statement.setInt(2, block.getX());
            statement.setInt(3, block.getY());
            statement.setInt(4, block.getZ());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        STEMCraft.info("Waystone removed at " + block.getLocation());

        if(!silent) {
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 2.0f);
        }
    }

    private void removeWaystone(Block block) {
        removeWaystone(block, false);
    }

    /**
     * Remove a list of blocks that could be a waystone
     * @param locations The list of locations to remove
     */
    private void removeWaystoneList(List<Location> locations) {
        if (locations.isEmpty()) return;
    
        for (Location location : locations) {
            removeWaystone(location.getBlock(), true);
        }
    }

    private void updateWaystone(Location location) {
        boolean exists = doesWaystoneExist(location);

        Block waystone = isValidWaystone(location.getBlock());


        if (exists && (waystone == null || !waystone.getLocation().equals(location))) {
            removeWaystone(location.getBlock());
        } else if (!exists && waystone != null && waystone.getLocation().equals(location)) {
            insertWaystone(waystone);
        }
    }

    private void updateWaystoneList(List<Location> locations) {
        STEMCraft.runLater(5, () -> {
            for(Location location : locations) {
                if (!worlds.contains(location.getWorld())) {
                    return;
                }

                List<Location> checkLocationList = Arrays.asList(
                        location,
                        location.clone().add(0, 1, 0)
                );

                for (Location checkLocation : checkLocationList) {
                    if (doesWaystoneExist(checkLocation)) {
                        removeWaystone(checkLocation.getBlock());
                    }

                    if (isValidWaystone(checkLocation.getBlock(), true) != null) {
                        insertWaystone(checkLocation.getBlock());
                    }
                }
            }
        });
    }

    /**
     * Insert a waystone
     *
     * @param block The block to insert
     * @param silent Should the insertion be silent
     */
    private void insertWaystone(Block block, Boolean silent) {
        if (waystoneCache.containsKey(block.getLocation())) {
            return;
        }

        Block blockBelow = block.getRelative(BlockFace.DOWN);
        String blockBelowName = blockBelow.getType().name();

        waystoneCache.put(block.getLocation(), blockBelowName);
        teleportCache.clear();

        try {
            PreparedStatement statement = SMDatabase.prepareStatement(
                    "INSERT INTO waystones (world, x, y, z, under_block) VALUES (?, ?, ?, ?, ?)"
            );
        statement.setString(1, block.getWorld().getName());
        statement.setInt(2, block.getX());
        statement.setInt(3, block.getY());
        statement.setInt(4, block.getZ());
        statement.setString(5, blockBelowName);
        statement.executeUpdate();
        statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        STEMCraft.info("Waystone added at " + block.getLocation());

        if(!silent) {
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2.0f);
        }
    }

    private void insertWaystone(Block block) {
        insertWaystone(block, false);
    }

    public boolean doesWaystoneExist(Location location) {
        return waystoneCache.containsKey(location);
    }

    private void teleportToNearestWaystone(Location location, Player player) {
        Location teleportTo = null;

        if(teleportCache.containsKey(location)) {
            teleportTo = teleportCache.get(location);
        } else {
            String underBlock = location.getBlock().getRelative(BlockFace.DOWN).getType().name();

            World world = location.getWorld();
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            // Adjust the coordinates by +/- 1000
            int minX = x - maxDistance;
            int minY = y - maxDistance;
            int minZ = z - maxDistance;
            int maxX = x + maxDistance;
            int maxY = y + maxDistance;
            int maxZ = z + maxDistance;

            double closestDistance = Double.MAX_VALUE;

            for(Location waystoneLocation : waystoneCache.keySet()) {
                if (waystoneLocation.getWorld().equals(world) && !waystoneLocation.equals(location) && waystoneCache.get(waystoneLocation).equals(underBlock)) {
                    if (waystoneLocation.getX() >= minX && waystoneLocation.getX() <= maxX &&
                            waystoneLocation.getY() >= minY && waystoneLocation.getY() <= maxY &&
                            waystoneLocation.getZ() >= minZ && waystoneLocation.getZ() <= maxZ) {

                        double distance = waystoneLocation.distance(location);
                        if (distance < closestDistance) {
                            teleportTo = waystoneLocation;
                            closestDistance = distance;
                        }
                    }
                }
            }

            if(teleportTo != null) {
                teleportCache.put(location, teleportTo);
            }
        }

        if (teleportTo != null) {
            Location safeLocation = SMCommon.findSafeLocation(teleportTo, 6, true);
            if (safeLocation != null) {
                Location finalTeleportTo = teleportTo;
                STEMCraft.runLater(() -> {
                    World world = location.getWorld();

                    if(world != null) {
                        world.playSound(location, Sound.BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF, 1f, 0.5f);
                        world.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 3f);
                        player.teleport(safeLocation);
                        world.playSound(safeLocation, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 3f);
                        world.playSound(safeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                        STEMCraft.runLater(5, () -> {
                            if(isValidWaystone(finalTeleportTo.getBlock()) == null) {
                                // Waystone is no longer valid
                                player.teleport(location);
                                removeWaystone(finalTeleportTo.getBlock(), true);
                            }
                        });
                    }
                });

                return;
            }
        }

        STEMCraft.runLater(() -> {
            Objects.requireNonNull(location.getWorld()).playSound(location, Sound.BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF, 1f, 0.5f);
        });
    }
}

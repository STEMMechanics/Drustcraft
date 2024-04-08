package com.stemcraft.feature;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
import com.stemcraft.core.SMCommon;
import com.stemcraft.core.SMDatabase;
import com.stemcraft.core.SMFeature;
import com.stemcraft.core.SMMessenger;
import com.stemcraft.core.event.SMEvent;

public class SMWaystones extends SMFeature {
    private List<String> waystoneTypes = Arrays.asList("GOLD_BLOCK", "EMERALD_BLOCK", "DIAMOND_BLOCK");

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

        SMConfig.main().set("waystones.allowedDimensions", new ArrayList<String>(), "Whitelist for waystone dimensions.");

        SMEvent.register(BlockBreakEvent.class, ctx -> {
            Block block = ctx.event.getBlock();
            if (block.getType() == Material.LODESTONE) {
                try {
                    this.removeWaystone(block);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                Block blockAbove = block.getRelative(BlockFace.UP);

                if (blockAbove.getType() == Material.LODESTONE) {
                    try {
                        this.removeWaystone(blockAbove);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        SMEvent.register(BlockPlaceEvent.class, ctx -> {
            Block block = ctx.event.getBlock();
            Block waystone = isValidWaystone(block);

            if(waystone != null) {
                try {
                    insertWaystone(waystone);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });

        SMEvent.register(PlayerInteractEvent.class, ctx -> {
            Player player = ctx.event.getPlayer();
            Block clickedBlock = ctx.event.getClickedBlock();

            if (clickedBlock == null) {
                return;
            }
    
            STEMCraft.runOnce("waystone-" + player.getName(), 5L, () -> {
                if(player.getGameMode() == GameMode.SURVIVAL && ctx.event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (clickedBlock.getType() == Material.LODESTONE && (player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType().equals(Material.AIR))) {
                        if(this.checkWaystoneExists(clickedBlock.getLocation())) {
                            this.teleportToNearestWaystone(clickedBlock.getLocation(), player);
                        }
                    }
                }
            });
        });

        SMEvent.register(EntityExplodeEvent.class, ctx -> {
            List<Location> locations = ctx.event.blockList().stream()
                .map(Block::getLocation)
                .collect(Collectors.toList());

            try {
                removeWaystones(locations);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        SMEvent.register(BlockPistonExtendEvent.class, ctx -> {
            List<Location> blockLocations = getPistonBlockLocations(ctx.event.getBlocks(), ctx.event.getDirection());

            try {
                updateWaystone(blockLocations);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        SMEvent.register(BlockPistonRetractEvent.class, ctx -> {
            List<Location> blockLocations = getPistonBlockLocations(ctx.event.getBlocks(), ctx.event.getDirection());
            try {
                updateWaystone(blockLocations);
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
            if(target != null) {
                extendedLocations.add(target.getLocation()); // Block before the move
            }

            target = block.getRelative(direction);
            if(target != null) {
                extendedLocations.add(target.getLocation()); // Block before after move
            }
        }

        return extendedLocations;
    }

    /**
     * Is the location a valid waystone. Returns the lodestone block.
     * @param loc
     * @return
     */
    private Block isValidWaystone(Block block) {
        if (block.getType() == Material.LODESTONE) {
            Block blockBelow = block.getRelative(BlockFace.DOWN);
            if(waystoneTypes.contains(blockBelow.getType().name())) {
                return block;
            }
        } else if(waystoneTypes.contains(block.getType().name())) {
            Block blockAbove = block.getRelative(BlockFace.UP);
            if(blockAbove.getType() == Material.LODESTONE) {
                return blockAbove;
            }
        }

        return null;
    }

    /**
     * Remove a Waystone
     * @param block
     * @throws SQLException
     */
    private void removeWaystone(Block block) throws SQLException {
        PreparedStatement statement = SMDatabase.prepareStatement(
                "DELETE FROM waystones WHERE world = ? AND x = ? AND y = ? AND z = ?"
        );
        statement.setString(1, block.getWorld().getName());
        statement.setInt(2, block.getX());
        statement.setInt(3, block.getY());
        statement.setInt(4, block.getZ());
        statement.executeUpdate();
        statement.close();

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 2.0f);
    }

    /**
     * Remove a list of blocks that could be a waystone
     * @param blocks
     * @throws SQLException
     */
    private void removeWaystones(List<Location> locations) throws SQLException {
        if (locations.isEmpty()) return;
    
        for (Location location : locations) {
            Block waystone = isValidWaystone(location.getBlock());
            if(waystone != null) {
                removeWaystone(waystone);
            }
        }
    }

    private void updateWaystoneForLocation(Location location) throws SQLException {
        Block waystone = isValidWaystone(location.getBlock());
        boolean exists = checkWaystoneExists(location);

        if (exists && (waystone == null || !waystone.getLocation().equals(location))) {
            removeWaystone(location.getBlock());
        } else if (!exists && waystone != null && waystone.getLocation().equals(location)) {
            insertWaystone(waystone);
        }
    }

    private void updateWaystone(List<Location> locations) throws SQLException {
        STEMCraft.runLater(5, () -> {
            try {
                List<String> allowedDimensionStringList = SMConfig.main().getStringList("waystones.allowedDimensions");
                boolean allowedDimensionStringListIsNull = allowedDimensionStringList == null;

                for(Location location : locations) {
                    if (!allowedDimensionStringListIsNull)
                        if (!allowedDimensionStringList.contains(Objects.requireNonNull(location.getWorld()).getName()))
                            return;

                    updateWaystoneForLocation(location.add(0f, 1f, 0f));

                    updateWaystoneForLocation(location);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Insert a waystone
     */
    private void insertWaystone(Block block) throws SQLException {
        Block blockBelow = block.getRelative(BlockFace.DOWN);

        String blockBelowName = blockBelow.getType().name();
        if(this.waystoneTypes.contains(blockBelowName)) {
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

            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2.0f);
        }
    }

    public boolean checkWaystoneExists(Location location) {
        try {
            String query = "SELECT COUNT(*) FROM waystones WHERE x = ? AND y = ? AND z = ? AND world = ?";
            PreparedStatement statement = SMDatabase.prepareStatement(query);
            statement.setInt(1, location.getBlockX());
            statement.setInt(2, location.getBlockY());
            statement.setInt(3, location.getBlockZ());
            statement.setString(4, location.getWorld().getName());
            
            // Execute the query and retrieve the result
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count > 0; // If count > 0, location is registered; otherwise, it is not
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false; // Default return value if there's an error or no result found
    }

    private void teleportToNearestWaystone(Location location, Player player) {
        Block underBlock = location.getBlock().getRelative(BlockFace.DOWN);
        
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // Adjust the coordinates by +/- 1000
        int search = 4000;
        int minX = x - search;
        int minY = y - search;
        int minZ = z - search;
        int maxX = x + search;
        int maxY = y + search;
        int maxZ = z + search;
        
        try {
            PreparedStatement statement = SMDatabase.prepareStatement(
                    "SELECT * FROM waystones WHERE under_block = ? AND world = ? AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?"
            );
            statement.setString(1, underBlock.getType().name());
            statement.setString(2, world.getName());
            statement.setInt(3, minX);
            statement.setInt(4, maxX);
            statement.setInt(5, minY);
            statement.setInt(6, maxY);
            statement.setInt(7, minZ);
            statement.setInt(8, maxZ);
            
            ResultSet resultSet = statement.executeQuery();

            Location closestWaystoneLocation = null;
            double closestDistance = Double.MAX_VALUE;

            while (resultSet.next()) {
                String resultWorldName = resultSet.getString("world");
                int resultX = resultSet.getInt("x");
                int resultY = resultSet.getInt("y");
                int resultZ = resultSet.getInt("z");

                if(!resultWorldName.equalsIgnoreCase(location.getWorld().getName()) || resultX != location.getBlockX() || resultY != location.getBlockY() || resultZ != location.getBlockZ()) {
                    Location waystoneLocation = new Location(Bukkit.getWorld(resultWorldName), resultX, resultY, resultZ);
                    double distance = waystoneLocation.distance(location);
                    if (distance < closestDistance) {
                        closestWaystoneLocation = waystoneLocation;
                        closestDistance = distance;
                    }
                }
            }

            if (closestWaystoneLocation != null) {
                // Teleport the player to a safe location near the closest waystone
                Location safeLocation = SMCommon.findSafeLocation(closestWaystoneLocation, 6, true);
                if (safeLocation != null) {
                    STEMCraft.runLater(() -> {
                        location.getWorld().playSound(location, Sound.BLOCK_METAL_PRESSURE_PLATE_CLICK_OFF, 1f, 0.5f);
                        location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 3f);
                        player.teleport(safeLocation);
                        location.getWorld().playSound(safeLocation, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 3f);
                        location.getWorld().playSound(safeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    });
                } else {
                    player.sendMessage("Unable to find a safe location near the waystone");
                }
            } else {
                SMMessenger.infoLocale(player, "WAYSTONE_NONE_FOUND");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

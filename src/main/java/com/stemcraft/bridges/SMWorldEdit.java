package com.stemcraft.bridges;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.limit.PermissiveSelectorLimits;
import com.sk89q.worldedit.regions.selector.limit.SelectorLimits;
import com.stemcraft.STEMCraft;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SMWorldEdit {
    private static final boolean initialized = false;
    private static Plugin worldEditPlugin;

    /**
     * Get the base plugin if installed and loaded
     * @return {WorldEditPlugin} The plugin class
     */
    private static WorldEditPlugin getBase() {
        if (!initialized) {
            if (worldEditPlugin == null) {
                worldEditPlugin = STEMCraft.getPlugin().getServer().getPluginManager().getPlugin("WorldEdit");
                if(!(worldEditPlugin instanceof WorldEditPlugin)) {
                    worldEditPlugin = null;
                }
            }
        }

        return (WorldEditPlugin) worldEditPlugin;
    }

    /**
     * Get the player selection from the brush as a list of Locations
     * @param player The player selection
     * @return The list of Location or null of no selection
     */
    public static List<Location> getSelection(Player player) {
        WorldEditPlugin worldEdit = getBase();
        if(worldEdit == null) {
            return null;
        }

        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(player.getWorld());
        Region region;

        try {
            region = worldEdit.getSession(player).getSelection(world);
        } catch (Exception e) {
            return null;
        }

        if (region == null) {
            return null;
        }

        List<Location> points = new ArrayList<>();

        if (region instanceof Polygonal2DRegion polyRegion) {
            for (com.sk89q.worldedit.math.BlockVector2 point : polyRegion.getPoints()) {
                points.add(new Location(player.getWorld(), point.getX(), region.getMinimumPoint().getY(), point.getZ()));
            }
        } else {
            // For non-polygon selections, just use min and max points
            points.add(BukkitAdapter.adapt(player.getWorld(), region.getMinimumPoint()));
            points.add(BukkitAdapter.adapt(player.getWorld(), region.getMaximumPoint()));
        }

        return points;
    }

    /**
     * Set a players world edit selection
     * @param player The player to set
     * @param points The selection points
     */
    public static void setSelection(Player player, List<Location> points) {
        WorldEditPlugin worldEdit = getBase();
        if (worldEdit == null || points.size() < 2) {
            return;
        }

        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(Objects.requireNonNull(points.get(0).getWorld()));
        LocalSession session = worldEdit.getSession(player);
        RegionSelector selector;

        if (points.size() == 2) {
            // Cuboid selection
            selector = new CuboidRegionSelector(world);
            BlockVector3 pos1 = toBlockVector3(points.get(0));
            BlockVector3 pos2 = toBlockVector3(points.get(1));

            try {
                selector.selectPrimary(pos1, null);
                selector.selectSecondary(pos2, null);
            } catch (Exception e) {
                STEMCraft.error(e);
                return;
            }
        } else {
            // Polygon selection
            selector = new Polygonal2DRegionSelector(world);
            SelectorLimits limits = PermissiveSelectorLimits.getInstance();


            boolean isFirst = true;
            for (Location point : points) {
                BlockVector3 pos = toBlockVector3(point);
                try {
                    if (isFirst) {
                        selector.selectPrimary(pos, limits);
                        isFirst = false;
                    } else {
                        selector.selectSecondary(pos, limits);
                    }
                } catch (Exception e) {
                    STEMCraft.error(e);
                }
            }
        }

        session.setRegionSelector(world, selector);
    }

    /**
     * Convert Location to BlockVector3
     * @param location The location to convert
     * @return The resulting BlockVector3
     */
    private static BlockVector3 toBlockVector3(Location location) {
        return BlockVector3.at(location.getX(), location.getY(), location.getZ());
    }
}

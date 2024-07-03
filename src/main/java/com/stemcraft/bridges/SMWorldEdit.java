package com.stemcraft.bridges;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.stemcraft.STEMCraft;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

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
}

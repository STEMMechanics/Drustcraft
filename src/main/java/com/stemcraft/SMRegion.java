package com.stemcraft;

import com.stemcraft.utils.SMUtilsMath;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Setter
@Getter
public class SMRegion {
    private static List<SMRegion> regionList = new ArrayList<>();

    private String name;
    private List<Location> points;
    private Location teleportTo;

    /**
     * Save a region to disk
     */
    public void save() {
        if(name.isEmpty() || points.isEmpty()) {
            return;
        }

        String path = "regions." + Objects.requireNonNull(points.get(0).getWorld()).getName() + "." + name;
        SMConfig.remove(path);

        for(int i = 0; i < points.size(); i++) {
            Location location = points.get(i);
            SMConfig.set(path + ".points." + i + ".x", location.getBlockX());
            SMConfig.set(path + ".points." + i + ".y", location.getBlockY());
            SMConfig.set(path + ".points." + i + ".z", location.getBlockZ());
        }

        if(teleportTo != null && teleportTo.getWorld() != null) {
            SMConfig.set(path + ".flags.teleport.world", teleportTo.getWorld().getName());
            SMConfig.set(path + ".flags.teleport.x", teleportTo.getX());
            SMConfig.set(path + ".flags.teleport.y", teleportTo.getY());
            SMConfig.set(path + ".flags.teleport.z", teleportTo.getZ());
        }
    }

    /**
     * Get all the regions filtered by World (null for all)
     * @param world Filter by world
     */
    public static List<SMRegion> findRegions(World world) {
        List<SMRegion> list = new ArrayList<>();

        regionList.forEach(region -> {
            if(!region.getPoints().isEmpty()) {
                if(Objects.equals(region.getPoints().get(0).getWorld(), world)) {
                    list.add(region);
                }
            }
        });

        return list;
    }

    /**
     * Get all the regions filtered by Location (null for all)
     * @param location Filter by location
     */
    public static List<SMRegion> findRegions(Location location) {
        List<SMRegion> list = new ArrayList<>();

        regionList.forEach(region -> {
            if(!region.getPoints().isEmpty()) {
                if(SMUtilsMath.insideRegion(location, region.getPoints())) {
                    list.add(region);
                }
            }
        });

        return list;
    }
}

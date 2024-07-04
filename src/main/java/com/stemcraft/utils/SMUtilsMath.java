package com.stemcraft.utils;

import org.bukkit.Location;

import java.util.List;
import java.util.Objects;

public class SMUtilsMath {

    /**
     * Check if a location is inside a region (List<Location>)
     * @param location The location to check
     * @param points The region points
     * @return If the location is inside the region
     */
    public static boolean insideRegion(Location location, List<Location> points) {
        if(points.isEmpty() || points.size() < 2) {
            return false;
        }

        if(!Objects.equals(location.getWorld(), points.get(0).getWorld())) {
            return false;
        }

        if(points.size() == 2) {
            // treat as a rectangle
            Location min = points.get(0);
            Location max = points.get(1);

            return location.getX() >= Math.min(min.getX(), max.getX()) &&
                    location.getX() <= Math.max(min.getX(), max.getX()) &&
                    location.getY() >= Math.min(min.getY(), max.getY()) &&
                    location.getY() <= Math.max(min.getY(), max.getY()) &&
                    location.getZ() >= Math.min(min.getZ(), max.getZ()) &&
                    location.getZ() <= Math.max(min.getZ(), max.getZ());
        } else {
            // treat as a polygon
            boolean inside = false;
            int i, j;
            for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {
                Location point1 = points.get(i);
                Location point2 = points.get(j);
                if (((point1.getZ() > location.getZ()) != (point2.getZ() > location.getZ())) &&
                        (location.getX() < (point2.getX() - point1.getX()) * (location.getZ() - point1.getZ()) /
                                (point2.getZ() - point1.getZ()) + point1.getX())) {
                    inside = !inside;
                }
            }
            return inside;
        }
    }

    /**
     * Round a location to 2 decimal points.
     *
     * @param loc The location to round
     * @return The rounded location
     */
    public static Location round(Location loc) {
        double x = Math.round(loc.getX() * 100.0) / 100.0;
        double y = Math.round(loc.getY() * 100.0) / 100.0;
        double z = Math.round(loc.getZ() * 100.0) / 100.0;
        float yaw = (float) (Math.round(loc.getYaw() * 100.0) / 100.0);
        float pitch = (float) (Math.round(loc.getPitch() * 100.0) / 100.0);

        return new Location(loc.getWorld(), x, y, z, yaw, pitch);
    }
}

package com.stemcraft.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class SMUtilsLocation {

    /**
     * Create a location from a comma separated string
     * @param string The string to parse
     * @param world The world to use if not specified in the string
     * @return The location or null
     */
    public static Location fromString(String string, World world) {
        String[] values = string.split(",");

        if (values.length < 3) {
            return null; // Not enough values for a valid location
        }

        World locationWorld;
        int index = 0;

        try {
            Double.parseDouble(values[0]);
            // If this succeeds, the first value is a double, so use the provided world
            if (world == null) {
                return null; // No world provided when needed
            }
            locationWorld = world;
        } catch (NumberFormatException e) {
            // First value is not a double, treat it as a world name
            locationWorld = Bukkit.getWorld(values[0]);
            if (locationWorld == null) {
                return null; // Invalid world name
            }
            index = 1; // Start parsing coordinates from the next value
        }

        if (values.length < index + 3) {
            return null; // Not enough values for coordinates
        }

        try {
            double x = Double.parseDouble(values[index]);
            double y = Double.parseDouble(values[index + 1]);
            double z = Double.parseDouble(values[index + 2]);

            Location location = new Location(locationWorld, x, y, z);

            // Parse yaw if available
            if (values.length > index + 3) {
                location.setPitch(Float.parseFloat(values[index + 3]));
            }

            // Parse pitch if available
            if (values.length > index + 4) {
                location.setYaw(Float.parseFloat(values[index + 4]));
            }

            return location;
        } catch (NumberFormatException e) {
            return null; // Invalid number format for coordinates, yaw, or pitch
        }
    }

    public static Location fromString(String string) {
        return fromString(string, null);
    }

    /**
     * Convert Location to a comma separated string
     * @param location The location to convert
     * @param includePitchYaw Include the pitch/yaw in the string
     * @param includeWorld Include the world name in the string
     * @return The converted string
     */
    public static String toString(Location location, boolean includePitchYaw, boolean includeWorld) {
        String result = location.getX() + "," + location.getY() + "," + location.getZ();

        if(includePitchYaw) {
            result += "," + location.getPitch() + "," + location.getYaw();
        }

        if(includeWorld) {
            result = location.getWorld().getName() + "," + result;
        }

        return result;
    }

    public static String toString(Location location) {
        return toString(location, true, true);
    }
}

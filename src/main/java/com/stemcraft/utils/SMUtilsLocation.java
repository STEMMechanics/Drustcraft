package com.stemcraft.utils;

import com.stemcraft.STEMCraft;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Objects;

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
            result = Objects.requireNonNull(location.getWorld()).getName() + "," + result;
        }

        return result;
    }

    public static String toString(Location location) {
        return toString(location, true, true);
    }


    /**
     * Convert Hash  Map to Location
     * @param map The map to convert
     * @param world The world to use if missing from the map
     * @return The converted location
     */
    public static Location fromMap(HashMap<String, Object> map, World world) {
        World locationWorld = world;

        if(map.containsKey("world")) {
            locationWorld = Bukkit.getWorld((String) map.get("world"));
            if(locationWorld == null) {
                return null;
            }
        }

        if(!map.containsKey("x") || !map.containsKey("y") || !map.containsKey("z")) {
            return null;
        }

        Location location = new Location(locationWorld, (double)map.get("x"), (double)map.get("y"), (double)map.get("z"));

        if(map.containsKey("pitch")) {
            location.setPitch(parseNumber(map.get("pitch"), Float.class));
        }

        if(map.containsKey("yaw")) {
            location.setPitch(parseNumber(map.get("yaw"), Float.class));
        }

        return location;
    }

    public static Location fromMap(HashMap<String, Object> map) {
        return fromMap(map, null);
    }

    /**
     * Convert Location to a Hash Map
     * @param location The location to convert
     * @param includePitchYaw Include the pitch/yaw in the string
     * @param includeWorld Include the world name in the string
     * @return The converted location
     */
    public static HashMap<String, Object> toMap(Location location, boolean includePitchYaw, boolean includeWorld) {
        HashMap<String, Object> map = new HashMap<>();

        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());

        if(includePitchYaw) {
            map.put("pitch", location.getPitch());
            map.put("yaw", location.getYaw());
        }

        if(includeWorld && location.getWorld() != null) {
            map.put("world", location.getWorld().getName());
        }

        return map;
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends Number> T parseNumber(Object value, Class<T> clazz, T defaultValue) {
        if(defaultValue == null) {
            try {
                if (clazz == Integer.class) {
                    defaultValue = clazz.getConstructor(int.class).newInstance(0);
                } else if (clazz == Float.class) {
                    defaultValue = clazz.getConstructor(float.class).newInstance(0.0f);
                } else if (clazz == Double.class) {
                    defaultValue = clazz.getConstructor(double.class).newInstance(0.0);
                }
            } catch(Exception e) {
                STEMCraft.error(e);
            }
        }

        if (value == null) {
            return defaultValue;
        }

        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        if (value instanceof Number num) {
            if (clazz == Integer.class) {
                return clazz.cast(num.intValue());
            } else if (clazz == Float.class) {
                return clazz.cast(num.floatValue());
            } else if (clazz == Double.class) {
                return clazz.cast(num.doubleValue());
            }
        } else if (value instanceof String) {
            try {
                String strValue = (String) value;
                if (clazz == Integer.class) {
                    return clazz.cast(Integer.parseInt(strValue));
                } else if (clazz == Float.class) {
                    return clazz.cast(Float.parseFloat(strValue));
                } else if (clazz == Double.class) {
                    return clazz.cast(Double.parseDouble(strValue));
                }
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends Number> T parseNumber(Object value, Class<T> clazz) {
        return parseNumber(value, clazz, null);
    }
}

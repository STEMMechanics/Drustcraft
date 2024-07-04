package com.stemcraft;

import com.stemcraft.utils.SMUtilsMath;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
@Setter
@Getter
public class SMRegion {
    private static HashMap<String, SMRegion> regionList = new HashMap<>();
    private static HashMap<Player, List<SMRegion>> playerRegionList = new HashMap<>();

    private String name;
    private List<Location> points;
    private int priority;
    private Location teleportEnter;
    private Location teleportExit;
    private Boolean allowDrops;

    public boolean setName(String newName) {
        if(newName == null || newName.isEmpty()) {
            return false;
        }

        String formattedNewName = newName.toLowerCase();
        if(name != null) {
            if (name.equals(formattedNewName)) {
                return true;
            }

            if (regionList.containsKey(formattedNewName)) {
                return false;
            }

            regionList.remove(name);
        }

        regionList.put(formattedNewName, this);

        SMConfig.remove("regions." + name);
        name = formattedNewName;

        return true;
    }

    /**
     * Delete the region
     */
    public void delete() {
        SMConfig.remove("regions." + name);
        SMConfig.save("regions");

        playerRegionList.forEach((player, list) -> list.removeIf(item -> item.getName().equals(name)));

        regionList.remove(name);
    }

    /**
     * Save a region to disk
     */
    public void save() {
        if(name == null || name.isEmpty() || points == null || points.isEmpty()) {
            return;
        }

        String path = "regions." + name;
        SMConfig.remove(path);

        SMConfig.set(path + ".world", Objects.requireNonNull(points.get(0).getWorld()).getName());
        SMConfig.set(path + ".priority", priority);

        for(int i = 0; i < points.size(); i++) {
            Location location = points.get(i);
            SMConfig.set(path + ".points." + i, location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }

        if(teleportEnter != null && teleportEnter.getWorld() != null) {
            SMConfig.set(path + ".flags.teleport-enter", teleportEnter.getWorld().getName() + "," + teleportEnter.getX() + "," + teleportEnter.getY() + "," + teleportEnter.getZ());
        }

        if(teleportExit != null && teleportExit.getWorld() != null) {
            SMConfig.set(path + ".flags.teleport-exit", teleportExit.getWorld().getName() + "," + teleportExit.getX() + "," + teleportExit.getY() + "," + teleportExit.getZ());
        }

        if(allowDrops != null) {
            SMConfig.set(path + ".flags.allow-drops", allowDrops);
        }

        SMConfig.save("regions");
    }

    /**
     * Create a new region
     * @param name The unique name for the region
     * @param points The boundary points of the region
     * @return The region object or null
     */
    public static SMRegion create(String name, List<Location> points) {
        if(regionList.containsKey(name)) {
            return null;
        }

        SMRegion region = new SMRegion();
        region.name = name;
        region.points = points;

        regionList.put(name, region);
        region.save();

        return region;
    }

    /**
     * Get an existing region object by name
     * @param name The name of the region
     * @return The region object or null
     */
    public static SMRegion get(String name) {
        return regionList.get(name.toLowerCase());
    }

    /**
     * Returns is a region with the name exists
     * @param name The name to check
     * @return If the region exists
     */
    public static boolean exists(String name) {
        return regionList.containsKey(name.toLowerCase());
    }

    /**
     * Get a region name list
     * @return The region names
     */
    public static List<String> getRegionNames() {
        return new ArrayList<>(regionList.keySet());
    }

    /**
     * Get a region list
     * @return The region list
     */
    public static List<SMRegion> getRegions() {
        return new ArrayList<>(regionList.values());
    }

    /**
     * Get all the regions filtered by World (null for all)
     * @param world Filter by world
     */
    public static List<SMRegion> findRegions(World world) {
        List<SMRegion> list = new ArrayList<>();

        regionList.forEach((name, region) -> {
            if(!region.getPoints().isEmpty()) {
                if(Objects.equals(region.getPoints().get(0).getWorld(), world)) {
                    list.add(region);
                }
            }
        });

        sortRegionsByPriority(list);
        return list;
    }

    /**
     * Get all the regions filtered by Location (null for all)
     * @param location Filter by location
     */
    public static List<SMRegion> findRegions(Location location) {
        List<SMRegion> list = new ArrayList<>();

        regionList.forEach((name, region) -> {
            if(!region.getPoints().isEmpty()) {
                if(SMUtilsMath.insideRegion(location, region.getPoints())) {
                    list.add(region);
                }
            }
        });

        sortRegionsByPriority(list);
        return list;
    }

    /**
     * Return the list of regions a player is inside
     * @param player The player to query
     * @return The region list
     */
    public static List<SMRegion> playerRegions(Player player) {
        if(playerRegionList.containsKey(player)) {
            return playerRegionList.get(player);
        }

        return new ArrayList<>();
    }

    /**
     * Update the regions that the player is inside
     * @param player The player to update
     * @return If to cancel the player update (ie player movement)
     */
    public static boolean updatePlayerRegions(Player player, Location newLocation) {
        if(!player.isOnline()) {
            playerRegionList.remove(player);
        } else {
            List<SMRegion> regionList = findRegions(newLocation);

            if(playerRegionList.containsKey(player)) {
                boolean skipEnterRegionEvent = false;
                boolean skipFutureTeleports = false;

                List<SMRegion> previousRegions = playerRegionList.get(player);
                List<SMRegion> enteredRegions = regionList.stream()
                        .filter(region -> !previousRegions.contains(region))
                        .toList();
                List<SMRegion> exitedRegions = previousRegions.stream()
                        .filter(region -> !regionList.contains(region))
                        .toList();

                for(SMRegion region : exitedRegions) {
                    if(region.teleportExit != null && !skipFutureTeleports) {
                        SMPlayer.teleport(player, region.teleportExit);
                        skipEnterRegionEvent = true;
                        skipFutureTeleports = true;
                    }
                }

                if(!skipEnterRegionEvent) {
                    for(SMRegion region : enteredRegions) {
                        if(region.teleportEnter != null && !skipFutureTeleports) {
                            SMPlayer.teleport(player, region.teleportEnter);
                            skipFutureTeleports = true;
                        }
                    }
                }
            }

            playerRegionList.put(player, regionList);
        }

        return false;
    }

    public static boolean updatePlayerRegions(Player player) {
        return updatePlayerRegions(player, player.getLocation());
    }

    /**
     * Reload the regions players are inside. This will adjust any flags
     */
    public static void reloadPlayerRegions() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            playerRegionList.put(player, findRegions(player.getLocation()));
        }
    }

    /**
     * Sort the passed list by priority
     * @param list The list to sort
     */
    private static void sortRegionsByPriority(List<SMRegion> list) {
        list.sort(Comparator.comparingInt(SMRegion::getPriority).reversed());
    }

    public static void loadRegions() {
        regionList.clear();

        List<String> names = SMConfig.getKeys("regions");
        names.forEach(name -> {
            String worldName = SMConfig.getString("regions." + name + ".world", "");
            if(worldName.isEmpty()) {
                return;
            }

            World world = Bukkit.getWorld(worldName);
            if(world == null) {
                return;
            }

            List<String> pointNumbers = SMConfig.getKeys("regions." + name + ".points");
            if(pointNumbers.isEmpty()) {
                return;
            }

            SMRegion region = new SMRegion();
            region.name = name.toLowerCase();

            List<Location> points = new ArrayList<>();
            pointNumbers.forEach(pointIndex -> {
                String[] values = SMConfig.getString("regions." + name + ".points." + pointIndex, "").split(",");
                if(values.length >= 3) {
                    try {
                        double x = Double.parseDouble(values[0]);
                        double y = Double.parseDouble(values[1]);
                        double z = Double.parseDouble(values[2]);

                        Location location = new Location(world, x, y, z);
                        points.add(location);
                    } catch (NumberFormatException e) {
                        // Skip this point if any value can't be parsed as a double
                    }
                }
            });
            region.points = points;

            if(SMConfig.contains("regions." + name + ".flags.teleport-enter")) {
                String[] values = SMConfig.getString("regions." + name + ".flags.teleport-enter", "").split(",");
                if(values.length >= 4) {
                    try {
                        World flagWorld = Bukkit.getWorld(values[0]);
                        double x = Double.parseDouble(values[1]);
                        double y = Double.parseDouble(values[2]);
                        double z = Double.parseDouble(values[3]);
                        if(flagWorld != null) {
                            region.teleportEnter = new Location(flagWorld, x, y, z);
                        }
                    } catch (NumberFormatException e) {
                        // Skip this point if any value can't be parsed as a double
                    }
                }
            }

            regionList.put(name, region);
        });
    }
}

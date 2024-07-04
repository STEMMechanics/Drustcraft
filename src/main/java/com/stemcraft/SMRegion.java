package com.stemcraft;

import com.stemcraft.utils.SMUtilsMath;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
@Setter
@Getter
public class SMRegion {
    private static List<SMRegion> regionList = new ArrayList<>();
    private static HashMap<Player, List<SMRegion>> playerRegionList = new HashMap<>();

    private String name;
    private List<Location> points;
    private int priority;
    private Location teleportEnter;
    private Location teleportExit;
    private Boolean allowDrops;

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

        SMConfig.set(path + ".priority", priority);

        if(teleportEnter != null && teleportEnter.getWorld() != null) {
            SMConfig.set(path + ".flags.teleport-enter.world", teleportEnter.getWorld().getName());
            SMConfig.set(path + ".flags.teleport-enter.x", teleportEnter.getX());
            SMConfig.set(path + ".flags.teleport-enter.y", teleportEnter.getY());
            SMConfig.set(path + ".flags.teleport-enter.z", teleportEnter.getZ());
        }

        if(teleportExit != null && teleportExit.getWorld() != null) {
            SMConfig.set(path + ".flags.teleport-exit.world", teleportExit.getWorld().getName());
            SMConfig.set(path + ".flags.teleport-exit.x", teleportExit.getX());
            SMConfig.set(path + ".flags.teleport-exit.y", teleportExit.getY());
            SMConfig.set(path + ".flags.teleport-exit.z", teleportExit.getZ());
        }

        if(allowDrops != null) {
            SMConfig.set(path + ".flags.allow-drops", allowDrops);
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

        sortRegionsByPriority(list);
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
     * Sort the passed list by priority
     * @param list The list to sort
     */
    private static void sortRegionsByPriority(List<SMRegion> list) {
        list.sort(Comparator.comparingInt(SMRegion::getPriority).reversed());
    }
}

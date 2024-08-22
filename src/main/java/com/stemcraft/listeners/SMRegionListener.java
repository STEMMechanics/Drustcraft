package com.stemcraft.listeners;

import com.stemcraft.*;
import com.stemcraft.utils.SMUtilsMath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.*;

import java.util.*;

public class SMRegionListener extends SMListener {
    /**
     * A map of the last known regions that the player was in
     */
    private static HashMap<Player, List<SMRegion>> playerRegionList = new HashMap<>();

    @FunctionalInterface
    public interface PlayerEnterCallback {
        void onEnter(Player player);
    }

    static HashMap<SMRegion, List<PlayerEnterCallback>> enterCallbackMap = new HashMap<>();

//    @FunctionalInterface
//    public interface PlayerExitCallback {
//        void onExit(Player player);
//    }
//
//    @FunctionalInterface
//    public interface BlockBreakCallback {
//        boolean onBlockBreak(Block block, Player player);
//    }
//
//    @FunctionalInterface
//    public interface BlockPlaceCallback {
//        boolean onBlockPlace(Block block, Player player);
//    }

    static List<SMRegion> regions = new ArrayList<>();

    /**
     * Add a region to the listener list
     * @param region The region to add
     */
    static void addRegion(SMRegion region) {
        regions.add(region);
    }

    /**
     * Remove a region from the listener list
     * @param region The region to remove
     */
    static void removeRegion(SMRegion region) {
        regions.remove(region);
    }

    public static void onPlayerEnter(SMRegion region, PlayerEnterCallback callback) {
        List<PlayerEnterCallback> enterCallbackList = enterCallbackMap.computeIfAbsent(region, k -> new ArrayList<>());
        enterCallbackList.add(callback);
    }




    /**
     * When a player moves event
     * @param event The event
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(SMRegionListener.updatePlayerRegions(event.getPlayer(), event.getTo())) {
            event.setCancelled(true);
        }
    }

    /**
     * Update the regions that the player is inside
     * @param player The player to update
     * @return If to cancel the player update (ie player movement)
     */
    private static boolean updatePlayerRegions(Player player, Location newLocation) {
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

                if(!player.hasPermission("stemcraft.region.override")) {
                    for (SMRegion region : exitedRegions) {
                        if (region.teleportExit != null && !skipFutureTeleports) {
                            SMPlayer.teleport(player, region.teleportExit);
                            skipEnterRegionEvent = true;
                            skipFutureTeleports = true;
                        }
                    }

                    if (!skipEnterRegionEvent) {
                        for (SMRegion region : enteredRegions) {
                            if (region.teleportEnter != null && !skipFutureTeleports) {
                                SMPlayer.teleport(player, region.teleportEnter);
                                skipFutureTeleports = true;
                            }
                        }
                    }
                }
            }

            playerRegionList.put(player, regionList);
        }

        return false;
    }

    /**
     * Get all the regions filtered by World (null for all)
     * @param world Filter by world
     */
    public static List<SMRegion> findRegions(World world) {
        List<SMRegion> list = new ArrayList<>();

        for(SMRegion region : regionList) {
            if(!region.getPoints().isEmpty()) {
                if(Objects.equals(region.getPoints().get(0).getWorld(), world)) {
                    list.add(region);
                }
            }
        }

        sortRegionsByPriority(list);
        return list;
    }

    /**
     * Get all the regions filtered by Location (null for all)
     * @param location Filter by location
     */
    public static List<SMRegion> findRegions(Location location) {
        List<SMRegion> list = new ArrayList<>();

        for(SMRegion region : regionList) {
            if(!region.getPoints().isEmpty()) {
                if(SMUtilsMath.insideRegion(location, region.getPoints())) {
                    list.add(region);
                }
            }
        }

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

    private static void sortRegionsByPriority(List<SMRegion> list) {
        list.sort(Comparator.comparingInt(SMRegion::getPriority).reversed());
    }

}

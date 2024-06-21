package com.stemcraft.utils;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.util.Collection;

public class SMUtilsPlayer {

    public static Collection<? extends Player> find(Location location, int radius, World world, String gamemode) {
        Collection<? extends Player> playerList = Bukkit.getOnlinePlayers();

        for(Player player : playerList) {
            if(location != null && radius > 0 && player.getLocation().distance(location) > radius) {
                playerList.remove(player);
                continue;
            }

            if(world != null && player.getWorld() != world) {
                playerList.remove(player);
                continue;
            }
            if(gamemode != null && !player.getGameMode().name().equalsIgnoreCase(gamemode)) {
                playerList.remove(player);
                continue;
            }
        }

        return playerList;
    }

    public static String yawToCompass(float yaw) {
        double rotation = (yaw - 90) % 360;
        if (rotation < 0) {
            rotation += 360.0;
        }

        if (0 <= rotation && rotation < 22.5 || 337.5 <= rotation && rotation < 360) {
            return "W"; // West
        } else if (22.5 <= rotation && rotation < 67.5) {
            return "NW"; // Northwest
        } else if (67.5 <= rotation && rotation < 112.5) {
            return "N"; // North
        } else if (112.5 <= rotation && rotation < 157.5) {
            return "NE"; // Northeast
        } else if (157.5 <= rotation && rotation < 202.5) {
            return "E"; // East
        } else if (202.5 <= rotation && rotation < 247.5) {
            return "SE"; // Southeast
        } else if (247.5 <= rotation && rotation < 292.5) {
            return "S"; // South
        } else if (292.5 <= rotation && rotation < 337.5) {
            return "SW"; // Southwest
        }
        return ""; // This should never happen
    }

    /**
     * Opens a specific type of inventory for the player, or falls back to a default inventory if the specific inventory
     * opening method is not available.
     *
     * @param player The player for whom the inventory should be opened.
     * @param type The type of inventory to open.
     * @param methodName The name of the method to invoke for opening the inventory.
     * @param location The location for the inventory (if applicable).
     * @param force A boolean indicating whether to force the opening of the inventory (if applicable).
     */
    private static void openInventory(Player player, InventoryType type, String methodName, Location location,
                                      Boolean force) {
        try {
            Class<?> playerClass = org.bukkit.entity.Player.class;
            Method method = playerClass.getMethod(methodName, org.bukkit.Location.class, boolean.class);
            method.invoke(player, location, force);
        } catch (Exception e) {
            player.openInventory(Bukkit.createInventory(player, type));
        }
    }

    /**
     * Opens an Anvil inventory for the player.
     *
     * @param player The player for whom the inventory should be opened.
     * @param location The location for the inventory (if applicable).
     * @param force A boolean indicating whether to force the opening of the inventory (if applicable).
     */
    public static void openAnvil(Player player, Location location, Boolean force) {
        openInventory(player, InventoryType.ANVIL, "openAnvil", location, force);
    }

    /**
     * Opens a Cartography Table inventory for the player.
     *
     * @param player The player for whom the inventory should be opened.
     * @param location The location for the inventory (if applicable).
     * @param force A boolean indicating whether to force the opening of the inventory (if applicable).
     */
    public static void openCartography(Player player, Location location, Boolean force) {
        openInventory(player, InventoryType.CARTOGRAPHY, "openCartographyTable", location, force);
    }

    /**
     * Opens a Grindstone inventory for the player.
     *
     * @param player The player for whom the inventory should be opened.
     * @param location The location for the inventory (if applicable).
     * @param force A boolean indicating whether to force the opening of the inventory (if applicable).
     */
    public static void openGrindstone(Player player, Location location, Boolean force) {
        openInventory(player, InventoryType.GRINDSTONE, "openGrindstone", location, force);
    }

    /**
     * Opens a Loom inventory for the player.
     *
     * @param player The player for whom the inventory should be opened.
     * @param location The location for the inventory (if applicable).
     * @param force A boolean indicating whether to force the opening of the inventory (if applicable).
     */
    public static void openLoom(Player player, Location location, Boolean force) {
        openInventory(player, InventoryType.LOOM, "openLoom", location, force);
    }

    /**
     * Opens a Smithing table inventory for the player.
     *
     * @param player The player for whom the inventory should be opened.
     * @param location The location for the inventory (if applicable).
     * @param force A boolean indicating whether to force the opening of the inventory (if applicable).
     */
    public static void openSmithing(Player player, Location location, Boolean force) {
        openInventory(player, InventoryType.SMITHING, "openSmithingTable", location, force);
    }

    /**
     * Opens a Stonecutter inventory for the player.
     *
     * @param player The player for whom the inventory should be opened.
     * @param location The location for the inventory (if applicable).
     * @param force A boolean indicating whether to force the opening of the inventory (if applicable).
     */
    public static void openStonecutter(Player player, Location location, Boolean force) {
        openInventory(player, InventoryType.STONECUTTER, "openStonecutter", location, force);
    }
}

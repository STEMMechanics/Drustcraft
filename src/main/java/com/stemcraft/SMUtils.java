package com.stemcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class SMUtils {

    /**
     * Convert a set object to a list object.
     *
     * @param set The set object to convert.
     * @return The converted list object.
     */
    public static List<String> convertSetToList(Set<?> set) {
        List<String> stringList = new ArrayList<>();
        for (Object obj : set) {
            stringList.add(String.valueOf(obj));
        }

        return stringList;
    }

    /**
     * Teleport a player after 1 tick. This avoids the moved too quickly issue
     *
     * @param player The player to teleport
     * @param location The location to teleport the player
     */
    public static void delayedTeleport(Player player, Location location) {
        STEMCraft.runLater(1, () -> {
            player.teleport(location);
        });
    }
}

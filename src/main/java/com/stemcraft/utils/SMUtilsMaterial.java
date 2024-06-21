package com.stemcraft.utils;

import org.bukkit.Material;

public class SMUtilsMaterial {

    /**
     * Check if the material is air.
     * @param material The material to check.
     * @return True if the material is air.
     */
    public static boolean isAir(final Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR || material.isAir();
    }
}

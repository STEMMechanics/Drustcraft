package com.stemcraft.utils;

import com.stemcraft.STEMCraft;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class SMUtilsSerializer {

    public static String serializePotionEffects(List<PotionEffect> effects) {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> serializedEffects = effects.stream()
                .map(PotionEffect::serialize)
                .collect(Collectors.toList());
        config.set("effects", serializedEffects);
        return config.saveToString();
    }

    @SuppressWarnings("unchecked")
    public static List<PotionEffect> deserializePotionEffects(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            STEMCraft.error(e);
        }
        List<Map<String, Object>> serializedEffects = (List<Map<String, Object>>) config.getList("effects");
        return serializedEffects.stream()
                .map(PotionEffect::new)
                .collect(Collectors.toList());
    }

    public static String serializeItemStacks(ItemStack[] stacks) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("items", stacks);
        return config.saveToString();
    }

    public static ItemStack[] deserializeItemStacks(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            STEMCraft.error(e);
        }

        return config.getList("items").toArray(new ItemStack[0]);
    }
}

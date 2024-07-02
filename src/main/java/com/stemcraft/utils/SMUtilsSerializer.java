package com.stemcraft.utils;

import com.stemcraft.STEMCraft;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class SMUtilsSerializer {

    public static String serializePotionEffects(List<PotionEffect> effects) {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> serializedEffects = effects.stream()
                .map(PotionEffect::serialize)
                .collect(Collectors.toList());
        config.set("serialized", serializedEffects);
        return config.saveToString().trim();
    }

    @SuppressWarnings("unchecked")
    public static List<PotionEffect> deserializePotionEffects(String yaml) {
        if(!yaml.isEmpty()) {
            YamlConfiguration config = new YamlConfiguration();
            try {
                config.loadFromString(yaml);
            } catch (Exception e) {
                STEMCraft.error(e);
            }

            List<Map<String, Object>> serializedEffects = (List<Map<String, Object>>) config.getList("serialized");
            if (serializedEffects != null) {
                return serializedEffects.stream()
                        .map(PotionEffect::new)
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }

    public static String serializeItemStacks(ItemStack[] stacks) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("serialized.size", stacks.length);

        for(int i = 0; i < stacks.length; i++ ) {
            if(stacks[i] != null) {
                config.set("serialized.contents." + i, stacks[i]);
            }
        }

        return config.saveToString().trim();
    }

    public static ItemStack[] deserializeItemStacks(String yaml) {
        if(!yaml.isEmpty()) {
            YamlConfiguration config = new YamlConfiguration();
            try {
                config.loadFromString(yaml);
            } catch (Exception e) {
                STEMCraft.error(e);
            }

            int size = config.getInt("serialized.size", 0);
            ItemStack[] stacks = new ItemStack[size];

            for (int i = 0; i < size; i++) {
                ItemStack item = config.getItemStack("serialized.contents." + i);
                stacks[i] = item;
            }

            return stacks;
        }

        return new ItemStack[0];
    }
}

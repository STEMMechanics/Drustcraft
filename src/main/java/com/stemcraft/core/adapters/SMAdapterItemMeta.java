package com.stemcraft.core.adapters;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.stemcraft.core.SMJsonAdapter;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.stemcraft.STEMCraft;

public class SMAdapterItemMeta implements SMJsonAdapter, JsonSerializer<ItemMeta>, JsonDeserializer<ItemMeta> {
    private final String META_TYPE_KEY = "meta-type";
    private final String DISPLAY_MAP_COLOR_KEY = "display-map-color";
    private final String CUSTOM_COLOR_KEY = "custom-color";
    private final String CUSTOM_EFFECTS_KEY = "custom-effects";
        private final String FIREWORK_EFFECTS_KEY = "firework-effects";
    private final String FIREWORK_EFFECT_KEY = "firework-effect";
    private final String COLOR_KEY = "color";

    @Override
    public Class<?> adapterFor() {
        return ItemMeta.class;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ItemMeta deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<String, Object> deserialize = context.deserialize(json, Map.class);
        String metaType = deserialize.getOrDefault(META_TYPE_KEY, "").toString().toUpperCase();
        
        if(metaType.equals("MAP")) {
            if(deserialize.containsKey(DISPLAY_MAP_COLOR_KEY)) {
                Color mapColor = SMAdapterItemMeta.deserializeRawColor(
                        (Map<String, Object>) deserialize.get(DISPLAY_MAP_COLOR_KEY));

                deserialize.put(DISPLAY_MAP_COLOR_KEY, mapColor);
            }
        } else if (metaType.equals("POTION")) {
            if(deserialize.containsKey(CUSTOM_EFFECTS_KEY)) {
                List<PotionEffect> potionEffects = SMAdapterItemMeta.deserializePotionEffects(
                        (List<Map<String, Object>>) deserialize.get(CUSTOM_EFFECTS_KEY));
                deserialize.put(CUSTOM_EFFECTS_KEY, potionEffects);
            }

            if(deserialize.containsKey(CUSTOM_COLOR_KEY)) {
                Color customColor = Color.deserialize((Map<String, Object>) deserialize.get(CUSTOM_COLOR_KEY));
                deserialize.put(CUSTOM_COLOR_KEY, customColor);
            }
        } else if (metaType.equalsIgnoreCase("FIREWORK")) {
            if(deserialize.containsKey(FIREWORK_EFFECTS_KEY)) {
                List<FireworkEffect> fireworkEffects = SMAdapterItemMeta.deserializeFireworkEffects(
                        (List<Map<String, Object>>) deserialize.get(FIREWORK_EFFECTS_KEY));

                deserialize.put(FIREWORK_EFFECTS_KEY, fireworkEffects);
            }
        } else if (metaType.equalsIgnoreCase("FIREWORK_EFFECT")) {
            if(deserialize.containsKey(FIREWORK_EFFECT_KEY)) {
                FireworkEffect fireworkEffect = SMAdapterItemMeta.deserializeRawFireworkEffect(
                        (Map<String, Object>) deserialize.get(FIREWORK_EFFECT_KEY));

                deserialize.put(FIREWORK_EFFECT_KEY, fireworkEffect);
            }
        } else if (metaType.equalsIgnoreCase("LEATHER_ARMOR") || metaType.equalsIgnoreCase("COLORABLE_ARMOR")) {
            if(deserialize.containsKey(COLOR_KEY)) {
                Color color = SMAdapterItemMeta.deserializeRawColor(
                        (Map<String, Object>) deserialize.get(COLOR_KEY));

                deserialize.put(COLOR_KEY, color);
            }
        }

        return (ItemMeta) ConfigurationSerialization.deserializeObject(deserialize, ConfigurationSerialization.getClassByAlias("ItemMeta"));
    }

    @Override
    public JsonElement serialize(ItemMeta src, Type typeOfSrc, JsonSerializationContext context) {
        Map<String, Object> serialize = src.serialize();
        return context.serialize(serialize, Map.class);
    }

    private static List<PotionEffect> deserializePotionEffects(List<Map<String, Object>> rawEffects) {
        return rawEffects.stream().map(SMAdapterItemMeta::deserializeRawPotionEffect).collect(Collectors.toList());
    }
    
    @SuppressWarnings({"unchecked", "deprecation"})
    private static PotionEffect deserializeRawPotionEffect(Map<String, Object> rawEffect) {
        Map<String, Object> rawType = (Map<String, Object>) rawEffect.getOrDefault("type", new LinkedTreeMap<>());
        int typeId = ((Double) rawType.getOrDefault("id", 1D)).intValue();
        
        PotionEffectType type = PotionEffectType.getById(typeId);
        if (type == null) {
            type = PotionEffectType.GLOWING;
        }
        
        int duration = ((Double) rawEffect.getOrDefault("duration", 1D)).intValue();
        int amplifier = ((Double) rawEffect.getOrDefault("amplifier", 1D)).intValue();
        boolean ambient = (boolean) rawEffect.getOrDefault("ambient", true);
        boolean particles = (boolean) rawEffect.getOrDefault("particles", true);
        boolean icon = (boolean) rawEffect.getOrDefault("icon", true);
        
        return new PotionEffect(type, duration, amplifier, ambient, particles, icon);
    }

    public static List<FireworkEffect> deserializeFireworkEffects(List<Map<String, Object>> rawEffects) {
        
        return rawEffects.stream()
                .map(SMAdapterItemMeta::deserializeRawFireworkEffect)
                .collect(Collectors.toList());
    }
    
    @SuppressWarnings("unchecked")
    public static FireworkEffect deserializeRawFireworkEffect(Map<String, Object> rawEffect) {
        
        ArrayList<Map<String, Object>> colors = (ArrayList<Map<String, Object>>) rawEffect.get(
                "colors");
        ArrayList<Map<String, Object>> fades = (ArrayList<Map<String, Object>>) rawEffect.get(
                "fadeColors");
        
        rawEffect.put("colors", deserializeRawColors(colors));
        rawEffect.put("fadeColors", deserializeRawColors(fades));
        
        return (FireworkEffect) FireworkEffect.deserialize(rawEffect);
    }
    
    public static List<Color> deserializeRawColors(List<Map<String, Object>> rawColors) {
        return rawColors.stream().map(SMAdapterItemMeta::deserializeRawColor).collect(Collectors.toList());
    }

    public static Color deserializeRawColor(Map<String, Object> rawColor) {
        Number redNum;
        Number greenNum;
        Number blueNum;
        if (rawColor.containsKey("RED")){
            redNum = ((Number) rawColor.remove("RED"));
            greenNum = ((Number) rawColor.remove("GREEN"));
            blueNum = ((Number) rawColor.remove("BLUE"));
        }else {
            redNum = ((Number) rawColor.remove("red"));
            greenNum = ((Number) rawColor.remove("green"));
            blueNum = ((Number) rawColor.remove("blue"));
        }
        
        final int red = redNum.intValue();
        final int green = greenNum.intValue();
        final int blue = blueNum.intValue();
        rawColor.put("RED", Math.abs(red));
        rawColor.put("GREEN", Math.abs(green));
        rawColor.put("BLUE", Math.abs((blue)));
        
        return Color.deserialize(rawColor);
    }
}
package com.stemcraft.utils;

import com.stemcraft.STEMCraft;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SMUtilsItemStack {

    /**
     * Adds an attribute to the ItemStack with the given key and value.
     *
     * @param item The ItemStack to modify.
     * @param key The key for the attribute.
     * @param value The value for the attribute.
     */
    public static <T, Z> void addAttribute(ItemStack item, String key, T value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(STEMCraft.getPlugin(), key);
            PersistentDataType<Z, T> type = getPersistentDataType(value);
            if (type != null) {
                meta.getPersistentDataContainer().set(namespacedKey, type, value);
                item.setItemMeta(meta);
            }
        }
    }

    /**
     * Checks if the ItemStack has an attribute with the given key.
     *
     * @param item The ItemStack to check.
     * @param key The key for the attribute.
     * @return true if the attribute exists, false otherwise.
     */
    public static boolean hasAttribute(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(STEMCraft.getPlugin(), key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            return container.has(namespacedKey, PersistentDataType.STRING);
        }
        return false;
    }

    /**
     * Removes an attribute from the ItemStack with the given key.
     *
     * @param item The ItemStack to modify.
     * @param key The key for the attribute to remove.
     */
    public static void removeAttribute(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(STEMCraft.getPlugin(), key);
            meta.getPersistentDataContainer().remove(namespacedKey);
            item.setItemMeta(meta);
        }
    }

    /**
     * Retrieves an attribute from the ItemStack with the given key.
     *
     * @param item The ItemStack to check.
     * @param key The key for the attribute.
     * @param typeClass The class of the type you're expecting (String.class, Byte.class, etc.).
     * @return The value of the attribute, or null if not found or if the type doesn't match.
     */
    /**
     * Retrieves an attribute from the ItemStack with the given key or returns a default value if not found.
     *
     * @param item The ItemStack to check.
     * @param key The key for the attribute.
     * @param typeClass The class of the type you're expecting (String.class, Byte.class, etc.).
     * @param defaultValue The default value to return if the attribute is not found or there's an issue.
     * @return The value of the attribute or the default value.
     */
    public static <T, Z> T getAttribute(ItemStack item, String key, Class<T> typeClass, T defaultValue) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(STEMCraft.getPlugin(), key);
            PersistentDataType<Z, T> type = getPersistentDataType(typeClass);
            if (type != null) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                if (container.has(namespacedKey, type)) {
                    T value = container.get(namespacedKey, type);
                    if (value != null) {
                        return value;
                    }
                }
            }
        }

        return defaultValue;
    }

    public static <T, Z> T getAttribute(ItemStack item, String key, Class<T> typeClass) {
        return getAttribute(item, key, typeClass, null);
    }

    /**
     * Determines the PersistentDataType based on the object provided (class or value).
     *
     * @param object The object for which to determine the PersistentDataType (Class<?> or instance of a type).
     * @return The corresponding PersistentDataType, or null if the type is unsupported.
     */
    @SuppressWarnings("unchecked")
    private static <T, Z> PersistentDataType<Z, T> getPersistentDataType(Object object) {
        if (object instanceof Class<?>) {
            Class<?> typeClass = (Class<?>) object;
            if (typeClass == String.class) {
                return (PersistentDataType<Z, T>) PersistentDataType.STRING;
            } else if (typeClass == Byte.class) {
                return (PersistentDataType<Z, T>) PersistentDataType.BYTE;
            } else if (typeClass == Integer.class) {
                return (PersistentDataType<Z, T>) PersistentDataType.INTEGER;
            } else if (typeClass == Double.class) {
                return (PersistentDataType<Z, T>) PersistentDataType.DOUBLE;
            } else if (typeClass == Float.class) {
                return (PersistentDataType<Z, T>) PersistentDataType.FLOAT;
            }
        } else {
            if (object instanceof String) {
                return (PersistentDataType<Z, T>) PersistentDataType.STRING;
            } else if (object instanceof Byte) {
                return (PersistentDataType<Z, T>) PersistentDataType.BYTE;
            } else if (object instanceof Integer) {
                return (PersistentDataType<Z, T>) PersistentDataType.INTEGER;
            } else if (object instanceof Double) {
                return (PersistentDataType<Z, T>) PersistentDataType.DOUBLE;
            } else if (object instanceof Float) {
                return (PersistentDataType<Z, T>) PersistentDataType.FLOAT;
            }
        }
        // Add more types if needed
        return null;
    }
}

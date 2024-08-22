package com.stemcraft;

import com.stemcraft.interfaces.SMConfigType;
import com.stemcraft.utils.SMUtilsString;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.FlowStyle;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SMConfig {
    private static final Map<String, YamlDocument> files = new HashMap<>();
    private static final Map<String, YamlDocument> defaults = new HashMap<>();
    private static final Map<Class<?>, SMConfigType<?>> types = new HashMap<>();

    public static void registerType(Class<?> type, SMConfigType<?> typeClass) {
        types.put(type, typeClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getType(Class<T> type, String path) {
        if (types.containsKey(type)) {
            return (T) types.get(type).load(path);
        } else {
            throw new IllegalArgumentException("No type registered for class: " + type.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void setType(Class<T> type, String path, T value) {
        SMConfigType<T> configType = (SMConfigType<T>) types.get(type);
        if (configType != null) {
            configType.save(path, value);
        } else {
            throw new IllegalArgumentException("No type registered for class: " + type.getName());
        }
    }

    private static String getFileFromPath(String path) {
        return getFileFromPath(path, false);
    }

    private static String getFileFromPath(String path, Boolean includeExtension) {
        String filePath = SMUtilsString.beforeFirst(path, ".");

        if(includeExtension) {
            filePath += ".yml";
        }

        return filePath;
    }

    private static String getKeyFromPath(String path) {
        return SMUtilsString.afterFirst(path, ".");
    }

    /**
     * Check if specific configuration file loaded
     *
     * @param path The configuration path.
     */
    public static Boolean isLoaded(String path) {
        return files.containsKey(getFileFromPath(path));
    }

    /**
     * Load a specific configuration file
     *
     * @param path The configuration path.
     */
    private static YamlDocument load(String path) {
        return load(path, false);
    }

    /**
     * Load a specific configuration file
     *
     * @param path The configuration path.
     * @param reload Reload the configuration file.
     */
    @SuppressWarnings("SameParameterValue")
    private static YamlDocument load(String path, Boolean reload) {
        String name = getFileFromPath(path);
        String filePath = getFileFromPath(path, true);

        if (!isLoaded(path)) {
            try {
                File file = new File(STEMCraft.getPlugin().getDataFolder(), filePath);
                if (!file.exists()) {
                    InputStream defaultData = STEMCraft.getPlugin().getResource(filePath);
                    if (defaultData != null) {
                        // Ensure the parent directory exists
                        if(!file.getParentFile().mkdirs()) {
                            STEMCraft.error(new Exception("Could not create config directory"));
                        }

                        // Write the defaultData to the filePath
                        try {
                            Files.copy(defaultData, file.toPath());
                        } catch (IOException e) {
                            STEMCraft.error(e);
                        }
                    }
                }

                YamlDocument yamlFile = YamlDocument.create(
                        file,
                        new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)),
                        GeneralSettings.builder().setKeyFormat(GeneralSettings.KeyFormat.OBJECT).build(),
                        LoaderSettings.DEFAULT,
                        DumperSettings.builder().setFlowStyle(FlowStyle.AUTO).build(),
                        UpdaterSettings.DEFAULT);

                files.put(name, yamlFile);

                InputStream inputStream = STEMCraft.getPlugin().getResource(filePath);
                if(inputStream != null) {
                    YamlDocument yamlDefaults = YamlDocument.create(inputStream);
                    defaults.put(name, yamlDefaults);
                }

            } catch (Exception e) {
                STEMCraft.error(e);
            }
        } else if (reload) {
            reload(path);
        }

        return getFile(path);
    }

    /**
     * Get a specific configuration file
     *
     * @param path The configuration path.
     * @return The YAML document.
     */
    private static YamlDocument getFile(String path) {
        return getFile(path, true);
    }

    @SuppressWarnings("SameParameterValue")
    private static YamlDocument getFile(String path, boolean loadIfNotExists) {
        if(files.containsKey(getFileFromPath(path))) {
            return files.get(getFileFromPath(path));
        }

        if(loadIfNotExists) {
            return load(path);
        }

        return null;
    }

    /**
     * Get a specific configuration file
     *
     * @param path The configuration path.
     * @return The YAML document.
     */
    private static YamlDocument getDefaultFile(String path) {
        return defaults.getOrDefault(getFileFromPath(path), null);
    }

    /**
     * Reload all config files
     */
    public static void reloadAll() {
        for (String name : files.keySet()) {
            reload(name);
        }
    }

    /**
     * Save the config file.
     */
    public static void save(String path) {
        YamlDocument file = getFile(path);
        if (file != null) {
            try {
                file.save();
            } catch (Exception e) {
                STEMCraft.error(e);
            }
        }
    }

    /**
     * Reload the config file.
     */
    public static void reload(String path) {
        YamlDocument file = getFile(path);
        if (file != null) {
            try {
                file.reload();
            } catch (Exception e) {
                STEMCraft.error(e);
            }
        }
    }

    /**
     * Set key value in config.
     *
     * @param path The path to the key to set
     * @param value The value to set the key
     */
    public static void set(String path, Object value) {
        set(path, value, "");
    }

    /**
     * Set key value in config.
     *
     * @param path The path to the key to set
     * @param value The value to set the key
     * @param comment The key comment
     */
    public static void set(String path, Object value, String comment) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);
        if (file != null) {
            file.set(key, value);
            if (comment != null && !comment.isEmpty()) {
                file.getBlock(key).addComment(comment);
            }
        }
    }

    /**
     * remove key value in config.
     *
     * @param path The path to the key to remove
     */
    public static void remove(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);
        if (file != null) {
            if (file.getBlock(key) != null) {
                file.getBlock(key).removeComments();
            }

            file.remove(key);
        }
    }

    /**
     * Check if specific key exists.
     *
     * @param path The path to check.
     * @return {Boolean} If the key exists.
     */
    public static Boolean contains(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);
        if (file != null) {
            return file.contains(key);
        }

        return false;
    }

    /**
     * Rename a key.
     *
     * @param path The path to change from.
     * @param newPath The path to change to.
     */
    public static void renameKey(String path, String newPath) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);
        String newKey = getKeyFromPath(newPath);

        if(file != null) {
            if(file.contains(key)) {
                Object data = file.get(key);
                file.remove(key);
                file.set(newKey, data);
            }
        }
    }

    /**
     * Get boolean value of key. If it does not exist, returns the default or null.
     *
     * @param key The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static Boolean getBoolean(String key) {
        return getBoolean(key, getDefaultBoolean(key));
    }

    /**
     * Get boolean of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static Boolean getDefaultBoolean(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getBoolean(key);
        }

        return null;
    }

    /**
     * Get boolean value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static Boolean getBoolean(String path, Boolean defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);
        if (file != null) {
            return file.getBoolean(key, defValue);
        }

        return defValue;
    }

    /**
     * Get integer value of key. If it does not exist, returns the default or null.
     *
     * @param key The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static Integer getInt(String key) {
        return getInt(key, getDefaultInt(key));
    }

    /**
     * Get integer of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static Integer getDefaultInt(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getInt(key);
        }

        return null;
    }

    /**
     * Get long value of key. If it does not exist, returns the default or null.
     *
     * @param key The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static Long getLong(String key) {
        return getLong(key, getDefaultLong(key));
    }

    /**
     * Get long of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static Long getDefaultLong(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getLong(key);
        }

        return null;
    }

    /**
     * Get long value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static Long getLong(String path, Long defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (file != null) {
            return file.getLong(key, defValue);
        }

        return defValue;
    }

    /**
     * Get integer value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static Integer getInt(String path, Integer defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (file != null) {
            return file.getInt(key, defValue);
        }

        return defValue;
    }

    /**
     * Get integer list value of key. If it does not exist, returns the default or null.
     *
     * @param path The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static List<Integer> getIntList(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        return file.getIntList(key, getDefaultIntList(key));
    }

    /**
     * Get integer list of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static List<Integer> getDefaultIntList(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getIntList(key);
        }

        return null;
    }

    /**
     * Get integer list value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static List<Integer> getIntList(String path, List<Integer> defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (file != null) {
            return file.getIntList(key, defValue);
        }

        return defValue;
    }

    /**
     * Get double value of key. If it does not exist, returns the default or null.
     *
     * @param key The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static Double getDouble(String key) {
        return getDouble(key, getDefaultDouble(key));
    }

    /**
     * Get double of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static Double getDefaultDouble(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getDouble(key);
        }

        return null;
    }

    /**
     * Get double value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static Double getDouble(String path, Double defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (file != null) {
            return file.getDouble(key, defValue);
        }

        return defValue;
    }

    /**
     * Get double list value of key. If it does not exist, returns the default or null.
     *
     * @param path The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static List<Double> getDoubleList(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        return file.getDoubleList(key, getDefaultDoubleList(key));
    }

    /**
     * Get double list of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static List<Double> getDefaultDoubleList(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getDoubleList(key);
        }

        return null;
    }

    /**
     * Get double list value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static List<Double> getDoubleList(String path, List<Double> defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (file != null) {
            return file.getDoubleList(key, defValue);
        }

        return defValue;
    }

    /**
     * Get float value of key. If it does not exist, returns the default or null.
     *
     * @param key The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static Float getFloat(String key) {
        return getFloat(key, getDefaultFloat(key));
    }

    /**
     * Get float of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static Float getDefaultFloat(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getFloat(key);
        }

        return null;
    }

    /**
     * Get boolean value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static Float getFloat(String path, Float defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (file != null) {
            return file.getFloat(key, defValue);
        }

        return defValue;
    }

    /**
     * Get float list value of key. If it does not exist, returns the default or null.
     *
     * @param path The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static List<Float> getFloatList(String path) {
        YamlDocument file = getDefaultFile(path);
        String key = getKeyFromPath(path);

        return file.getFloatList(key, getDefaultFloatList(key));
    }

    /**
     * Get float list of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static List<Float> getDefaultFloatList(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getFloatList(key);
        }

        return null;
    }

    /**
     * Get float list value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static List<Float> getFloatList(String path, List<Float> defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (file != null) {
            return file.getFloatList(key, defValue);
        }

        return defValue;
    }

    /**
     * Get string value of key. If it does not exist, returns the default or null.
     *
     * @param key The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static String getString(String key) {
        return getString(key, getDefaultString(key));
    }

    /**
     * Get string of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static String getDefaultString(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getString(key);
        }

        return null;
    }

    /**
     * Get string value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static String getString(String path, String defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (file != null) {
            return file.getString(key, defValue);
        }

        return defValue;
    }

    /**
     * Get string list value of key. If it does not exist, returns the default or null.
     *
     * @param path The key to retrieve the value.
     * @return The key value, default or null.
     */
    public static List<String> getStringList(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        return file.getStringList(key, getDefaultStringList(key));
    }

    /**
     * Get string list of the default value of a key.
     *
     * @param path The key to retrieve the default value.
     * @return The key value or null.
     */
    public static List<String> getDefaultStringList(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (defaults != null) {
            return defaults.getStringList(key);
        }

        return null;
    }

    /**
     * Get string list value of key. If it does not exist, returns defValue or null.
     *
     * @param path The key to retrieve the value.
     * @param defValue The default value to return if not existent.
     * @return The key value or defValue.
     */
    public static List<String> getStringList(String path, List<String> defValue) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (file != null) {
            return file.getStringList(key, defValue);
        }

        return defValue;
    }

    /**
     * Fetches a map from the file based on the given key and filters it to contain only String values.
     *
     * @param path The key to fetch the map from the file.
     * @return A map with string keys and Integer values.
     */
    public static Map<String, String> getStringMap(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        Map<String, Object> valueMap =
                file != null ? file.getSection(key).getStringRouteMappedValues(false) : new HashMap<>();
        Map<String, String> resultMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            resultMap.put(entry.getKey().toString(), entry.getValue().toString());
        }

        return resultMap;
    }

    /**
     * Fetches a map from the file based on the given key and filters it to contain only Char values.
     *
     * @param path The key to fetch the map from the file.
     * @return A map with string keys and Integer values.
     */
    public static Map<String, Character> getCharMap(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        Map<String, Object> valueMap =
                file != null ? file.getSection(key).getStringRouteMappedValues(false) : new HashMap<>();
        Map<String, Character> resultMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            String valueAsString = entry.getValue().toString();
            char value = !valueAsString.isEmpty() ? valueAsString.charAt(0) : '\u0000';
            resultMap.put(entry.getKey().toString(), value);
        }

        return resultMap;
    }

    /**
     * Fetches a map from the file based on the given key and filters it to contain only Integer values.
     *
     * @param path The key to fetch the map from the file.
     * @return A map with string keys and Integer values.
     */
    public static Map<String, Integer> getIntMap(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        Map<String, Object> valueMap =
                file != null ? file.getSection(key).getStringRouteMappedValues(false) : new HashMap<>();
        Map<String, Integer> resultMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            try {
                Integer value = Integer.parseInt(entry.getValue().toString());
                resultMap.put(entry.getKey().toString(), value);
            } catch (Exception e) {
                /* empty */
            }
        }

        return resultMap;
    }

    /**
     * Fetches a map from the file based on the given key and filters it to contain only Float values.
     *
     * @param path The key to fetch the map from the file.
     * @return A map with string keys and Float values.
     */
    public static Map<String, Float> getFloatMap(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        Map<String, Object> valueMap =
                file != null ? file.getSection(key).getStringRouteMappedValues(false) : new HashMap<>();
        Map<String, Float> resultMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            try {
                Float value = Float.parseFloat(entry.getValue().toString());
                resultMap.put(entry.getKey().toString(), value);
            } catch (Exception e) {
                /* empty */
            }
        }

        return resultMap;
    }

    /**
     * Fetches a map from the file based on the given key and filters it to contain only Double values.
     *
     * @param path The key to fetch the map from the file.
     * @return A map with string keys and Double values.
     */
    public static Map<String, Double> getDoubleMap(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        Map<String, Object> valueMap =
                file != null ? file.getSection(key).getStringRouteMappedValues(false) : new HashMap<>();
        Map<String, Double> resultMap = new HashMap<>();

        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            try {
                Double value = Double.parseDouble(entry.getValue().toString());
                resultMap.put(entry.getKey().toString(), value);
            } catch (Exception e) {
                /* empty */
            }
        }

        return resultMap;
    }

    public static HashMap<String, Object> getMap(String path) {
        try {
            YamlDocument file = getFile(path);
            String key = getKeyFromPath(path);

            if (file != null) {
                return convertSection(file.getSection(key));
            }
        } catch (Exception e) {
            /* nothing */
        }

        return new HashMap<>();
    }

    public static List<Map<?, ?>> getMapList(String path) {
        try {
            YamlDocument file = getFile(path);
            String key = getKeyFromPath(path);

            if (file != null) {
                return file.getMapList(key);
            }
        } catch (Exception e) {
            /* nothing */
        }

        return new ArrayList<>();
    }

    private static HashMap<String, Object> convertSection(Section section) {
        HashMap<String, Object> map = new HashMap<>();
        for (Object objKey : section.getKeys()) {
            String key = objKey.toString();
            Object value = section.get(key);
            if (value instanceof Section) {
                map.put(key, convertSection((Section)value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }
    /**
     * Fetches a list of keys from the root.
     *
     * @return A list of string keys.
     */
    public static List<String> getKeys() {
        return getKeys(null);
    }

    /**
     * Fetches a list of keys from a path.
     *
     * @param path The path to fetch keys, null for root.
     * @return A list of string keys.
     */
    public static List<String> getKeys(String path) {
        YamlDocument file = getFile(path);
        String key = getKeyFromPath(path);

        if (key.isEmpty()) {
            return SMUtils.convertSetToList(file.getKeys());
        }

        Section section = file.getSection(key);
        if (section != null) {
            return SMUtils.convertSetToList(section.getKeys());
        }

        return new ArrayList<>();
    }

    /**
     * Fetches a list of default keys from the root.
     *
     * @return A list of string keys.
     */
    public static List<String> getDefaultKeys() {
        return getDefaultKeys(null);
    }

    /**
     * Fetches a list of default keys from a path.
     *
     * @param path The path to fetch default keys, null for root.
     * @return A list of string keys.
     */
    public static List<String> getDefaultKeys(String path) {
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        if (key.isEmpty()) {
            return SMUtils.convertSetToList(defaults.getKeys());
        }

        return SMUtils.convertSetToList(defaults.getSection(key).getKeys());
    }

    /**
     * Will add any missing default root values to a user configuration file.
     */
    public static void addMissingDefaultValues() {
        addMissingDefaultValues(null);
    }

    /**
     * Will add any missing default values to a user configuration file.
     *
     * @param path The key to add from, null for root
     */
    public static void addMissingDefaultValues(String path) {
        YamlDocument file = getFile(path);
        YamlDocument defaults = getDefaultFile(path);
        String key = getKeyFromPath(path);

        List<String> defaultKeys = getDefaultKeys(key);
        if (defaultKeys.isEmpty()) {
            return;
        }

        defaultKeys.removeAll(getKeys(key));

        for (String defaultKey : defaultKeys) {
            file.set(defaultKey, defaults.get(defaultKey));
        }

        try {
            file.save();
        } catch (Exception e) {
            STEMCraft.error(e);
        }
    }
}

package com.stemcraft.interfaces;

public interface SMConfigType<T> {
    /**
     * Register the type with the config system
     */
    public void register();

    /**
     * Load a value from the config
     * @param path The path to the value
     * @return The loaded value
     */
    public T load(String path);

    /**
     * Save a value to the config
     * @param path The path to save the value to
     * @param value The value to save
     */
    public void save(String path, T value);
}

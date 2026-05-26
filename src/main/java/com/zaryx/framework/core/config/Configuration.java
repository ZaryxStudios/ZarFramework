package com.zaryx.framework.core.config;

import java.util.*;

/**
 * Base interface for framework configurations.
 * Provides a consistent contract for accessing settings.
 */
public interface Configuration {

    /**
     * Retrieves a string from the configuration
     */
    String getString(String key, String defaultValue);

    /**
     * Retrieves an int from the configuration
     */
    int getInt(String key, int defaultValue);

    /**
     * Retrieves a boolean from the configuration
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Retrieves a double from the configuration
     */
    double getDouble(String key, double defaultValue);

    /**
     * Retrieves a list of strings
     */
    List<String> getStringList(String key);

    /**
     * Retrieves a map of values
     */
    Map<String, Object> getMap(String key);

    /**
     * Sets a value
     */
    void set(String key, Object value);

    /**
     * Retrieves all keys
     */
    Set<String> getKeys();

    /**
     * Checks whether a key exists
     */
    boolean contains(String key);

    /**
     * Returns the configuration name
     */
    String getName();

    /**
     * Saves the configuration
     */
    void save();

    /**
     * Reloads the configuration
     */
    void reload();

    /**
     * Retrieves all top-level sections
     */
    Set<String> getSections();
}

package com.zaryx.framework.core.storage;

import java.util.*;

/**
 * Base interface for framework storage systems.
 */
public interface StorageProvider {

    /**
     * Connects to the storage provider
     */
    boolean connect();

    /**
     * Disconnects from the provider
     */
    void disconnect();

    /**
     * Checks whether it is connected
     */
    boolean isConnected();

    /**
     * Saves an object
     */
    <T> boolean save(String key, T value);

    /**
     * Loads an object
     */
    <T> T load(String key, Class<T> type);

    /**
     * Checks whether a key exists
     */
    boolean exists(String key);

    /**
     * Deletes an object
     */
    boolean delete(String key);

    /**
     * Returns all keys
     */
    Set<String> getAllKeys();

    /**
     * Clears the storage
     */
    void clear();

    /**
     * Returns the storage type
     */
    String getType();

    /**
     * Returns statistics
     */
    Map<String, Object> getStats();
}

package com.zaryx.framework.core.registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Generic registry for framework components.
 * Allows registering, retrieving, and managing components of any type.
 */
public class Registry<T> {

    private final Logger logger;
    private final String registryName;
    private final Map<String, T> entries;
    private final Map<String, String> metadata;

    public Registry(Logger logger, String registryName) {
        this.logger = logger;
        this.registryName = registryName;
        this.entries = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
    }

    /**
     * Registers an entry
     */
    public boolean register(String key, T value) {
        return register(key, value, null);
    }

    /**
     * Registers an entry with metadata
     */
    public boolean register(String key, T value, String metadata) {
        if (key == null || value == null) {
            logger.warning("Cannot register null key or value in " + registryName);
            return false;
        }

        if (entries.containsKey(key)) {
            logger.warning("'" + key + "' is already registered in " + registryName);
            return false;
        }

        entries.put(key, value);
        if (metadata != null) {
            this.metadata.put(key, metadata);
        }
        logger.fine("Registered in " + registryName + ": " + key);
        return true;
    }

    /**
     * Retrieves an entry
     */
    public T get(String key) {
        return key != null ? entries.get(key) : null;
    }

    /**
     * Retrieves an entry with type casting
     */
    @SuppressWarnings("unchecked")
    public <U extends T> U get(String key, Class<U> type) {
        T value = get(key);
        if (value != null && type.isInstance(value)) {
            return (U) value;
        }
        return null;
    }

    /**
     * Retrieves all values
     */
    public Collection<T> getAll() {
        return Collections.unmodifiableCollection(entries.values());
    }

    /**
     * Retrieves all keys
     */
    public Set<String> getAllKeys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    /**
     * Unregisters an entry
     */
    public boolean unregister(String key) {
        boolean removed = entries.remove(key) != null;
        metadata.remove(key);
        if (removed) {
            logger.fine("Unregistered from " + registryName + ": " + key);
        }
        return removed;
    }

    /**
     * Checks if an entry is registered
     */
    public boolean isRegistered(String key) {
        return key != null && entries.containsKey(key);
    }

    /**
     * Returns the number of registered entries
     */
    public int size() {
        return entries.size();
    }

    /**
     * Clears the registry
     */
    public void clear() {
        int size = entries.size();
        entries.clear();
        metadata.clear();
        logger.info("Registry " + registryName + " cleared: " + size + " entries removed");
    }

    /**
     * Returns metadata for an entry
     */
    public String getMetadata(String key) {
        return key != null ? metadata.get(key) : null;
    }

    /**
     * Returns the registry name
     */
    public String getRegistryName() {
        return registryName;
    }

    /**
     * Returns registry statistics
     */
    public String getStats() {
        return registryName + " | Entries: " + entries.size() + 
               " | With metadata: " + metadata.size();
    }
}

package com.zaryx.okaso.core.registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

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

    public boolean register(String key, T value) {
        return register(key, value, null);
    }

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

    public T get(String key) {
        return key != null ? entries.get(key) : null;
    }

    @SuppressWarnings("unchecked")
    public <U extends T> U get(String key, Class<U> type) {
        T value = get(key);
        if (value != null && type.isInstance(value)) {
            return (U) value;
        }
        return null;
    }

    public Collection<T> getAll() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public Set<String> getAllKeys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public boolean unregister(String key) {
        boolean removed = entries.remove(key) != null;
        metadata.remove(key);
        if (removed) {
            logger.fine("Unregistered from " + registryName + ": " + key);
        }
        return removed;
    }

    public boolean isRegistered(String key) {
        return key != null && entries.containsKey(key);
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        int size = entries.size();
        entries.clear();
        metadata.clear();
        logger.info("Registry " + registryName + " cleared: " + size + " entries removed");
    }

    public String getMetadata(String key) {
        return key != null ? metadata.get(key) : null;
    }

    public String getRegistryName() {
        return registryName;
    }

    public String getStats() {
        return registryName + " | Entries: " + entries.size() +
               " | With metadata: " + metadata.size();
    }
}

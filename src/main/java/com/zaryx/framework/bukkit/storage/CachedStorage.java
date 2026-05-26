package com.zaryx.framework.bukkit.storage;

import com.zaryx.framework.bukkit.storage.core.StorageContext;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CachedStorage<T> {

    private final StorageContext storageContext;
    private final Map<String, T> cache = new HashMap<>();
    private final Set<String> dirty = new HashSet<>();
    private final Class<T> type;
    private final Type genericType;

    public CachedStorage(StorageContext storageContext, Class<T> type) {
        this.storageContext = storageContext;
        this.type = type;
        this.genericType = null;
    }

    public CachedStorage(StorageContext storageContext, Type genericType) {
        this.storageContext = storageContext;
        this.genericType = genericType;
        this.type = null;
    }

    public T get(String key) {
        if (this.cache.containsKey(key)) {
            return this.cache.get(key);
        }

        T value = this.genericType != null ? this.storageContext.load(key, this.genericType) : this.storageContext.load(key, this.type);
        if (value != null) {
            this.cache.put(key, value);
        }

        return value;
    }

    public void set(String key, T value) {
        this.cache.put(key, value);
        this.dirty.add(key);
    }

    public boolean isLoaded(String key) {
        return this.cache.containsKey(key);
    }

    public void unload(String key) {
        this.flush(key);
        this.cache.remove(key);
        this.dirty.remove(key);
    }

    public void flush(String key) {
        if (!this.dirty.contains(key)) return;

        T value = this.cache.get(key);
        if (value == null) return;

        this.storageContext.save(key, value);
        this.dirty.remove(key);
    }

    public void saveAll() {
        for (String key : new HashSet<>(this.dirty)) {
            flush(key);
        }
    }
}
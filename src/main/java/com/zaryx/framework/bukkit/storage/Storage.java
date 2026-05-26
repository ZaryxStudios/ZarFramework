package com.zaryx.framework.bukkit.storage;

import com.zaryx.framework.bukkit.storage.core.StorageContext;

import java.lang.reflect.Type;

public abstract class Storage<T> {

    protected final CachedStorage<T> cache;

    protected Storage(StorageContext context, Class<T> type) {
        this.cache = new CachedStorage<>(context, type);
    }

    protected Storage(StorageContext context, Type type) {
        this.cache = new CachedStorage<>(context, type);
    }

    public T get(String key) {
        return this.cache.get(key);
    }

    public void set(String key, T value) {
        this.cache.set(key, value);
    }

    public void unload(String key) {
        this.cache.unload(key);
    }

    public void saveAll() {
        this.cache.saveAll();
    }
}

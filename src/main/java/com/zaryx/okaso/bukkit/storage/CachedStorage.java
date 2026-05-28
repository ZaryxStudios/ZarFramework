package com.zaryx.okaso.bukkit.storage;

import com.zaryx.okaso.bukkit.storage.core.StorageContext;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CachedStorage<T> {

    private final StorageContext storageContext;
    private final Map<String, Entry<T>> cache = new HashMap<>();
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
        Entry<T> entry = this.cache.get(key);
        if (entry != null) {
            return entry.value;
        }

        T value = this.genericType != null ? this.storageContext.load(key, this.genericType) : this.storageContext.load(key, this.type);
        if (value != null) {
            this.cache.put(key, Entry.loaded(value));
        }

        return value;
    }

    public void set(String key, T value) {
        this.cache.put(key, Entry.dirty(value));
    }

    public boolean isLoaded(String key) {
        return this.cache.containsKey(key);
    }

    public void unload(String key) {
        this.flush(key);
        this.cache.remove(key);
    }

    public void flush(String key) {
        Entry<T> entry = this.cache.get(key);
        if (entry == null || !entry.dirty) return;

        if (this.storageContext.save(key, entry.value)) {
            entry.dirty = false;
        }
    }

    public void saveAll() {
        for (String key : new HashSet<>(this.cache.keySet())) {
            flush(key);
        }
    }

    private static final class Entry<T> {
        private final T value;
        private boolean dirty;

        private Entry(T value, boolean dirty) {
            this.value = value;
            this.dirty = dirty;
        }

        private static <T> Entry<T> loaded(T value) {
            return new Entry<>(value, false);
        }

        private static <T> Entry<T> dirty(T value) {
            return new Entry<>(value, true);
        }
    }
}

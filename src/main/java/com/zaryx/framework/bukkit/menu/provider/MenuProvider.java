package com.zaryx.framework.bukkit.menu.provider;

import com.zaryx.framework.bukkit.menu.core.Menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class MenuProvider {

    private static final long DEFAULT_TTL_MILLIS = 5 * 60 * 1000L; // 5 minutes

    private final java.util.concurrent.ConcurrentMap<MenuKey, CacheEntry> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile long ttlMillis = DEFAULT_TTL_MILLIS;

    protected abstract Menu create(MenuKey parameter);

    public Menu get(Object... parameter) {
        MenuKey key = new MenuKey(parameter);
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.touch();
            return entry.menu;
        }

        Menu created = create(key);
        CacheEntry newEntry = new CacheEntry(created);
        CacheEntry existing = cache.putIfAbsent(key, newEntry);
        (existing != null ? existing : newEntry).touch();
        return existing != null ? existing.menu : created;
    }

    public void invalidate(MenuKey key) {
        if (key == null) return;
        cache.remove(key);
    }

    public void clearCache() {
        cache.clear();
    }

    public void purgeExpired() {
        if (ttlMillis <= 0) return;
        long now = System.currentTimeMillis();
        for (java.util.Map.Entry<MenuKey, CacheEntry> e : cache.entrySet()) {
            CacheEntry ce = e.getValue();
            if (ce == null) continue;
            if (now - ce.lastAccess > ttlMillis) {
                cache.remove(e.getKey(), ce);
            }
        }
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    public void setTtlMillis(long ttlMillis) {
        this.ttlMillis = Math.max(0, ttlMillis);
    }

    private static final class CacheEntry {
        final Menu menu;
        volatile long lastAccess;

        CacheEntry(Menu menu) {
            this.menu = menu;
            this.lastAccess = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccess = System.currentTimeMillis();
        }
    }

    public static MenuProvider of(Function<MenuKey, Menu> factory) {
        return new MenuProvider() {
            @Override
            protected Menu create(MenuKey parameter) {
                return factory.apply(parameter);
            }
        };
    }
}

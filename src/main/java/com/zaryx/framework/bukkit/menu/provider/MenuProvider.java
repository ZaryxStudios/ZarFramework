package com.zaryx.framework.bukkit.menu.provider;

import com.zaryx.framework.bukkit.menu.core.Menu;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Base class for menu providers with built-in caching support.
 *
 * <p>Subclasses implement {@link #create(MenuKey)} to construct a {@link Menu}
 * for a given set of parameters. The provider caches created menus and
 * automatically expires entries after a configurable TTL.</p>
 *
 * <p>Use {@link #of(Function)} for simple lambda-based providers without
 * creating a subclass.</p>
 *
 * <h3>Cache behavior</h3>
 * <ul>
 *   <li>Menus are cached by their {@link MenuKey} (parameter array).</li>
 *   <li>Each access refreshes the entry's last-access timestamp.</li>
 *   <li>Expired entries are removed by {@link #purgeExpired()}.</li>
 *   <li>Set TTL to 0 to disable expiration.</li>
 * </ul>
 */
public abstract class MenuProvider {

    private static final long DEFAULT_TTL_MILLIS = 5 * 60 * 1000L; // 5 minutes

    private final ConcurrentMap<MenuKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private volatile long ttlMillis = DEFAULT_TTL_MILLIS;

    /**
     * Create a new menu instance for the given parameters.
     * Called when no cached entry exists.
     *
     * @param parameter the cache key wrapping the provider parameters
     * @return a new menu instance (never null)
     */
    protected abstract Menu create(MenuKey parameter);

    /**
     * Get (or create and cache) a menu for the given parameters.
     *
     * @param parameter the provider parameters
     * @return a cached or freshly created menu
     */
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

    /**
     * Invalidate (remove) a specific cached menu entry.
     *
     * @param key the cache key to invalidate
     */
    public void invalidate(MenuKey key) {
        if (key == null) return;
        cache.remove(key);
    }

    /**
     * Clear all cached menus.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Remove all cache entries whose TTL has expired.
     */
    public void purgeExpired() {
        if (ttlMillis <= 0) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<MenuKey, CacheEntry> e : cache.entrySet()) {
            CacheEntry ce = e.getValue();
            if (ce == null) continue;
            if (now - ce.lastAccess > ttlMillis) {
                cache.remove(e.getKey(), ce);
            }
        }
    }

    /**
     * Get the current TTL in milliseconds.
     *
     * @return the TTL (0 means no expiration)
     */
    public long getTtlMillis() {
        return ttlMillis;
    }

    /**
     * Set the TTL for cached menus.
     *
     * @param ttlMillis time-to-live in milliseconds; 0 disables expiration
     */
    public void setTtlMillis(long ttlMillis) {
        this.ttlMillis = Math.max(0, ttlMillis);
    }

    /**
     * Internal cache entry holding a menu and its last-access timestamp.
     */
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

    /**
     * Create a simple lambda-based provider.
     *
     * @param factory function that creates a menu from a {@link MenuKey}
     * @return a new provider backed by the given factory
     */
    public static MenuProvider of(Function<MenuKey, Menu> factory) {
        return new MenuProvider() {
            @Override
            protected Menu create(MenuKey parameter) {
                return factory.apply(parameter);
            }
        };
    }
}

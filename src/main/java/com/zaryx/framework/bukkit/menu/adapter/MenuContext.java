package com.zaryx.framework.bukkit.menu.adapter;

import com.zaryx.framework.bukkit.menu.core.Menu;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player context attached to a menu session.
 * <p>
 * Stores arbitrary key-value data (flags, state, navigation info) and
 * maintains a navigation back-stack for multi-page menu hierarchies.
 * <p>
 * Thread-safe: backed by {@link ConcurrentHashMap}.
 *
 * <h3>Well-known keys:</h3>
 * <ul>
 *   <li>{@link #NAVIGATING} – Boolean flag indicating an ongoing navigation step</li>
 *   <li>{@link #BACK_STACK} – {@link Deque}&lt;{@link Menu}&gt; for back-navigation</li>
 * </ul>
 */
public class MenuContext {

    /** Flag key set to {@code true} while the player is navigating between menus. */
    public static final String NAVIGATING = "menu:navigating";

    /** Key for the back-navigation stack ({@link Deque Deque&lt;Menu&gt;}). */
    public static final String BACK_STACK = "menu:back_stack";

    private final Map<String, Object> data = new ConcurrentHashMap<>();

    /**
     * Store a value under the given key.
     *
     * @param key   the storage key (must not be null)
     * @param value the value to store (may be null to act as a tombstone)
     * @param <T>   the value type
     * @throws NullPointerException if {@code key} is null
     */
    public <T> void set(String key, T value) {
        Objects.requireNonNull(key, "key must not be null");
        this.data.put(key, value);
    }

    /**
     * Retrieve a value by key.
     *
     * @param key the storage key
     * @param <T> the expected value type
     * @return the stored value, or {@code null} if absent (or key is null)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key == null) {
            return null;
        }
        return (T) this.data.get(key);
    }

    /**
     * Retrieve a value by key, returning a default if absent.
     *
     * @param key the storage key
     * @param def the fallback value when the key is missing or the stored value is null
     * @param <T> the expected value type
     * @return the stored value if present and non-null, otherwise {@code def}
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T def) {
        if (key == null) {
            return def;
        }
        Object value = this.data.get(key);
        return value != null ? (T) value : def;
    }

    /**
     * Return the navigation back-stack, creating it on first access.
     *
     * @return the per-player back-stack (never null)
     */
    @SuppressWarnings("unchecked")
    public Deque<Menu> getBackStack() {
        return (Deque<Menu>) this.data.computeIfAbsent(BACK_STACK, k -> new ArrayDeque<Menu>());
    }

    /**
     * Check whether a key exists in this context.
     *
     * @param key the storage key
     * @return {@code true} if the key is present
     */
    public boolean has(String key) {
        return key != null && this.data.containsKey(key);
    }

    /**
     * Remove a single key from this context.
     *
     * @param key the storage key (no-op if null)
     */
    public void remove(String key) {
        if (key != null) {
            this.data.remove(key);
        }
    }

    /**
     * Clear all data including the back-stack.
     */
    public void clear() {
        this.data.clear();
    }

    /**
     * Clear only the navigation back-stack, keeping all other data.
     */
    public void clearBackStack() {
        Deque<Menu> stack = getBackStack();
        stack.clear();
    }

    /**
     * Release all resources held by this context.
     * <p>
     * Called after a player fully closes their last menu.
     * Equivalent to {@link #clear()} but named for semantic clarity.
     */
    public void release() {
        this.data.clear();
    }
}

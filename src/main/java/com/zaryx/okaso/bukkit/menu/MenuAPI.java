package com.zaryx.okaso.bukkit.menu;

import com.zaryx.okaso.bukkit.menu.core.Menu;
import com.zaryx.okaso.bukkit.menu.provider.MenuProvider;
import com.zaryx.okaso.bukkit.menu.provider.MenuProviderRegistry;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Public facade for the menu subsystem.
 * Provides registration, lookup, opening, and cache management operations.
 */
public final class MenuAPI {

    private MenuAPI() {}

    // ---- Registration ----

    /**
     * Register a menu provider under the given ID.
     * @throws IllegalArgumentException if id is null/empty or provider is null
     * @throws IllegalArgumentException if a provider is already registered under this id
     */
    public static void register(String id, MenuProvider provider) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id must not be null or empty");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider must not be null");
        }
        MenuProviderRegistry.getInstance().registerProvider(id, provider);
    }

    /**
     * Unregister a menu provider by ID and clear its cached menus.
     */
    public static void unregister(String id) {
        if (id == null || id.trim().isEmpty()) return;
        MenuProvider provider = get(id);
        if (provider != null) {
            provider.clearCache();
        }
        MenuProviderRegistry.getInstance().unregisterProvider(id);
    }

    // ---- Lookup ----

    /**
     * Get a registered provider by ID, or null if not found.
     */
    public static MenuProvider get(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        return MenuProviderRegistry.getInstance().getProvider(id);
    }

    /**
     * Check if a provider is registered under the given ID.
     */
    public static boolean isRegistered(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        return MenuProviderRegistry.getInstance().isProviderRegistered(id);
    }

    /**
     * Get all registered provider IDs.
     */
    public static Collection<String> registeredIds() {
        return MenuProviderRegistry.getInstance().getRegisteredProviders();
    }

    // ---- Opening menus ----

    /**
     * Open a menu from a registered provider, passing optional parameters.
     * @throws IllegalArgumentException if player is null
     * @throws IllegalStateException if no provider is registered with the given id
     * @throws IllegalStateException if the provider returns a null menu
     */
    public static void open(String id, Player player, Object... params) {
        if (player == null) {
            throw new IllegalArgumentException("player must not be null");
        }
        MenuProvider provider = get(id);
        if (provider == null) {
            throw new IllegalStateException("No menu registered with ID: " + id);
        }
        Menu menu = provider.get(params);
        if (menu == null) {
            throw new IllegalStateException("Menu provider returned null for ID: " + id);
        }
        menu.open(player);
    }

    /**
     * Resolve a menu instance from a registered provider without opening it.
     * @throws IllegalStateException if no provider is registered with the given id
     * @throws IllegalStateException if the provider returns a null menu
     */
    public static Menu resolve(String id, Object... params) {
        MenuProvider provider = get(id);
        if (provider == null) {
            throw new IllegalStateException("No menu registered with ID: " + id);
        }
        Menu menu = provider.get(params);
        if (menu == null) {
            throw new IllegalStateException("Menu provider returned null for ID: " + id);
        }
        return menu;
    }

    /**
     * Open an already-constructed menu for a player.
     * @throws IllegalArgumentException if menu or player is null
     */
    public static void open(Menu menu, Player player) {
        if (menu == null) {
            throw new IllegalArgumentException("menu must not be null");
        }
        if (player == null) {
            throw new IllegalArgumentException("player must not be null");
        }
        menu.open(player);
    }

    // ---- Cache management ----

    /**
     * Invalidate a specific cached menu entry in a provider.
     */
    public static void invalidate(String id, Object... params) {
        MenuProvider provider = get(id);
        if (provider != null) {
            provider.invalidate(new com.zaryx.okaso.bukkit.menu.provider.MenuKey(params));
        }
    }

    /**
     * Clear all cached menus for a specific provider.
     */
    public static void clearCache(String id) {
        MenuProvider provider = get(id);
        if (provider != null) {
            provider.clearCache();
        }
    }

    /**
     * Set the TTL (time-to-live) for cached menus in a provider.
     * @param ttlMillis 0 to disable expiration, &gt;0 to enable
     */
    public static void setCacheTtl(String id, long ttlMillis) {
        MenuProvider provider = get(id);
        if (provider != null) {
            provider.setTtlMillis(ttlMillis);
        }
    }
}

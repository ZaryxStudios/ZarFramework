package com.zaryx.okaso.bukkit.menu;

import com.zaryx.okaso.bukkit.menu.core.Menu;
import com.zaryx.okaso.bukkit.menu.provider.MenuProvider;
import com.zaryx.okaso.bukkit.menu.provider.MenuProviderRegistry;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class MenuAPI {

    private MenuAPI() {}

    public static void register(String id, MenuProvider provider) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id must not be null or empty");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider must not be null");
        }
        MenuProviderRegistry.getInstance().registerProvider(id, provider);
    }

    public static void unregister(String id) {
        if (id == null || id.trim().isEmpty()) return;
        MenuProvider provider = get(id);
        if (provider != null) {
            provider.clearCache();
        }
        MenuProviderRegistry.getInstance().unregisterProvider(id);
    }

    public static MenuProvider get(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        return MenuProviderRegistry.getInstance().getProvider(id);
    }

    public static boolean isRegistered(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        return MenuProviderRegistry.getInstance().isProviderRegistered(id);
    }

    public static Collection<String> registeredIds() {
        return MenuProviderRegistry.getInstance().getRegisteredProviders();
    }

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

    public static void open(Menu menu, Player player) {
        if (menu == null) {
            throw new IllegalArgumentException("menu must not be null");
        }
        if (player == null) {
            throw new IllegalArgumentException("player must not be null");
        }
        menu.open(player);
    }

    public static void invalidate(String id, Object... params) {
        MenuProvider provider = get(id);
        if (provider != null) {
            provider.invalidate(new com.zaryx.okaso.bukkit.menu.provider.MenuKey(params));
        }
    }

    public static void clearCache(String id) {
        MenuProvider provider = get(id);
        if (provider != null) {
            provider.clearCache();
        }
    }

    public static void setCacheTtl(String id, long ttlMillis) {
        MenuProvider provider = get(id);
        if (provider != null) {
            provider.setTtlMillis(ttlMillis);
        }
    }
}

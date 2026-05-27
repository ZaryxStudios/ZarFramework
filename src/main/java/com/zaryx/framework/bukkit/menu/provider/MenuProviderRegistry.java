package com.zaryx.framework.bukkit.menu.provider;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for menu providers. Handles registration, lookup, and periodic cache cleanup.
 * Automatically clears all providers and caches when the owning plugin disables.
 */
public class MenuProviderRegistry {
    private static MenuProviderRegistry instance;
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<String, MenuProvider> providers;
    private final CopyOnWriteArrayList<String> registered;

    public MenuProviderRegistry(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.providers = new ConcurrentHashMap<>();
        this.registered = new CopyOnWriteArrayList<>();
        Bukkit.getPluginManager().registerEvents(new ProviderHandler(), plugin);

        // Periodic cleanup: purge expired entries from all providers every 60 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                for (MenuProvider p : providers.values()) {
                    try {
                        p.purgeExpired();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error purging expired menu provider cache", e);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error during menu provider cleanup cycle", e);
            }
        }, 1200L, 1200L);
    }

    public static MenuProviderRegistry getInstance() {
        return instance;
    }

    /**
     * Register a provider. Throws if a provider with the same key already exists.
     */
    public void registerProvider(String name, MenuProvider provider) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name must not be null or empty");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Provider must not be null");
        }
        String key = name.trim().toLowerCase();
        MenuProvider prev = providers.putIfAbsent(key, provider);
        if (prev != null) {
            throw new IllegalArgumentException("Provider already registered: " + key);
        }
        registered.add(key);
        logger.fine("Menu provider registered: " + key);
    }

    /**
     * Unregister a provider and clear its cache.
     */
    public void unregisterProvider(String name) {
        if (name == null) return;
        String key = name.trim().toLowerCase();
        MenuProvider removed = providers.remove(key);
        if (removed != null) {
            removed.clearCache();
        }
        registered.remove(key);
    }

    public MenuProvider getProvider(String name) {
        if (name == null) return null;
        return providers.get(name.trim().toLowerCase());
    }

    public Collection<String> getRegisteredProviders() {
        return Collections.unmodifiableList(registered);
    }

    public boolean isProviderRegistered(String name) {
        if (name == null) return false;
        return registered.contains(name.trim().toLowerCase());
    }

    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Clear all providers and their caches. Called on plugin disable.
     */
    public void clear() {
        for (MenuProvider provider : providers.values()) {
            try {
                provider.clearCache();
            } catch (Exception ignored) {}
        }
        providers.clear();
        registered.clear();
    }

    private class ProviderHandler implements Listener {
        @EventHandler
        public void onPluginDisable(PluginDisableEvent event) {
            if (event.getPlugin() == plugin) {
                clear();
                instance = null;
            }
        }
    }
}

package com.zaryx.framework.bukkit.menu.provider;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MenuProviderRegistry {
    private static MenuProviderRegistry instance;
    private final JavaPlugin plugin;
    private final Map<String, MenuProvider> providers;
    private final List<String> registered;

    public MenuProviderRegistry(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.providers = new ConcurrentHashMap<>();
        this.registered = new CopyOnWriteArrayList<>();
        Bukkit.getPluginManager().registerEvents(new ProviderHandler(), plugin);
    }

    public static MenuProviderRegistry getInstance() {
        return instance;
    }

    public void registerProvider(String name, MenuProvider provider) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name must not be null or empty");
        }
        String key = name.trim().toLowerCase();
        MenuProvider prev = providers.putIfAbsent(key, provider);
        if (prev != null) {
            throw new IllegalArgumentException("Provider already registered: " + key);
        }
        registered.add(key);
    }

    public void unregisterProvider(String name) {
        if (name == null) return;
        String key = name.trim().toLowerCase();
        providers.remove(key);
        registered.remove(key);
    }

    public MenuProvider getProvider(String name) {
        if (name == null) return null;
        return providers.get(name.trim().toLowerCase());
    }

    public Collection<String> getRegisteredProviders() {
        return java.util.Collections.unmodifiableList(registered);
    }

    public boolean isProviderRegistered(String name) {
        if (name == null) return false;
        return registered.contains(name.trim().toLowerCase());
    }

    public int getProviderCount() {
        return providers.size();
    }

    public void clear() {
        providers.clear();
        registered.clear();
    }

    public class ProviderHandler implements Listener {
        @EventHandler
        public void onPluginDisable(PluginDisableEvent event) {
            if (event.getPlugin() == plugin) {
                clear();
            }
        }
    }
}

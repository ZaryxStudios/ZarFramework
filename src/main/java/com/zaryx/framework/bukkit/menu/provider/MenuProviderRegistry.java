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
        if (providers.containsKey(name)) {
            throw new IllegalArgumentException("Provider already registered: " + name);
        }
        providers.put(name, provider);
        registered.add(name);
    }

    public void unregisterProvider(String name) {
        providers.remove(name);
        registered.remove(name);
    }

    public MenuProvider getProvider(String name) {
        return providers.get(name);
    }

    public Collection<String> getRegisteredProviders() {
        return registered;
    }

    public boolean isProviderRegistered(String name) {
        return registered.contains(name);
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
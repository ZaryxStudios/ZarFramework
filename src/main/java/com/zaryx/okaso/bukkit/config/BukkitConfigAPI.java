package com.zaryx.okaso.bukkit.config;

import com.zaryx.okaso.bukkit.config.core.ConfigManager;
import org.bukkit.plugin.Plugin;

public final class BukkitConfigAPI {

    private BukkitConfigAPI() {}

    public static void load(Class<?> clazz, Plugin plugin) {
        ConfigManager.getInstance().load(clazz, plugin);
    }

    public static void save(Class<?> clazz) {
        ConfigManager.getInstance().save(clazz);
    }

    public static void reload(Class<?> clazz, Plugin plugin) {
        ConfigManager.getInstance().reload(clazz, plugin);
    }
}

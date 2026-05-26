package com.zaryx.framework.bungee.config;

import com.zaryx.framework.bungee.config.core.BungeeConfigManager;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeeConfigAPI {

    private BungeeConfigAPI() {}

    public static void load(Class<?> clazz, Plugin plugin) {
        BungeeConfigManager.getInstance().load(clazz, plugin);
    }

    public static void save(Class<?> clazz) {
        BungeeConfigManager.getInstance().save(clazz);
    }

    public static void reload(Class<?> clazz, Plugin plugin) {
        BungeeConfigManager.getInstance().reload(clazz, plugin);
    }
}

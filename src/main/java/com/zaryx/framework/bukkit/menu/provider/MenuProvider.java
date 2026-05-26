package com.zaryx.framework.bukkit.menu.provider;

import com.zaryx.framework.bukkit.menu.core.Menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class MenuProvider {

    private final java.util.concurrent.ConcurrentMap<MenuKey, Menu> cache = new java.util.concurrent.ConcurrentHashMap<>();

    protected abstract Menu create(MenuKey parameter);

    public Menu get(Object... parameter) {
        MenuKey key = new MenuKey(parameter);
        Menu m = cache.get(key);
        if (m != null) return m;
        Menu created = create(key);
        Menu existing = cache.putIfAbsent(key, created);
        return existing != null ? existing : created;
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

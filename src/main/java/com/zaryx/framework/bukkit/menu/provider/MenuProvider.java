package com.zaryx.framework.bukkit.menu.provider;

import com.zaryx.framework.bukkit.menu.core.Menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class MenuProvider {

    private final Map<MenuKey, Menu> cache = new HashMap<>();

    protected abstract Menu create(MenuKey parameter);

    public Menu get(Object... parameter) {
        MenuKey key = new MenuKey(parameter);
        return this.cache.computeIfAbsent(key, this::create);
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
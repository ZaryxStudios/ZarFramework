package com.zaryx.framework.bukkit.menu.adapter;

import com.zaryx.framework.bukkit.menu.core.Menu;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MenuContext {

    private final Map<String, Object> data = new ConcurrentHashMap<>();
    public static final String NAVIGATING = "menu:navigating";
    public static final String BACK_STACK = "menu:back_stack";

    public <T> void set(String key, T value) {
        this.data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key == null) {
            return null;
        }
        return (T) this.data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T def) {
        if (key == null) {
            return def;
        }
        Object value = this.data.get(key);
        return value != null ? (T) value : def;
    }

    @SuppressWarnings("unchecked")
    public Deque<Menu> getBackStack() {
        return (Deque<Menu>) this.data.computeIfAbsent(BACK_STACK, k -> new ArrayDeque<Menu>());
    }

    public boolean has(String key) {
        return key != null && this.data.containsKey(key);
    }

    public void remove(String key) {
        if (key != null) {
            this.data.remove(key);
        }
    }

    public void clear() {
        this.data.clear();
    }

    public void clearBackStack() {
        getBackStack().clear();
    }
}

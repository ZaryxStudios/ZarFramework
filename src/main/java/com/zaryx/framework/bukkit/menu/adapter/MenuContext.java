package com.zaryx.framework.bukkit.menu.adapter;

import com.zaryx.framework.bukkit.menu.core.Menu;
import lombok.Getter;

import java.util.*;

@Getter
public class MenuContext {

    private final Map<String, Object> data = new HashMap<>();
    public static final String NAVIGATING = "menu:navigating";
    public static final String BACK_STACK = "menu:back_stack";

    public <T> void set(String key, T value) {
        this.data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) this.data.getOrDefault(key, 0);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T def) {
        Object value = this.data.get(key);
        return value != null ? (T) value : def;
    }

    @SuppressWarnings("unchecked")
    public Deque<Menu> getBackStack() {
        return (Deque<Menu>) this.data.computeIfAbsent(
                BACK_STACK, k ->  new ArrayDeque<Menu>());
    }

    public boolean has(String key) {
        return this.data.containsKey(key);
    }

    public void remove(String key) {
        this.data.remove(key);
    }

    public void clear() {
        this.data.clear();
    }

    public void clearBackStack() {
        this.data.remove(BACK_STACK);
    }
}
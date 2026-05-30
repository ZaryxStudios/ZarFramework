package com.zaryx.okaso.bukkit.command.extra;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CommandContext {

    private final CommandSender sender;
    private final Map<String, Object> values;

    public CommandContext(CommandSender sender) {
        this.sender = sender;
        this.values = new ConcurrentHashMap<>();
    }

    public CommandSender getSender() { return sender; }

    public boolean isPlayer() { return sender instanceof Player; }

    public Player getPlayer() { return isPlayer() ? (Player) sender : null; }

    public void put(String key, Object value) {
        if (key != null) values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return key != null ? (T) values.get(key) : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        if (key == null) return defaultValue;
        Object v = values.get(key);
        return v != null ? (T) v : defaultValue;
    }

    public boolean has(String key) {
        return key != null && values.containsKey(key);
    }

    public void remove(String key) {
        if (key != null) values.remove(key);
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    public int size() { return values.size(); }

    public void clear() { values.clear(); }
}

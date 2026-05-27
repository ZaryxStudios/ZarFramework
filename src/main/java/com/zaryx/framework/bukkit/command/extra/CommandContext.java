package com.zaryx.framework.bukkit.command.extra;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-command-execution context holding the sender and parsed arguments.
 * Thread-safe via ConcurrentHashMap.
 */
public final class CommandContext {

    private final CommandSender sender;
    private final Map<String, Object> values;

    public CommandContext(CommandSender sender) {
        this.sender = sender;
        this.values = new ConcurrentHashMap<>();
    }

    /** The command sender (player or console). */
    public CommandSender getSender() { return sender; }

    /** Check if the sender is a Player. */
    public boolean isPlayer() { return sender instanceof Player; }

    /** Get the sender as a Player, or null if not a player. */
    public Player getPlayer() { return isPlayer() ? (Player) sender : null; }

    /** Store a value in the context. Null keys are ignored. */
    public void put(String key, Object value) {
        if (key != null) values.put(key, value);
    }

    /**
     * Retrieve a typed value from the context.
     * @return the value, or null if the key is null or not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return key != null ? (T) values.get(key) : null;
    }

    /**
     * Retrieve a typed value, falling back to a default.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        if (key == null) return defaultValue;
        Object v = values.get(key);
        return v != null ? (T) v : defaultValue;
    }

    /** Check if a key exists in the context. */
    public boolean has(String key) {
        return key != null && values.containsKey(key);
    }

    /** Remove a key from the context. */
    public void remove(String key) {
        if (key != null) values.remove(key);
    }

    /** Get all keys currently stored. */
    public Set<String> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    /** Number of stored values. */
    public int size() { return values.size(); }

    /** Clear all stored values. */
    public void clear() { values.clear(); }
}

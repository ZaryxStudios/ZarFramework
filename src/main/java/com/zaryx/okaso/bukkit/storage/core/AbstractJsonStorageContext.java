package com.zaryx.okaso.bukkit.storage.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractJsonStorageContext implements StorageContext {

    private final Gson gson;
    private boolean connected;

    protected AbstractJsonStorageContext() {
        this(new GsonBuilder().serializeNulls().disableHtmlEscaping().create());
    }

    protected AbstractJsonStorageContext(Gson gson) {
        this.gson = gson;
        this.connected = false;
    }

    @Override
    public boolean connect() {
        this.connected = true;
        return true;
    }

    @Override
    public void disconnect() {
        this.connected = false;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public <T> boolean save(String key, T value) {
        this.requireKey(key);
        this.writeJson(key, this.gson.toJson(value));
        return true;
    }

    @Override
    public <T> T load(String key, Class<T> clazz) {
        this.requireKey(key);
        String json = this.readJson(key);
        return json == null ? null : this.gson.fromJson(json, clazz);
    }

    @Override
    public <T> T load(String key, Type type) {
        this.requireKey(key);
        String json = this.readJson(key);
        return json == null ? null : this.gson.fromJson(json, type);
    }

    @Override
    public boolean exists(String key) {
        this.requireKey(key);
        return this.existsJson(key);
    }

    @Override
    public boolean delete(String key) {
        this.requireKey(key);
        return this.deleteJson(key);
    }

    @Override
    public Set<String> getAllKeys() {
        return this.snapshotKeys();
    }

    @Override
    public void clear() {
        for (String key : this.snapshotKeys()) {
            this.deleteJson(key);
        }
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("connected", this.connected);
        stats.put("type", this.getType());
        stats.put("keys", this.snapshotKeys().size());
        return stats;
    }

    protected abstract void writeJson(String key, String json);

    protected abstract String readJson(String key);

    protected abstract boolean existsJson(String key);

    protected abstract boolean deleteJson(String key);

    protected abstract Set<String> snapshotKeys();

    protected Gson getGson() {
        return this.gson;
    }

    protected void requireKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }
}

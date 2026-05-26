package com.zaryx.framework.bukkit.storage.core;

import java.lang.reflect.Type;

public interface StorageContext {
    <T> void save(String key, T value);
    <T> T load(String key, Class<T> clazz);
    <T> T load(String key, Type type);
    boolean exists(String key);
    void delete(String key);
}
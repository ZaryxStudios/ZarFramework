package com.zaryx.framework.bukkit.storage.core;

import com.zaryx.framework.core.storage.StorageProvider;

import java.lang.reflect.Type;

public interface StorageContext extends StorageProvider {
    <T> T load(String key, Type type);
}

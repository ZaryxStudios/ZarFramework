package com.zaryx.okaso.bukkit.storage.core;

import com.zaryx.okaso.core.storage.StorageProvider;

import java.lang.reflect.Type;

public interface StorageContext extends StorageProvider {
    <T> T load(String key, Type type);
}

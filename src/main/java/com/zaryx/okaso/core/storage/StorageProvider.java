package com.zaryx.okaso.core.storage;

import java.util.*;

public interface StorageProvider {

    boolean connect();

    void disconnect();

    boolean isConnected();

    <T> boolean save(String key, T value);

    <T> T load(String key, Class<T> type);

    boolean exists(String key);

    boolean delete(String key);

    Set<String> getAllKeys();

    void clear();

    String getType();

    Map<String, Object> getStats();
}

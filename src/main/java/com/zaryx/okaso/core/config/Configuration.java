package com.zaryx.okaso.core.config;

import java.util.*;

public interface Configuration {

    String getString(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    double getDouble(String key, double defaultValue);

    List<String> getStringList(String key);

    Map<String, Object> getMap(String key);

    void set(String key, Object value);

    Set<String> getKeys();

    boolean contains(String key);

    String getName();

    void save();

    void reload();

    Set<String> getSections();
}

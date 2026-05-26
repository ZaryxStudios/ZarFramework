package com.zaryx.framework.bukkit.placeholder;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PlaceholderContext {

    private final Map<String, String> values = new HashMap<>();
    
    public PlaceholderContext add(String key, Object value) {
        this.values.put(key, String.valueOf(value));
        return this;
    }

    public Map<String, String> getValues() {
        return this.values;
    }

}

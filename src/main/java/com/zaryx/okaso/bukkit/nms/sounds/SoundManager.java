package com.zaryx.okaso.bukkit.nms.sounds;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SoundManager {

    private static final Map<String, Float> categoryVolumes = new HashMap<String, Float>();

    private SoundManager() {}

    public static void setCategoryVolume(String category, float volume) {
        if (category == null) return;
        float v = Math.max(0f, Math.min(1f, volume));
        categoryVolumes.put(category.toUpperCase(), v);
    }

    public static float getCategoryVolume(String category) {
        if (category == null) return 1.0f;
        Float v = categoryVolumes.get(category.toUpperCase());
        return v == null ? 1.0f : v;
    }

    public static float applyCategoryVolume(float baseVolume, String category) {
        return baseVolume * getCategoryVolume(category);
    }

    public static Map<String, Float> getAllVolumes() {
        return Collections.unmodifiableMap(categoryVolumes);
    }
}

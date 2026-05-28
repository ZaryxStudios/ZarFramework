package com.zaryx.framework.bukkit.nms.sounds;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Category abstraction that works even when SoundCategory does not exist.
 */
public class SoundCategoryManager {

    private static final Logger LOGGER = Logger.getLogger(SoundCategoryManager.class.getName());

    private static final Map<UUID, Float> PLAYER_VOLUMES = new HashMap<UUID, Float>();
    private final String category;

    public SoundCategoryManager(String category) {
        this.category = category == null ? "MASTER" : category.toUpperCase();
    }

    public static void setGlobalVolume(String category, float volume) {
        SoundCategoryManager manager = new SoundCategoryManager(category);
        for (Player player : Bukkit.getOnlinePlayers()) {
            manager.setPlayerVolume(player, volume);
        }
    }

    public void setPlayerVolume(Player player, float volume) {
        if (player == null) {
            return;
        }

        float normalized = Math.max(0.0f, Math.min(1.0f, volume));
        PLAYER_VOLUMES.put(player.getUniqueId(), normalized);

        // Player#setVolume(category, x) does not exist on old APIs and is optional.
        try {
            Class<?> categoryEnum = Class.forName("org.bukkit.SoundCategory");
            Object enumValue = Enum.valueOf((Class<Enum>) categoryEnum.asSubclass(Enum.class), category);
            Method setVolume = player.getClass().getMethod("setVolume", categoryEnum, float.class);
            setVolume.invoke(player, enumValue, normalized);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Sound category volume is not supported on this server API.", e);
        }
    }

    public float getPlayerVolume(Player player) {
        if (player == null) {
            return 1.0f;
        }
        Float value = PLAYER_VOLUMES.get(player.getUniqueId());
        return value == null ? 1.0f : value;
    }

    public boolean isMuted(Player player) {
        return getPlayerVolume(player) <= 0.0f;
    }

    public void mutePlayer(Player player) {
        setPlayerVolume(player, 0.0f);
    }

    public void unmutePlayer(Player player) {
        setPlayerVolume(player, 1.0f);
    }

    public static Collection<String> getAllCategories() {
        try {
            Class<?> categoryEnum = Class.forName("org.bukkit.SoundCategory");
            Object[] values = (Object[]) categoryEnum.getMethod("values").invoke(null);
            List<String> names = new ArrayList<String>(values.length);
            for (Object value : values) {
                names.add(String.valueOf(value));
            }
            return names;
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Sound categories are not available on this server API.", e);
            return Collections.singleton("MASTER");
        }
    }
}

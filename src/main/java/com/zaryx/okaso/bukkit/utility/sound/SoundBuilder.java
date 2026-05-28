package com.zaryx.okaso.bukkit.utility.sound;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fluent sound utility with legacy-safe fallbacks.
 */
public class SoundBuilder {

    private static final Logger LOGGER = Logger.getLogger(SoundBuilder.class.getName());

    private Sound sound = resolveSound("CLICK", "UI_BUTTON_CLICK");
    private float volume = 1.0f;
    private float pitch = 1.0f;
    private String category = "MASTER";

    public static SoundBuilder of(String soundName) {
        SoundBuilder builder = new SoundBuilder();
        builder.sound = resolveSound(soundName);
        return builder;
    }

    public static SoundBuilder of(Sound sound) {
        SoundBuilder builder = new SoundBuilder();
        builder.sound = sound;
        return builder;
    }

    public SoundBuilder sound(Sound sound) {
        this.sound = sound;
        return this;
    }

    public SoundBuilder volume(float volume) {
        this.volume = clamp(volume, 0.0f, 2.0f);
        return this;
    }

    public SoundBuilder pitch(float pitch) {
        this.pitch = clamp(pitch, 0.5f, 2.0f);
        return this;
    }

    public SoundBuilder category(String category) {
        this.category = category == null ? "MASTER" : category.toUpperCase();
        return this;
    }

    public void play(Player player) {
        if (player == null || sound == null) {
            return;
        }

        if (!playWithCategory(player, player.getLocation())) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public void play(Location location) {
        if (location == null || location.getWorld() == null || sound == null) {
            return;
        }

        if (!playWithCategory(location.getWorld(), location)) {
            location.getWorld().playSound(location, sound, volume, pitch);
        }
    }

    public void playToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            play(player);
        }
    }

    public static SoundBuilder click() { return of(resolveSound("CLICK", "UI_BUTTON_CLICK")); }
    public static SoundBuilder success() { return of(resolveSound("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP")); }
    public static SoundBuilder error() { return of(resolveSound("VILLAGER_NO", "ENTITY_VILLAGER_NO")); }
    public static SoundBuilder levelUp() { return of(resolveSound("LEVEL_UP", "ENTITY_PLAYER_LEVELUP")); }

    private static Sound resolveSound(String... names) {
        if (names == null) {
            return fallbackSound();
        }

        for (String name : names) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            try {
                return Sound.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return fallbackSound();
    }

    private static Sound fallbackSound() {
        Sound[] values = Sound.values();
        return values.length == 0 ? null : values[0];
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean playWithCategory(Object target, Location location) {
        try {
            Class<?> categoryEnum = Class.forName("org.bukkit.SoundCategory");
            Object enumValue = Enum.valueOf((Class<Enum>) categoryEnum.asSubclass(Enum.class), category);

            Method method;
            if (target instanceof Player) {
                method = target.getClass().getMethod("playSound", Location.class, Sound.class, categoryEnum, float.class, float.class);
                method.invoke(target, location, sound, enumValue, volume, pitch);
            } else {
                method = target.getClass().getMethod("playSound", Location.class, Sound.class, categoryEnum, float.class, float.class);
                method.invoke(target, location, sound, enumValue, volume, pitch);
            }
            return true;
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to play sound with category", t);
            return false;
        }
    }
}

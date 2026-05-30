package com.zaryx.okaso.bukkit.nms.sounds;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.zaryx.okaso.bukkit.OkasoPlugin;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NmsSound {

    private static final Logger LOGGER = Logger.getLogger(NmsSound.class.getName());

    private static final java.util.Map<String, String> LEGACY_TO_MODERN = new java.util.HashMap<>();

    static {
        LEGACY_TO_MODERN.put("CLICK", "UI_BUTTON_CLICK");
        LEGACY_TO_MODERN.put("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP");
        LEGACY_TO_MODERN.put("LEVEL_UP", "ENTITY_PLAYER_LEVELUP");
        LEGACY_TO_MODERN.put("VILLAGER_NO", "ENTITY_VILLAGER_NO");
        LEGACY_TO_MODERN.put("VILLAGER_YES", "ENTITY_VILLAGER_YES");
        LEGACY_TO_MODERN.put("VILLAGER_HAGGLE", "ENTITY_VILLAGER_TRADE");
        LEGACY_TO_MODERN.put("ANVIL_BREAK", "ENTITY_ITEM_BREAK");
        LEGACY_TO_MODERN.put("ANVIL_USE", "BLOCK_ANVIL_USE");
        LEGACY_TO_MODERN.put("FURNACE_FIRE_CRACKLE", "BLOCK_FURNACE_FIRE_CRACKLE");
    }

    private final String soundName;
    private final Sound bukkitSound;
    private float volume;
    private float pitch;
    private String category;
    private int delay;

    public NmsSound(String soundName) {
        this(soundName, 1.0f, 1.0f);
    }

    public NmsSound(String soundName, float volume, float pitch) {
        this.soundName = soundName;
        this.bukkitSound = resolveBukkitSound(soundName);
        this.volume = clamp(volume, 0.0f, 2.0f);
        this.pitch = clamp(pitch, 0.5f, 2.0f);
        this.category = "MASTER";
        this.delay = 0;
    }

    public NmsSound volume(float vol) {
        this.volume = clamp(vol, 0.0f, 2.0f);
        return this;
    }

    public NmsSound pitch(float p) {
        this.pitch = clamp(p, 0.5f, 2.0f);
        return this;
    }

    public NmsSound category(String cat) {
        this.category = cat == null ? "MASTER" : cat.toUpperCase();
        return this;
    }

    public NmsSound delay(int ticks) {
        this.delay = Math.max(0, ticks);
        return this;
    }

    public NmsSound with(float v, float p) {
        NmsSound copy = new NmsSound(soundName, v, p);
        copy.category = this.category;
        copy.delay = this.delay;
        return copy;
    }

    public void play(Player player) {
        if (player == null || bukkitSound == null) return;
        schedulePlay(() -> executePlay(player, player.getLocation()));
    }

    public void play(Player player, String cat) {
        if (player == null) return;
        String oldCat = this.category;
        this.category = cat == null ? "MASTER" : cat.toUpperCase();
        play(player);
        this.category = oldCat;
    }

    public void play(Player player, int times) {
        for (int i = 0; i < Math.max(1, times); i++) {
            final int idx = i;
            final Player p = player;
            Bukkit.getScheduler().runTaskLater(
                    OkasoPlugin.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            if (p != null && p.isOnline()) {
                                executePlay(p, p.getLocation());
                            }
                        }
                    }, (long) delay * idx
            );
        }
    }

    public void play(Location location) {
        if (location == null || location.getWorld() == null || bukkitSound == null) return;
        schedulePlay(() -> executePlay(null, location));
    }

    public void playToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            play(player);
        }
    }

    public void playInRadius(Location center, double radius) {
        if (center == null || center.getWorld() == null || bukkitSound == null) return;
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= radius) {
                executePlay(player, center);
            }
        }
    }

    public void stop(Player player) {
        if (player == null || bukkitSound == null) return;
        try {
            Method stopSound = player.getClass().getMethod("stopSound", Sound.class);
            stopSound.invoke(player, bukkitSound);
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to stop sound for " + player.getName(), t);
        }
    }

    public void stop(Player player, String cat) {
        if (player == null || bukkitSound == null) return;
        try {
            Class<?> categoryEnum = Class.forName("org.bukkit.SoundCategory");
            Object enumValue = Enum.valueOf((Class<Enum>) categoryEnum.asSubclass(Enum.class), cat == null ? "MASTER" : cat.toUpperCase());
            Method stopSound = player.getClass().getMethod("stopSound", Sound.class, categoryEnum);
            stopSound.invoke(player, bukkitSound, enumValue);
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to stop sound with category for " + player.getName(), t);
            stop(player);
        }
    }

    public String getSoundName() { return soundName; }
    public Sound getBukkitSound() { return bukkitSound; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public String getCategory() { return category; }
    public int getDelay() { return delay; }

    public static Set<String> getAvailableSounds() {
        Set<String> sounds = new HashSet<String>();
        for (Sound sound : Sound.values()) {
            sounds.add(sound.name());
        }
        return sounds;
    }

    public static boolean exists(String name) {
        return resolveBukkitSound(name) != null;
    }

    public static boolean existsLegacyOrModern(String name) {
        if (exists(name)) return true;
        String modern = LEGACY_TO_MODERN.get(name.toUpperCase());
        return modern != null && exists(modern);
    }

    private void schedulePlay(Runnable task) {
        if (delay > 0) {
            Bukkit.getScheduler().runTaskLater(
                    OkasoPlugin.getInstance(), task, (long) delay
            );
        } else {
            task.run();
        }
    }

    private void executePlay(Player player, Location location) {
        if (bukkitSound == null) return;
        float effectiveVolume = SoundManager.applyCategoryVolume(this.volume, this.category);
        if (!playWithCategory(player, location, effectiveVolume)) {
            if (player != null) {
                player.playSound(location, bukkitSound, effectiveVolume, pitch);
            } else if (location.getWorld() != null) {
                location.getWorld().playSound(location, bukkitSound, effectiveVolume, pitch);
            }
        }
    }

    private boolean playWithCategory(Player player, Location location, float effectiveVolume) {
        try {
            Class<?> categoryEnum = Class.forName("org.bukkit.SoundCategory");
            Object enumValue = Enum.valueOf((Class<Enum>) categoryEnum.asSubclass(Enum.class), category);

            if (player != null) {
                Method playerPlay = player.getClass().getMethod("playSound", Location.class, Sound.class, categoryEnum, float.class, float.class);
                playerPlay.invoke(player, location, bukkitSound, enumValue, effectiveVolume, pitch);
            } else if (location.getWorld() != null) {
                Method worldPlay = location.getWorld().getClass().getMethod("playSound", Location.class, Sound.class, categoryEnum, float.class, float.class);
                worldPlay.invoke(location.getWorld(), location, bukkitSound, enumValue, effectiveVolume, pitch);
            }
            return true;
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to play sound with category", t);
            return false;
        }
    }

    private static Sound resolveBukkitSound(String name) {
        if (name == null || name.trim().isEmpty()) return fallbackSound();
        String upper = name.toUpperCase();

        try { return Sound.valueOf(upper); } catch (IllegalArgumentException ignored) {}

        String modern = LEGACY_TO_MODERN.get(upper);
        if (modern != null) {
            try { return Sound.valueOf(modern); } catch (IllegalArgumentException ignored) {}
        }

        String normalized = upper.replace("MINECRAFT:", "").replace('.', '_').replace('-', '_');
        try { return Sound.valueOf(normalized); } catch (IllegalArgumentException ignored) {}

        String[] prefixes = {"ENTITY_", "BLOCK_", "UI_"};
        for (String prefix : prefixes) {
            try { return Sound.valueOf(prefix + normalized); } catch (IllegalArgumentException ignored) {}
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
}

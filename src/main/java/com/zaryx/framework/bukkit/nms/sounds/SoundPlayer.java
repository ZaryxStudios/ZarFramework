package com.zaryx.framework.bukkit.nms.sounds;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.zaryx.framework.bukkit.FrameworkPlugin;

/**
 * Utility to play sounds with optional delay/repeat and category fallback.
 */
public class SoundPlayer {

    private final NmsSound sound;
    private float volume = 1.0f;
    private float pitch = 1.0f;
    private String category = "MASTER";
    private int delay = 0;

    public SoundPlayer(NmsSound sound) {
        this.sound = sound;
    }

    public SoundPlayer(String soundName) {
        this.sound = new NmsSound(soundName);
    }

    public SoundPlayer volume(float volume) {
        this.volume = volume;
        return this;
    }

    public SoundPlayer pitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public SoundPlayer category(String category) {
        this.category = category == null ? "MASTER" : category;
        return this;
    }

    public SoundPlayer delay(int ticks) {
        this.delay = Math.max(0, ticks);
        return this;
    }

    public void play(final Player player) {
        if (player == null) {
            return;
        }

        Runnable task = new Runnable() {
            @Override
            public void run() {
                sound.with(volume, pitch).category(category).delay(delay).play(player);
            }
        };

        if (delay > 0) {
            Bukkit.getScheduler().runTaskLater(FrameworkPlugin.getInstance(), task, delay);
        } else {
            task.run();
        }
    }

    public void play(Player player, int times) {
        for (int i = 0; i < Math.max(1, times); i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(
                    FrameworkPlugin.getInstance(),
                    new Runnable() {
                        @Override
                        public void run() {
                            sound.with(volume, pitch).category(category).delay(delay).play(player);
                        }
                    },
                    delay * index
            );
        }
    }

    public void play(Location location) {
        // configure the sound and play at location
        sound.with(volume, pitch).category(category).delay(delay).play(location);
    }

    public void playToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            play(player);
        }
    }

    public static SoundPlayer click() { return new SoundPlayer("CLICK"); }
    public static SoundPlayer success() { return new SoundPlayer("ORB_PICKUP"); }
    public static SoundPlayer error() { return new SoundPlayer("VILLAGER_NO"); }
    public static SoundPlayer levelUp() { return new SoundPlayer("LEVEL_UP"); }
}

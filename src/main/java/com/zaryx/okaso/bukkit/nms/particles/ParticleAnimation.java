package com.zaryx.okaso.bukkit.nms.particles;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Particle animation sequences.
 */
public class ParticleAnimation {

    private final List<ParticleKeyframe> keyframes = new ArrayList<>();
    private boolean loop = false;
    private int repeatCount = 1;

    public ParticleAnimation addKeyframe(int tick, ParticleEffect effect) {
        keyframes.add(new ParticleKeyframe(tick, effect));
        return this;
    }

    public ParticleAnimation addKeyframe(int tick, ParticleEffect effect, Location offset) {
        keyframes.add(new ParticleKeyframe(tick, effect, offset));
        return this;
    }

    public ParticleAnimation loop() {
        this.loop = true;
        return this;
    }

    public ParticleAnimation repeat(int count) {
        this.repeatCount = count;
        return this;
    }

    /**
     * Play the animation at location.
     */
    public void play(Location location) {
        int totalTicks = keyframes.isEmpty() ? 0 : keyframes.get(keyframes.size() - 1).tick;
        int iterations = loop ? Integer.MAX_VALUE : repeatCount;

        for (int iter = 0; iter < iterations; iter++) {
            for (ParticleKeyframe frame : keyframes) {
                scheduleFrame(location, frame);
            }
        }
    }

    private void scheduleFrame(Location baseLocation, ParticleKeyframe frame) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            com.zaryx.okaso.bukkit.OkasoPlugin.getInstance(), () -> {
                Location loc = baseLocation.clone();
                if (frame.offset != null) {
                    loc.add(frame.offset);
                }
                frame.effect.display(loc);
            }, frame.tick
        );
    }

    /**
     * Create a spiral animation.
     */
    public static ParticleAnimation spiral(ParticleEffect particle, int height, int radius, int duration) {
        ParticleAnimation animation = new ParticleAnimation();
        int steps = 20;
        int tickStep = duration / steps;

        for (int i = 0; i < steps; i++) {
            double angle = (2 * Math.PI * i) / steps;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = (double) i * height / steps;

            final int tick = i * tickStep;
            animation.addKeyframe(tick, particle, new org.bukkit.Location(null, x, y, z));
        }

        return animation;
    }

    /**
     * Create a pulse animation.
     */
    public static ParticleAnimation pulse(ParticleEffect particle, int count, int duration) {
        ParticleAnimation animation = new ParticleAnimation();
        int tickStep = duration / count;

        for (int i = 0; i < count; i++) {
            final int tick = i * tickStep;
            animation.addKeyframe(tick, particle);
        }

        return animation;
    }

    /**
     * Create a ring animation.
     */
    public static ParticleAnimation ring(ParticleEffect particle, int radius, int particles) {
        ParticleAnimation animation = new ParticleAnimation();

        for (int i = 0; i < particles; i++) {
            double angle = (2 * Math.PI * i) / particles;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            animation.addKeyframe(0, particle, new org.bukkit.Location(null, x, 0, z));
        }

        return animation;
    }

    /**
     * Create a fountain animation.
     */
    public static ParticleAnimation fountain(ParticleEffect particle, int height, int particles) {
        ParticleAnimation animation = new ParticleAnimation();

        for (int i = 0; i < particles; i++) {
            double progress = (double) i / particles;
            double y = height * Math.sin(progress * Math.PI);
            double spread = progress * 2;

            animation.addKeyframe(i, particle, new org.bukkit.Location(null, (Math.random() - 0.5) * spread, y, (Math.random() - 0.5) * spread
                )
            );
        }

        return animation;
    }

    private static class ParticleKeyframe {
        final int tick;
        final ParticleEffect effect;
        final Location offset;

        ParticleKeyframe(int tick, ParticleEffect effect) {
            this(tick, effect, null);
        }

        ParticleKeyframe(int tick, ParticleEffect effect, Location offset) {
            this.tick = tick;
            this.effect = effect;
            this.offset = offset;
        }
    }
}

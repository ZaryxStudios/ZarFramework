package com.zaryx.framework.bukkit.nms.particles;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Particle abstraction that works on legacy (Effect) and modern (Particle API) servers.
 */
public class ParticleEffect {

    private final String particleName;
    private final int count;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;

    public ParticleEffect(String particleName) {
        this(particleName, 1, 0f, 0f, 0f);
    }

    public ParticleEffect(String particleName, int count, float offsetX, float offsetY, float offsetZ) {
        this.particleName = particleName == null ? "SMOKE" : particleName;
        this.count = Math.max(1, count);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public void display(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        if (spawnModern(location, null)) {
            return;
        }

        // Legacy fallback (1.7/1.8): only basic effect support.
        for (int i = 0; i < count; i++) {
            location.getWorld().playEffect(location, Effect.SMOKE, 0);
        }
    }

    public void display(Player player, Location location) {
        if (player == null || location == null) {
            return;
        }

        if (!spawnModern(location, player)) {
            player.playEffect(location, Effect.SMOKE, 0);
        }
    }

    private boolean spawnModern(Location location, Player player) {
        try {
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            Object particleEnum = Enum.valueOf((Class<Enum>) particleClass.asSubclass(Enum.class), normalizeParticleName(particleName));

            if (player != null) {
                Method method = player.getClass().getMethod("spawnParticle", particleClass, Location.class, int.class, double.class, double.class, double.class);
                method.invoke(player, particleEnum, location, count, (double) offsetX, (double) offsetY, (double) offsetZ);
            } else {
                Method method = location.getWorld().getClass().getMethod("spawnParticle", particleClass, Location.class, int.class, double.class, double.class, double.class);
                method.invoke(location.getWorld(), particleEnum, location, count, (double) offsetX, (double) offsetY, (double) offsetZ);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String normalizeParticleName(String input) {
        return input.toUpperCase().replace('-', '_').replace('.', '_');
    }

    public static ParticleEffect smoke() { return new ParticleEffect("SMOKE"); }
    public static ParticleEffect flame() { return new ParticleEffect("FLAME"); }
    public static ParticleEffect heart() { return new ParticleEffect("HEART"); }
    public static ParticleEffect explosion() { return new ParticleEffect("EXPLOSION_NORMAL"); }
}

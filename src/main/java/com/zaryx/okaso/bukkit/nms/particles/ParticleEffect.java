package com.zaryx.okaso.bukkit.nms.particles;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParticleEffect {

    // Abstracts particle names and extra data across legacy and modern Minecraft.
    private static final Logger LOGGER = Logger.getLogger(ParticleEffect.class.getName());

    private static final java.util.Map<String, String> LEGACY_TO_MODERN = new java.util.HashMap<>();

    static {
        LEGACY_TO_MODERN.put("EXPLOSION_NORMAL", "EXPLOSION");
        LEGACY_TO_MODERN.put("EXPLOSION_LARGE", "EXPLOSION_EMITTER");
        LEGACY_TO_MODERN.put("EXPLOSION_HUGE", "EXPLOSION_EMITTER");
        LEGACY_TO_MODERN.put("FIREWORKS_SPARK", "FIREWORK");
        LEGACY_TO_MODERN.put("WATER_BUBBLE", "BUBBLE");
        LEGACY_TO_MODERN.put("WATER_SPLASH", "SPLASH");
        LEGACY_TO_MODERN.put("WATER_WAKE", "FISHING");
        LEGACY_TO_MODERN.put("SUSPENDED", "UNDERWATER");
        LEGACY_TO_MODERN.put("SUSPENDED_DEPTH", "UNDERWATER");
        LEGACY_TO_MODERN.put("CRIT", "CRIT");
        LEGACY_TO_MODERN.put("CRIT_MAGIC", "ENCHANTED_HIT");
        LEGACY_TO_MODERN.put("SMOKE_NORMAL", "SMOKE");
        LEGACY_TO_MODERN.put("SMOKE_LARGE", "LARGE_SMOKE");
        LEGACY_TO_MODERN.put("SPELL", "EFFECT");
        LEGACY_TO_MODERN.put("SPELL_INSTANT", "INSTANT_EFFECT");
        LEGACY_TO_MODERN.put("SPELL_MOB", "ENTITY_EFFECT");
        LEGACY_TO_MODERN.put("SPELL_MOB_AMBIENT", "AMBIENT_ENTITY_EFFECT");
        LEGACY_TO_MODERN.put("SPELL_WITCH", "WITCH");
        LEGACY_TO_MODERN.put("DRIP_WATER", "DRIPPING_WATER");
        LEGACY_TO_MODERN.put("DRIP_LAVA", "DRIPPING_LAVA");
        LEGACY_TO_MODERN.put("VILLAGER_ANGRY", "ANGRY_VILLAGER");
        LEGACY_TO_MODERN.put("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        LEGACY_TO_MODERN.put("TOWN_AURA", "MYCELIUM");
        LEGACY_TO_MODERN.put("ENCHANTMENT_TABLE", "ENCHANT");
        LEGACY_TO_MODERN.put("REDSTONE", "DUST");
        LEGACY_TO_MODERN.put("SNOWBALL", "ITEM_SNOWBALL");
        LEGACY_TO_MODERN.put("SLIME", "ITEM_SLIME");
        LEGACY_TO_MODERN.put("HEART", "HEART");
        LEGACY_TO_MODERN.put("PORTAL", "PORTAL");
    }

    private final String particleName;
    private final int count;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float speed;
    private Object extraData;

    // Builds a particle with a name and default spread.
    public ParticleEffect(String particleName) {
        this(particleName, 1, 0f, 0f, 0f, 0f);
    }

    public ParticleEffect(String particleName, int count, float offsetX, float offsetY, float offsetZ) {
        this(particleName, count, offsetX, offsetY, offsetZ, 0f);
    }

    public ParticleEffect(String particleName, int count, float offsetX, float offsetY, float offsetZ, float speed) {
        this.particleName = particleName == null ? "SMOKE" : particleName;
        this.count = Math.max(1, count);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.speed = speed;
    }

    public ParticleEffect data(Object extraData) {
        this.extraData = extraData;
        return this;
    }

    public ParticleEffect color(int r, int g, int b) {
        try {
            this.extraData = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(r, g, b), 1
            );
        } catch (Throwable ignored) {}
        return this;
    }

    public ParticleEffect color(int r, int g, int b, float size) {
        try {
            this.extraData = new Particle.DustOptions(
                org.bukkit.Color.fromRGB(r, g, b), size
            );
        } catch (Throwable ignored) {}
        return this;
    }

    public void display(Location location) {
        if (location == null || location.getWorld() == null) return;

        if (spawnModern(location, null)) return;

        for (int i = 0; i < count; i++) {
            location.getWorld().playEffect(location, Effect.SMOKE, 0);
        }
    }

    public void display(Player player, Location location) {
        if (player == null || location == null) return;

        if (!spawnModern(location, player)) {
            player.playEffect(location, Effect.SMOKE, 0);
        }
    }

    public void displayToAll(Location location) {
        if (location == null || location.getWorld() == null) return;
        for (Player player : location.getWorld().getPlayers()) {
            display(player, location);
        }
    }

    public void display(Location... locations) {
        for (Location loc : locations) {
            display(loc);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean spawnModern(Location location, Player player) {
        try {
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            String modernName = LEGACY_TO_MODERN.getOrDefault(normalizeParticleName(particleName), normalizeParticleName(particleName));
            Object particleEnum = Enum.valueOf((Class<Enum>) particleClass.asSubclass(Enum.class), modernName);

            if (player != null) {
                if (extraData != null) {
                    Method method = player.getClass().getMethod("spawnParticle", particleClass, Location.class, int.class, double.class, double.class, double.class, double.class, Object.class);
                    method.invoke(player, particleEnum, location, count, (double) offsetX, (double) offsetY, (double) offsetZ, (double) speed, extraData);
                } else {
                    Method method = player.getClass().getMethod("spawnParticle", particleClass, Location.class, int.class, double.class, double.class, double.class, double.class);
                    method.invoke(player, particleEnum, location, count, (double) offsetX, (double) offsetY, (double) offsetZ, (double) speed);
                }
            } else {
                World world = location.getWorld();
                if (extraData != null) {
                    Method method = world.getClass().getMethod("spawnParticle", particleClass, Location.class, int.class, double.class, double.class, double.class, double.class, Object.class);
                    method.invoke(world, particleEnum, location, count, (double) offsetX, (double) offsetY, (double) offsetZ, (double) speed, extraData);
                } else {
                    Method method = world.getClass().getMethod("spawnParticle", particleClass, Location.class, int.class, double.class, double.class, double.class, double.class);
                    method.invoke(world, particleEnum, location, count, (double) offsetX, (double) offsetY, (double) offsetZ, (double) speed);
                }
            }
            return true;
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to spawn particle " + particleName, t);
            return false;
        }
    }

    private static String normalizeParticleName(String input) {
        return input.toUpperCase().replace('-', '_').replace('.', '_');
    }

    public static ParticleEffect smoke() { return new ParticleEffect("SMOKE"); }
    public static ParticleEffect flame() { return new ParticleEffect("FLAME", 1, 0, 0, 0, 0); }
    public static ParticleEffect heart() { return new ParticleEffect("HEART"); }
    public static ParticleEffect explosion() { return new ParticleEffect("EXPLOSION_NORMAL"); }
    public static ParticleEffect enchant() { return new ParticleEffect("ENCHANTMENT_TABLE", 5, 0.5f, 0.5f, 0.5f, 0f); }
    public static ParticleEffect portal() { return new ParticleEffect("PORTAL", 5, 0.5f, 0.5f, 0.5f, 0f); }
    public static ParticleEffect crit() { return new ParticleEffect("CRIT"); }
    public static ParticleEffect magicCrit() { return new ParticleEffect("CRIT_MAGIC"); }
    public static ParticleEffect note() { return new ParticleEffect("NOTE"); }
    public static ParticleEffect lava() { return new ParticleEffect("LAVA"); }
    public static ParticleEffect waterSplash() { return new ParticleEffect("WATER_SPLASH", 5, 0.5f, 0.5f, 0.5f, 0f); }
    public static ParticleEffect redstone() { return new ParticleEffect("REDSTONE"); }
    public static ParticleEffect snowball() { return new ParticleEffect("SNOWBALL"); }
    public static ParticleEffect slime() { return new ParticleEffect("SLIME"); }

    public static ParticleEffect coloredDust(int r, int g, int b) {
        return new ParticleEffect("REDSTONE").color(r, g, b);
    }

    public static ParticleEffect coloredDust(int r, int g, int b, float size) {
        return new ParticleEffect("REDSTONE").color(r, g, b, size);
    }

    public String getParticleName() { return particleName; }
    public int getCount() { return count; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    public float getSpeed() { return speed; }
    public Object getExtraData() { return extraData; }
}

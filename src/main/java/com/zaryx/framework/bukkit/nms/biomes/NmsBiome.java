package com.zaryx.framework.bukkit.nms.biomes;

import com.zaryx.framework.bukkit.utility.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * NMS Biome wrapper for cross-version compatibility.
 */
public class NmsBiome {

    private static final String VERSION = Reflection.getNmsVersion();
    private static final boolean MODERN = Reflection.isModernNms();

    private final Biome bukkitBiome;
    private final String nmsName;

    public NmsBiome(Biome biome) {
        this.bukkitBiome = biome;
        this.nmsName = convertToNmsName(biome);
    }

    public NmsBiome(String biomeName) {
        this.nmsName = biomeName;
        this.bukkitBiome = convertToBukkit(nmsName);
    }

    private String convertToNmsName(Biome biome) {
        if (biome == null) return "PLAINS";

        String name = biome.name();

        // Modern versions use namespaced keys
        if (MODERN) {
            return "minecraft:" + name.toLowerCase();
        }

        // Legacy versions use uppercase without namespace
        return name;
    }

    private Biome convertToBukkit(String nmsName) {
        if (nmsName == null) return Biome.PLAINS;

        String name = nmsName;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1);
        }

        try {
            return Biome.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Biome.PLAINS;
        }
    }

    /**
     * Get NMS BiomeBase class.
     */
    public Class<?> getNmsBiomeClass() {
        if (MODERN) {
            return Reflection.getNMS("net.minecraft.world.level.biome.Biome");
        }
        return Reflection.getNMS("net.minecraft.server." + VERSION + ".BiomeBase");
    }

    /**
     * Get NMS Biome registry.
     */
    public Object getBiomeRegistry() {
        try {
            if (MODERN) {
                Class<?> registryClass = Reflection.getNMS("net.minecraft.core.RegistryBlocks");
                // Access via IRegistry
            }
            Class<?> biomeManagerClass = Reflection.getNMS(
                "net.minecraft.server." + VERSION + ".BiomeManager"
            );
            Field field = biomeManagerClass.getDeclaredField("a");
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set biome at location (requires chunk reload).
     */
    public void setBiome(Location location) {
        setBiome(location.getWorld(), location.getBlockX(), location.getBlockZ());
    }

    /**
     * Set biome at coordinates.
     */
    public void setBiome(World world, int x, int z) {
        try {
            if (MODERN) {
                setBiomeModern(world, x, z);
            } else {
                setBiomeLegacy(world, x, z);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set biome", e);
        }
    }

    private void setBiomeModern(World world, int x, int z) throws Exception {
        // Modern biome setting via chunk snapshot
        int cx = x >> 4;
        int cz = z >> 4;

        Chunk chunk = world.getChunkAt(cx, cz);
        Object nmsChunk = getNmsChunk(chunk);

        if (nmsChunk != null) {
            Class<?> chunkClass = nmsChunk.getClass();
            Method getSection = chunkClass.getMethod("getSection", int.class);
            Method setBiome = chunkClass.getMethod("a", Biome.class, int.class, int.class, int.class);
            setBiome.invoke(nmsChunk, bukkitBiome, x & 15, 0, z & 15);
        }
    }

    private void setBiomeLegacy(World world, int x, int z) throws Exception {
        // Legacy biome setting
        int cx = x >> 4;
        int cz = z >> 4;

        Chunk chunk = world.getChunkAt(cx, cz);
        Object nmsChunk = getNmsChunk(chunk);

        if (nmsChunk != null) {
            Class<?> chunkClass = nmsChunk.getClass();
            Method setBiome = chunkClass.getMethod("setBiome", Biome.class);
            setBiome.invoke(nmsChunk, bukkitBiome);
        }
    }

    private Object getNmsChunk(Chunk chunk) {
        try {
            Class<?> craftChunkClass = Reflection.getOBC("CraftChunk");
            Field handleField = craftChunkClass.getDeclaredField("handle");
            handleField.setAccessible(true);
            return handleField.get(chunk);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get all available biome names.
     */
    public static Set<String> getAvailableBiomes() {
        Set<String> biomes = new HashSet<>();
        for (Biome biome : Biome.values()) {
            biomes.add(biome.name());
        }
        return biomes;
    }

    /**
     * Check if biome exists.
     */
    public static boolean exists(String name) {
        try {
            Biome.valueOf(name.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Getters
    public Biome getBukkitBiome() { return bukkitBiome; }
    public String getNmsName() { return nmsName; }

    @Override
    public String toString() {
        return "NmsBiome{bukkit=" + bukkitBiome + ", nms=" + nmsName + "}";
    }
}

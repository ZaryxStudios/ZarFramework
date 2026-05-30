package com.zaryx.okaso.bukkit.nms.biomes;

import com.zaryx.okaso.bukkit.utility.Reflection;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class NmsBiome {

    // Wraps a Bukkit biome and resolves the correct NMS name for the server version.
    private static final String VERSION = Reflection.getNmsVersion();
    private static final boolean MODERN = Reflection.isModernNms();

    private static final Map<String, String> LEGACY_TO_MODERN = new HashMap<>();
    private static final Map<String, String> MODERN_TO_LEGACY = new HashMap<>();

    static {
        LEGACY_TO_MODERN.put("PLAINS", "minecraft:plains");
        LEGACY_TO_MODERN.put("DESERT", "minecraft:desert");
        LEGACY_TO_MODERN.put("FOREST", "minecraft:forest");
        LEGACY_TO_MODERN.put("TAIGA", "minecraft:taiga");
        LEGACY_TO_MODERN.put("SWAMPLAND", "minecraft:swamp");
        LEGACY_TO_MODERN.put("SWAMP", "minecraft:swamp");
        LEGACY_TO_MODERN.put("HELL", "minecraft:nether_wastes");
        LEGACY_TO_MODERN.put("NETHER", "minecraft:nether_wastes");
        LEGACY_TO_MODERN.put("SKY", "minecraft:the_end");
        LEGACY_TO_MODERN.put("END", "minecraft:the_end");
        LEGACY_TO_MODERN.put("FROZEN_OCEAN", "minecraft:frozen_ocean");
        LEGACY_TO_MODERN.put("FROZEN_RIVER", "minecraft:frozen_river");
        LEGACY_TO_MODERN.put("ICE_PLAINS", "minecraft:snowy_plains");
        LEGACY_TO_MODERN.put("ICE_MOUNTAINS", "minecraft:snowy_plains");
        LEGACY_TO_MODERN.put("MUSHROOM_ISLAND", "minecraft:mushroom_fields");
        LEGACY_TO_MODERN.put("MUSHROOM_SHORE", "minecraft:mushroom_fields");
        LEGACY_TO_MODERN.put("BEACH", "minecraft:beach");
        LEGACY_TO_MODERN.put("DESERT_HILLS", "minecraft:desert_hills");
        LEGACY_TO_MODERN.put("FOREST_HILLS", "minecraft:wooded_hills");
        LEGACY_TO_MODERN.put("TAIGA_HILLS", "minecraft:taiga_hills");
        LEGACY_TO_MODERN.put("JUNGLE", "minecraft:jungle");
        LEGACY_TO_MODERN.put("JUNGLE_HILLS", "minecraft:jungle_hills");
        LEGACY_TO_MODERN.put("JUNGLE_EDGE", "minecraft:jungle_edge");
        LEGACY_TO_MODERN.put("DEEP_OCEAN", "minecraft:deep_ocean");
        LEGACY_TO_MODERN.put("STONE_BEACH", "minecraft:stone_shore");
        LEGACY_TO_MODERN.put("COLD_BEACH", "minecraft:snowy_beach");
        LEGACY_TO_MODERN.put("BIRCH_FOREST", "minecraft:birch_forest");
        LEGACY_TO_MODERN.put("BIRCH_FOREST_HILLS", "minecraft:birch_forest_hills");
        LEGACY_TO_MODERN.put("ROOFED_FOREST", "minecraft:dark_forest");
        LEGACY_TO_MODERN.put("COLD_TAIGA", "minecraft:snowy_taiga");
        LEGACY_TO_MODERN.put("COLD_TAIGA_HILLS", "minecraft:snowy_taiga_hills");
        LEGACY_TO_MODERN.put("MEGA_TAIGA", "minecraft:old_growth_pine_taiga");
        LEGACY_TO_MODERN.put("MEGA_TAIGA_HILLS", "minecraft:old_growth_pine_taiga");
        LEGACY_TO_MODERN.put("EXTREME_HILLS", "minecraft:windswept_hills");
        LEGACY_TO_MODERN.put("EXTREME_HILLS_PLUS", "minecraft:windswept_hills");
        LEGACY_TO_MODERN.put("SAVANNA", "minecraft:savanna");
        LEGACY_TO_MODERN.put("SAVANNA_PLATEAU", "minecraft:savanna_plateau");
        LEGACY_TO_MODERN.put("MESA", "minecraft:badlands");
        LEGACY_TO_MODERN.put("MESA_PLATEAU_FOREST", "minecraft:wooded_badlands");
        LEGACY_TO_MODERN.put("MESA_PLATEAU", "minecraft:badlands_plateau");
        LEGACY_TO_MODERN.put("SUNFLOWER_PLAINS", "minecraft:sunflower_plains");
        LEGACY_TO_MODERN.put("FLOWER_FOREST", "minecraft:flower_forest");
        LEGACY_TO_MODERN.put("ICE_PLAINS_SPIKES", "minecraft:ice_spikes");
        LEGACY_TO_MODERN.put("OCEAN", "minecraft:ocean");
        LEGACY_TO_MODERN.put("RIVER", "minecraft:river");

        for (Map.Entry<String, String> entry : LEGACY_TO_MODERN.entrySet()) {
            MODERN_TO_LEGACY.put(entry.getValue(), entry.getKey());
        }
    }

    // Holds the Bukkit biome, the resolved NMS name, and the namespaced key.
    private final Biome bukkitBiome;
    private final String nmsName;
    private final String namespacedKey;

    public NmsBiome(Biome biome) {
        this.bukkitBiome = biome != null ? biome : Biome.PLAINS;
        this.nmsName = convertToNmsName(this.bukkitBiome);
        this.namespacedKey = MODERN ? "minecraft:" + this.bukkitBiome.name().toLowerCase() : this.nmsName;
    }

    public NmsBiome(String biomeName) {
        this.nmsName = biomeName != null ? biomeName : "PLAINS";
        this.bukkitBiome = convertToBukkit(this.nmsName);
        this.namespacedKey = buildNamespacedKey(this.nmsName);
    }

    private String convertToNmsName(Biome biome) {
        if (biome == null) return "PLAINS";
        String name = biome.name();

        if (MODERN) {
            String modern = LEGACY_TO_MODERN.get(name);
            return modern != null ? modern : "minecraft:" + name.toLowerCase();
        }
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
        } catch (IllegalArgumentException ignored) {}

        String legacy = MODERN_TO_LEGACY.get(nmsName.toLowerCase());
        if (legacy != null) {
            try {
                return Biome.valueOf(legacy);
            } catch (IllegalArgumentException ignored) {}
        }

        return Biome.PLAINS;
    }

    private String buildNamespacedKey(String name) {
        if (name == null) return "minecraft:plains";
        if (name.contains(":")) return name.toLowerCase();
        return "minecraft:" + name.toLowerCase();
    }

    public Class<?> getNmsBiomeClass() {
        if (MODERN) {
            return Reflection.getNMS("net.minecraft.world.level.biome.Biome");
        }
        return Reflection.getNMS("BiomeBase");
    }

    public Object getBiomeRegistry() {
        try {
            if (MODERN) {

                Class<?> builtInRegistries = Reflection.getNMS("net.minecraft.core.registries.BuiltInRegistries");
                Field biomeField = builtInRegistries.getDeclaredField("BIOME");
                biomeField.setAccessible(true);
                return biomeField.get(null);
            }

            Class<?> biomeManagerClass = Reflection.getNMS("BiomeManager");
            Field field = biomeManagerClass.getDeclaredField("a");
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    public void setBiome(Location location) {
        setBiome(location.getWorld(), location.getBlockX(), location.getBlockZ());
    }

    public void setBiome(World world, int x, int z) {
        try {
            if (MODERN) {
                setBiomeModern(world, x, z);
            } else {
                setBiomeLegacy(world, x, z);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set biome at " + x + "," + z, e);
        }
    }

    public void setBiomeArea(World world, int x1, int z1, int x2, int z2) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                setBiome(world, x, z);
            }
        }
    }

    public void setBiomeCircle(World world, int centerX, int centerZ, int radius) {
        int radiusSquared = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radiusSquared) {
                    setBiome(world, centerX + x, centerZ + z);
                }
            }
        }
    }

    public void refreshBiomeForPlayer(Player player, Location location) {
        int cx = location.getBlockX() >> 4;
        int cz = location.getBlockZ() >> 4;
        player.sendChunkChange(location.getWorld().getChunkAt(cx, cz).getBlock(0, 0, 0).getLocation(), 16, 16, 16);
    }

    private void setBiomeModern(World world, int x, int z) throws Exception {
        int cx = x >> 4;
        int cz = z >> 4;

        Chunk chunk = world.getChunkAt(cx, cz);
        Object nmsChunk = Reflection.getNmsChunk(chunk);

        if (nmsChunk != null) {
            Class<?> chunkClass = nmsChunk.getClass();
            try {
                Method setBiome = chunkClass.getMethod("a", Biome.class, int.class, int.class, int.class);
                setBiome.invoke(nmsChunk, bukkitBiome, x & 15, 0, z & 15);
            } catch (NoSuchMethodException e) {

                Method setBiome = chunkClass.getMethod("setBiome", int.class, int.class, int.class, Biome.class);
                setBiome.invoke(nmsChunk, x & 15, 0, z & 15, bukkitBiome);
            }
        }
    }

    private void setBiomeLegacy(World world, int x, int z) throws Exception {
        int cx = x >> 4;
        int cz = z >> 4;

        Chunk chunk = world.getChunkAt(cx, cz);
        Object nmsChunk = Reflection.getNmsChunk(chunk);

        if (nmsChunk != null) {
            Class<?> chunkClass = nmsChunk.getClass();
            Method setBiome = chunkClass.getMethod("setBiome", Biome.class);
            setBiome.invoke(nmsChunk, bukkitBiome);
        }
    }

    public static Set<String> getAvailableBiomes() {
        Set<String> biomes = new HashSet<>();
        for (Biome biome : Biome.values()) {
            biomes.add(biome.name());
        }
        return biomes;
    }

    public static Map<String, String> getBiomeNamespacedMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Biome biome : Biome.values()) {
            String legacy = biome.name();
            String modern = LEGACY_TO_MODERN.get(legacy);
            map.put(legacy, modern != null ? modern : "minecraft:" + legacy.toLowerCase());
        }
        return map;
    }

    public static boolean exists(String name) {
        if (name == null) return false;
        String clean = name.contains(":") ? name.substring(name.indexOf(":") + 1) : name;
        try {
            Biome.valueOf(clean.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return LEGACY_TO_MODERN.containsKey(clean.toUpperCase());
        }
    }

    public static String toModernKey(String legacyName) {
        if (legacyName == null) return "minecraft:plains";
        String modern = LEGACY_TO_MODERN.get(legacyName.toUpperCase());
        return modern != null ? modern : "minecraft:" + legacyName.toLowerCase();
    }

    public static String toLegacyName(String modernKey) {
        if (modernKey == null) return "PLAINS";
        String legacy = MODERN_TO_LEGACY.get(modernKey.toLowerCase());
        if (legacy != null) return legacy;
        String name = modernKey.contains(":") ? modernKey.substring(modernKey.indexOf(":") + 1) : modernKey;
        return name.toUpperCase();
    }

    public Biome getBukkitBiome() { return bukkitBiome; }
    public String getNmsName() { return nmsName; }
    public String getNamespacedKey() { return namespacedKey; }

    @Override
    public String toString() {
        return "NmsBiome{bukkit=" + bukkitBiome + ", nms=" + nmsName + ", key=" + namespacedKey + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NmsBiome)) return false;
        NmsBiome nmsBiome = (NmsBiome) o;
        return namespacedKey.equals(nmsBiome.namespacedKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespacedKey);
    }
}

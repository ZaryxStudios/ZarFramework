package com.zaryx.framework.bukkit.utility.biome;

import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * Biome utilities and helpers.
 */
public class BiomeUtils {

    /**
     * Get biome temperature category.
     */
    public static TemperatureCategory getTemperature(Biome biome) {
        if (biome == null) return TemperatureCategory.NEUTRAL;

        BiomeFamily family = getFamily(biome);
        if (family == BiomeFamily.DESERT) {
            return TemperatureCategory.HOT;
        }
        if (family == BiomeFamily.SNOWY) {
            return TemperatureCategory.COLD;
        }
        if (family == BiomeFamily.JUNGLE || family == BiomeFamily.SWAMP) {
            return TemperatureCategory.HUMID;
        }
        if (family == BiomeFamily.PLAINS || family == BiomeFamily.MOUNTAIN) {
            return TemperatureCategory.WARM;
        }

        String name = biome.name();
        if (containsAny(name, "DESERT", "SAVANNA", "BADLANDS", "MESA")) {
            return TemperatureCategory.HOT;
        }
        if (containsAny(name, "SNOW", "ICE", "FROZEN", "COLD")) {
            return TemperatureCategory.COLD;
        }
        if (containsAny(name, "JUNGLE", "SWAMP", "MANGROVE")) {
            return TemperatureCategory.HUMID;
        }
        if (containsAny(name, "PLATEAU", "WINDSWEPT")) {
            return TemperatureCategory.WARM;
        }
        return TemperatureCategory.NEUTRAL;
    }

    /**
     * Check if biome is aquatic.
     */
    public static boolean isAquatic(Biome biome) {
        BiomeFamily family = getFamily(biome);
        return family == BiomeFamily.AQUATIC || family == BiomeFamily.SWAMP;
    }

    /**
     * Check if biome is forested.
     */
    public static boolean isForested(Biome biome) {
        BiomeFamily family = getFamily(biome);
        return family == BiomeFamily.FOREST || family == BiomeFamily.JUNGLE;
    }

    /**
     * Check if biome is mountainous.
     */
    public static boolean isMountainous(Biome biome) {
        BiomeFamily family = getFamily(biome);
        return family == BiomeFamily.MOUNTAIN;
    }

    /**
     * Check if biome is a plains variant.
     */
    public static boolean isPlains(Biome biome) {
        return getFamily(biome) == BiomeFamily.PLAINS;
    }

    /**
     * Get player biome.
     */
    public static Biome getPlayerBiome(Player player) {
        return player.getLocation().getBlock().getBiome();
    }

    /**
     * Get formatted biome name.
     */
    public static String getDisplayName(Biome biome) {
        if (biome == null) return "Unknown";

        String name = biome.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();

        for (String word : name.split(" ")) {
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
        }

        return result.toString().trim();
    }

    /**
     * Get biome family classification.
     */
    public static BiomeFamily getFamily(Biome biome) {
        if (biome == null) return BiomeFamily.OTHER;

        String name = biome.name();
        if (containsAny(name, "OCEAN", "RIVER", "BEACH", "REED", "MANGROVE_SWAMP")) {
            return BiomeFamily.AQUATIC;
        }
        if (containsAny(name, "FOREST", "TAIGA", "DARK_FOREST", "BIRCH", "FLOWER_FOREST", "OLD_GROWTH_PINE_TAIGA", "OLD_GROWTH_SPRUCE_TAIGA")) {
            return BiomeFamily.FOREST;
        }
        if (containsAny(name, "MOUNTAIN", "HILLS", "WINDSWEPT", "STONY_SHORE", "STONY_PEAKS")) {
            return BiomeFamily.MOUNTAIN;
        }
        if (containsAny(name, "PLAINS", "SUNFLOWER", "MEADOW")) {
            return BiomeFamily.PLAINS;
        }
        if (containsAny(name, "DESERT", "SAVANNA", "BADLANDS", "MESA")) {
            return BiomeFamily.DESERT;
        }
        if (containsAny(name, "SNOW", "ICE", "FROZEN", "COLD", "GROVE", "SNOWY")) {
            return BiomeFamily.SNOWY;
        }
        if (containsAny(name, "JUNGLE", "SPARSE_JUNGLE", "BAMBOO_JUNGLE")) {
            return BiomeFamily.JUNGLE;
        }
        if (containsAny(name, "SWAMP", "MANGROVE")) {
            return BiomeFamily.SWAMP;
        }
        if (containsAny(name, "NETHER")) {
            return BiomeFamily.NETHER;
        }
        if (containsAny(name, "END")) {
            return BiomeFamily.END;
        }

        return BiomeFamily.OTHER;
    }

    public enum TemperatureCategory {
        HOT, COLD, HUMID, WARM, NEUTRAL
    }

    public enum BiomeFamily {
        AQUATIC, FOREST, MOUNTAIN, PLAINS, DESERT, SNOWY, JUNGLE, SWAMP, NETHER, END, OTHER
    }

    private static boolean containsAny(String input, String... values) {
        for (String value : values) {
            if (input.contains(value)) {
                return true;
            }
        }
        return false;
    }
}

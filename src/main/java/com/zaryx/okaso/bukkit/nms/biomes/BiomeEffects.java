package com.zaryx.okaso.bukkit.nms.biomes;

import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * Lightweight biome visual profile container.
 * Packet-level biome visuals differ by version, so this class stores metadata safely.
 */
public class BiomeEffects {

    private final Biome biome;
    private String skyColor;
    private String foliageColor;
    private String grassColor;
    private float fogDensity = -1f;
    private String weather;

    public BiomeEffects(Biome biome) {
        this.biome = biome;
    }

    public BiomeEffects skyColor(int r, int g, int b) {
        this.skyColor = String.format("%02X%02X%02X", r, g, b);
        return this;
    }

    public BiomeEffects skyColor(String hex) {
        this.skyColor = sanitizeHex(hex);
        return this;
    }

    public BiomeEffects foliageColor(int r, int g, int b) {
        this.foliageColor = String.format("%02X%02X%02X", r, g, b);
        return this;
    }

    public BiomeEffects grassColor(int r, int g, int b) {
        this.grassColor = String.format("%02X%02X%02X", r, g, b);
        return this;
    }

    public BiomeEffects fogDensity(float density) {
        this.fogDensity = density;
        return this;
    }

    public BiomeEffects weather(String weather) {
        this.weather = weather;
        return this;
    }

    public void apply(Player player) {
        // Cross-version packet behavior is highly version-specific.
        // Keep API stable; callers can use this profile with their own packet adapters.
    }

    public Biome getBiome() {
        return biome;
    }

    public String getSkyColor() {
        return skyColor;
    }

    public String getFoliageColor() {
        return foliageColor;
    }

    public String getGrassColor() {
        return grassColor;
    }

    public float getFogDensity() {
        return fogDensity;
    }

    public String getWeather() {
        return weather;
    }

    private static String sanitizeHex(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("#", "").toUpperCase();
    }
}

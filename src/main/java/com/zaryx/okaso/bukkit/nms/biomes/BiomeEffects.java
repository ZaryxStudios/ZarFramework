package com.zaryx.okaso.bukkit.nms.biomes;

import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

public class BiomeEffects {

    private final Biome biome;
    private Integer skyColor;
    private Integer foliageColor;
    private Integer grassColor;
    private Integer waterColor;
    private Integer waterFogColor;
    private float fogDensity = -1f;
    private String weather;
    private float temperature = -1f;
    private float humidity = -1f;

    public BiomeEffects(Biome biome) {
        this.biome = biome != null ? biome : Biome.PLAINS;
    }

    public BiomeEffects skyColor(int r, int g, int b) {
        this.skyColor = rgbToInt(r, g, b);
        return this;
    }

    public BiomeEffects skyColor(int rgb) {
        this.skyColor = rgb;
        return this;
    }

    public BiomeEffects skyColor(String hex) {
        this.skyColor = hexToInt(hex);
        return this;
    }

    public BiomeEffects foliageColor(int r, int g, int b) {
        this.foliageColor = rgbToInt(r, g, b);
        return this;
    }

    public BiomeEffects foliageColor(int rgb) {
        this.foliageColor = rgb;
        return this;
    }

    public BiomeEffects grassColor(int r, int g, int b) {
        this.grassColor = rgbToInt(r, g, b);
        return this;
    }

    public BiomeEffects grassColor(int rgb) {
        this.grassColor = rgb;
        return this;
    }

    public BiomeEffects waterColor(int r, int g, int b) {
        this.waterColor = rgbToInt(r, g, b);
        return this;
    }

    public BiomeEffects waterColor(int rgb) {
        this.waterColor = rgb;
        return this;
    }

    public BiomeEffects waterFogColor(int r, int g, int b) {
        this.waterFogColor = rgbToInt(r, g, b);
        return this;
    }

    public BiomeEffects waterFogColor(int rgb) {
        this.waterFogColor = rgb;
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

    public BiomeEffects temperature(float temperature) {
        this.temperature = temperature;
        return this;
    }

    public BiomeEffects humidity(float humidity) {
        this.humidity = humidity;
        return this;
    }

    public static BiomeEffects desert() {
        return new BiomeEffects(Biome.DESERT)
            .skyColor(0x98A0B0)
            .grassColor(0xBFB755)
            .foliageColor(0xAEA042)
            .waterColor(0x3F76E4)
            .temperature(2.0f)
            .humidity(0.0f);
    }

    public static BiomeEffects forest() {
        return new BiomeEffects(Biome.FOREST)
            .skyColor(0x78A7FF)
            .grassColor(0x3B7E1C)
            .foliageColor(0x59AE30)
            .waterColor(0x3F76E4)
            .temperature(0.7f)
            .humidity(0.8f);
    }

    public static BiomeEffects snowy() {
        return new BiomeEffects(Biome.SNOWY_PLAINS)
            .skyColor(0xB0C0D0)
            .grassColor(0x80B0E0)
            .foliageColor(0x60A0C0)
            .waterColor(0x3F76E4)
            .temperature(0.0f)
            .humidity(0.5f);
    }

    public static BiomeEffects jungle() {
        return new BiomeEffects(Biome.JUNGLE)
            .skyColor(0x78A7FF)
            .grassColor(0x4B8B2C)
            .foliageColor(0x6BB53C)
            .waterColor(0x3F76E4)
            .temperature(0.95f)
            .humidity(0.9f);
    }

    public static BiomeEffects ocean() {
        return new BiomeEffects(Biome.OCEAN)
            .skyColor(0x78A7FF)
            .grassColor(0x8EB971)
            .foliageColor(0x71A74D)
            .waterColor(0x3F76E4)
            .temperature(0.5f)
            .humidity(0.5f);
    }

    public static BiomeEffects nether() {
        return new BiomeEffects(Biome.NETHER_WASTES)
            .skyColor(0x330303)
            .grassColor(0x6B3B2B)
            .foliageColor(0x6B3B2B)
            .waterColor(0x8B3B2B)
            .waterFogColor(0x8B3B2B)
            .temperature(2.0f)
            .humidity(0.0f)
            .fogDensity(2.0f);
    }

    public static BiomeEffects end() {
        return new BiomeEffects(Biome.THE_END)
            .skyColor(0x000000)
            .grassColor(0x8B8B8B)
            .foliageColor(0x8B8B8B)
            .waterColor(0x3F76E4)
            .temperature(0.5f)
            .humidity(0.5f);
    }

    public void apply(Player player) {
        if (player == null) return;

    }

    public Biome getBiome() { return biome; }
    public Integer getSkyColor() { return skyColor; }
    public Integer getFoliageColor() { return foliageColor; }
    public Integer getGrassColor() { return grassColor; }
    public Integer getWaterColor() { return waterColor; }
    public Integer getWaterFogColor() { return waterFogColor; }
    public float getFogDensity() { return fogDensity; }
    public String getWeather() { return weather; }
    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }

    public String getSkyColorHex() {
        return skyColor != null ? String.format("#%06X", skyColor) : null;
    }

    public String getFoliageColorHex() {
        return foliageColor != null ? String.format("#%06X", foliageColor) : null;
    }

    public String getGrassColorHex() {
        return grassColor != null ? String.format("#%06X", grassColor) : null;
    }

    private static int rgbToInt(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }

    private static Integer hexToInt(String hex) {
        if (hex == null) return null;
        String clean = hex.replace("#", "").trim();
        try {
            return (int) Long.parseLong(clean, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "BiomeEffects{" +
                "biome=" + biome +
                ", skyColor=" + getSkyColorHex() +
                ", foliageColor=" + getFoliageColorHex() +
                ", grassColor=" + getGrassColorHex() +
                ", waterColor=" + (waterColor != null ? String.format("#%06X", waterColor) : "null") +
                ", fogDensity=" + fogDensity +
                ", weather='" + weather + '\'' +
                '}';
    }
}

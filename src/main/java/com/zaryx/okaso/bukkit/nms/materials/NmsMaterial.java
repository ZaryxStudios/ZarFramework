package com.zaryx.okaso.bukkit.nms.materials;

import com.zaryx.okaso.bukkit.utility.Reflection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.*;

public class NmsMaterial {

    // Resolves Bukkit materials with aliases and caches results for speed.
    private static final Map<String, String[]> MATERIAL_ALIASES = new HashMap<>();
    private static final Map<String, Material> materialCache = new HashMap<>();

    static {
        MATERIAL_ALIASES.put("PLAYER_HEAD", new String[]{"PLAYER_HEAD", "SKULL_ITEM"});
        MATERIAL_ALIASES.put("SPLASH_POTION", new String[]{"SPLASH_POTION", "POTION"});
        MATERIAL_ALIASES.put("FIREWORK_ROCKET", new String[]{"FIREWORK_ROCKET", "FIREWORK"});
        MATERIAL_ALIASES.put("NETHERITE_INGOT", new String[]{"NETHERITE_INGOT", "DIAMOND"});
        MATERIAL_ALIASES.put("GOLDEN_APPLE", new String[]{"GOLDEN_APPLE", "GOLDEN_APPLE"});
        MATERIAL_ALIASES.put("ENCHANTED_GOLDEN_APPLE", new String[]{"ENCHANTED_GOLDEN_APPLE", "GOLDEN_APPLE"});
        MATERIAL_ALIASES.put("CRAFTING_TABLE", new String[]{"CRAFTING_TABLE", "WORKBENCH"});
        MATERIAL_ALIASES.put("ENDER_PEARL", new String[]{"ENDER_PEARL", "ENDER_PEARL"});
        MATERIAL_ALIASES.put("BLAZE_ROD", new String[]{"BLAZE_ROD", "BLAZE_ROD"});
        MATERIAL_ALIASES.put("MUSHROOM_STEW", new String[]{"MUSHROOM_STEW", "MUSHROOM_SOUP"});
    }

    // Holds the resolved Bukkit material, data value, and namespaced key.
    private final Material bukkitMaterial;
    private final byte data;
    private final String namespacedKey;

    public NmsMaterial(Material material) {
        this(material, (byte) 0);
    }

    public NmsMaterial(Material material, byte data) {
        this.bukkitMaterial = material == null ? Material.STONE : material;
        this.data = data;
        this.namespacedKey = "minecraft:" + this.bukkitMaterial.name().toLowerCase();
    }

    public NmsMaterial(String materialName) {
        this.bukkitMaterial = getMaterial(materialName);
        this.data = 0;
        this.namespacedKey = "minecraft:" + this.bukkitMaterial.name().toLowerCase();
    }

    public static Material getMaterial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Material.STONE;
        }

        String cacheKey = name.toUpperCase();
        if (materialCache.containsKey(cacheKey)) {
            return materialCache.get(cacheKey);
        }

        Material direct = resolveSingle(name);
        if (direct != null) {
            materialCache.put(cacheKey, direct);
            return direct;
        }

        String[] aliases = MATERIAL_ALIASES.get(name.toUpperCase());
        if (aliases != null) {
            for (String alias : aliases) {
                Material resolved = resolveSingle(alias);
                if (resolved != null) {
                    materialCache.put(cacheKey, resolved);
                    return resolved;
                }
            }
        }

        if (name.contains(":")) {
            String clean = name.substring(name.indexOf(":") + 1).toUpperCase();
            Material fromKey = resolveSingle(clean);
            if (fromKey != null) {
                materialCache.put(cacheKey, fromKey);
                return fromKey;
            }
        }

        materialCache.put(cacheKey, Material.STONE);
        return Material.STONE;
    }

    public static Set<String> getAvailableMaterials() {
        Set<String> set = new HashSet<>();
        for (Material material : Material.values()) {
            set.add(material.name());
        }
        return set;
    }

    public static boolean exists(String name) {
        return resolveSingle(name) != null;
    }

    public boolean isTool() {
        String name = bukkitMaterial.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") ||
               name.endsWith("_SHOVEL") || name.endsWith("_HOE") ||
               name.endsWith("_SWORD") || name.equals("SHEARS") ||
               name.equals("FLINT_AND_STEEL") || name.equals("FISHING_ROD");
    }

    public boolean isArmor() {
        String name = bukkitMaterial.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") ||
               name.equals("ELYTRA") || name.equals("TURTLE_HELMET");
    }

    public boolean isFood() {
        return bukkitMaterial.isEdible();
    }

    public boolean isBlock() {
        return bukkitMaterial.isBlock();
    }

    public boolean isFlammable() {
        return bukkitMaterial.isFlammable();
    }

    public boolean isItem() {
        return !bukkitMaterial.isBlock();
    }

    public boolean isOre() {
        String name = bukkitMaterial.name();
        return name.endsWith("_ORE") || name.equals("NETHERITE_SCRAP") ||
               name.equals("ANCIENT_DEBRIS") || name.equals("RAW_IRON") ||
               name.equals("RAW_GOLD") || name.equals("RAW_COPPER");
    }

    public boolean isWood() {
        String name = bukkitMaterial.name();
        return name.contains("WOOD") || name.contains("LOG") ||
               name.contains("PLANK") || name.contains("FENCE") ||
               name.contains("DOOR") || name.contains("TRAPDOOR") ||
               name.contains("SIGN") || name.contains("BOAT");
    }

    public Object toNms(ItemStack bukkit) {
        return Reflection.toNmsItem(bukkit);
    }

    public ItemStack fromNms(Object nmsItemStack) {
        return Reflection.fromNmsItem(nmsItemStack);
    }

    public ItemStack createStack(int amount) {
        return new ItemStack(bukkitMaterial, amount, data);
    }

    public ItemStack createStack(int amount, short durability) {
        return new ItemStack(bukkitMaterial, amount, durability);
    }

    public Object createNmsStack(int amount) {
        return toNms(createStack(amount));
    }

    private static Material resolveSingle(String name) {
        if (name == null) return null;

        String normalized = name.toUpperCase()
                .replace('-', '_')
                .replace('.', '_')
                .replace("MINECRAFT:", "");

        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {}

        Material legacy = Material.getMaterial(normalized);
        if (legacy != null) return legacy;

        if (normalized.startsWith("MINECRAFT_")) {
            return Material.getMaterial(normalized.substring("MINECRAFT_".length()));
        }

        return null;
    }

    public Material getBukkitMaterial() { return bukkitMaterial; }
    public byte getData() { return data; }
    public String getNamespacedKey() { return namespacedKey; }

    @Override
    public String toString() {
        return "NmsMaterial{bukkit=" + bukkitMaterial + ", data=" + data + ", key=" + namespacedKey + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NmsMaterial)) return false;
        NmsMaterial that = (NmsMaterial) o;
        return data == that.data && bukkitMaterial == that.bukkitMaterial;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bukkitMaterial, data);
    }
}

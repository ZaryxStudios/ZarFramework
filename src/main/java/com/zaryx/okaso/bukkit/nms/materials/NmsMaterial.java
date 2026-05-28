package com.zaryx.okaso.bukkit.nms.materials;

import com.zaryx.okaso.bukkit.utility.Reflection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Material resolver and NMS converter that works on old/new APIs.
 */
public class NmsMaterial {

    private static final Map<String, String[]> MATERIAL_ALIASES = new HashMap<String, String[]>();

    static {
        MATERIAL_ALIASES.put("PLAYER_HEAD", new String[]{"PLAYER_HEAD", "SKULL_ITEM"});
        MATERIAL_ALIASES.put("SPLASH_POTION", new String[]{"SPLASH_POTION", "POTION"});
        MATERIAL_ALIASES.put("FIREWORK_ROCKET", new String[]{"FIREWORK_ROCKET", "FIREWORK"});
        MATERIAL_ALIASES.put("NETHERITE_INGOT", new String[]{"NETHERITE_INGOT", "DIAMOND"});
    }

    private final Material bukkitMaterial;
    private final byte data;

    public NmsMaterial(Material material) {
        this(material, (byte) 0);
    }

    public NmsMaterial(Material material, byte data) {
        this.bukkitMaterial = material == null ? Material.STONE : material;
        this.data = data;
    }

    public NmsMaterial(String materialName) {
        this.bukkitMaterial = getMaterial(materialName);
        this.data = 0;
    }

    public static Material getMaterial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Material.STONE;
        }

        Material direct = resolveSingle(name);
        if (direct != null) {
            return direct;
        }

        String[] aliases = MATERIAL_ALIASES.get(name.toUpperCase());
        if (aliases != null) {
            for (String alias : aliases) {
                Material resolved = resolveSingle(alias);
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        return Material.STONE;
    }

    public static Set<String> getAvailableMaterials() {
        Set<String> set = new HashSet<String>();
        for (Material material : Material.values()) {
            set.add(material.name());
        }
        return set;
    }

    public static boolean exists(String name) {
        return resolveSingle(name) != null;
    }

    public Object toNms(ItemStack bukkit) {
        try {
            Class<?> craftItemStack = Reflection.getOBC("inventory.CraftItemStack");
            Method asNmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            return asNmsCopy.invoke(null, bukkit);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Bukkit item to NMS", e);
        }
    }

    public ItemStack fromNms(Object nmsItemStack) {
        try {
            Class<?> craftItemStack = Reflection.getOBC("inventory.CraftItemStack");
            Method asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", nmsItemStack.getClass());
            return (ItemStack) asBukkitCopy.invoke(null, nmsItemStack);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert NMS item to Bukkit", e);
        }
    }

    public Material getBukkitMaterial() {
        return bukkitMaterial;
    }

    public byte getData() {
        return data;
    }

    public ItemStack createStack(int amount) {
        return new ItemStack(bukkitMaterial, amount, data);
    }

    public ItemStack createStack(int amount, short durability) {
        return new ItemStack(bukkitMaterial, amount, durability);
    }

    private static Material resolveSingle(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.toUpperCase().replace('-', '_').replace('.', '_');

        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
        }

        Material legacy = Material.getMaterial(normalized);
        if (legacy != null) {
            return legacy;
        }

        if (normalized.startsWith("MINECRAFT_")) {
            return Material.getMaterial(normalized.substring("MINECRAFT_".length()));
        }

        return null;
    }
}

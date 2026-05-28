package com.zaryx.okaso.bukkit.nms.enchants;

import com.zaryx.okaso.bukkit.utility.Reflection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.*;

/**
 * NMS Enchantment wrapper for cross-version compatibility.
 */
public class NmsEnchantment {

    private static final String VERSION = Reflection.getNmsVersion();
    private static final boolean MODERN = Reflection.isModernNms();

    private final Enchantment enchantment;
    private final String nmsName;

    public NmsEnchantment(Enchantment enchantment) {
        this.enchantment = enchantment;
        this.nmsName = getNmsName(enchantment);
    }

    public NmsEnchantment(String enchantmentName) {
        this.enchantment = EnchantmentRegistry.resolve(enchantmentName);
        this.nmsName = this.enchantment != null
                ? getNmsName(this.enchantment)
                : EnchantmentRegistry.canonicalName(enchantmentName);
    }

    private String getNmsName(Enchantment enchantment) {
        if (enchantment == null) return "";

        String name = EnchantmentRegistry.canonicalName(enchantment.getName());

        if (MODERN) {
            return "minecraft:" + name.toLowerCase();
        }

        return name;
    }

    /**
     * Get NMS Enchantment class.
     */
    public Class<?> getNmsEnchantClass() {
        if (MODERN) {
            return Reflection.getNMS("net.minecraft.world.item.enchantment.Enchantment");
        }
        return Reflection.getNMS("net.minecraft.server." + VERSION + ".Enchantment");
    }

    /**
     * Get enchantment level from NMS.
     */
    public int getLevelFromNms(ItemStack item) {
        try {
            Object nmsItem = toNmsItem(item);
            if (nmsItem == null) return 0;

            Class<?> itemClass = nmsItem.getClass();
            Method getEnchantmentMap = itemClass.getMethod("getEnchantments");
            Object enchantments = getEnchantmentMap.invoke(nmsItem);

            if (enchantments instanceof Map) {
                for (Object key : ((Map<?, ?>) enchantments).keySet()) {
                    String keyName = key == null ? "" : key.toString().toUpperCase();
                    if (keyName.contains(nmsName.toUpperCase()) || keyName.contains(EnchantmentRegistry.canonicalName(nmsName))) {
                        return ((Map<?, Integer>) enchantments).get(key);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to Bukkit
        }
        return item.getEnchantments().getOrDefault(enchantment, 0);
    }

    /**
     * Apply enchantment via NMS.
     */
    public ItemStack applyNms(ItemStack item, int level) {
        try {
            Object nmsItem = toNmsItem(item);
            if (nmsItem == null) return item;

            Class<?> itemClass = nmsItem.getClass();
            Method addEnchantment = itemClass.getMethod(
                "addEnchantment", getNmsEnchantClass(), int.class
            );

            // Get NMS enchantment from registry
            Object nmsEnchant = getNmsEnchantFromRegistry();
            if (nmsEnchant != null) {
                addEnchantment.invoke(nmsItem, nmsEnchant, level);
            }

            return fromNmsItem(nmsItem);
        } catch (Exception e) {
            // Fallback to Bukkit
            ItemStack cloned = item.clone();
            cloned.addEnchantment(enchantment, level);
            return cloned;
        }
    }

    private Object getNmsEnchantFromRegistry() {
        try {
            if (MODERN) {
                Class<?> enchantmentManagerClass = Reflection.getNMS(
                    "net.minecraft.world.item.enchantment.Enchantments"
                );
                for (String alias : EnchantmentRegistry.aliasesOf(nmsName)) {
                    try {
                        return enchantmentManagerClass.getField(alias.toUpperCase()).get(null);
                    } catch (Throwable ignored) { }
                }
                return null;
            } else {
                Class<?> enchantmentClass = getNmsEnchantClass();
                for (String alias : EnchantmentRegistry.aliasesOf(nmsName)) {
                    try {
                        java.lang.reflect.Field field = enchantmentClass.getField(alias.toUpperCase());
                        return field.get(null);
                    } catch (Throwable ignored) { }
                }
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Object toNmsItem(ItemStack item) {
        try {
            Class<?> craftItemStack = Reflection.getOBC("inventory.CraftItemStack");
            return craftItemStack.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack fromNmsItem(Object nmsItem) {
        try {
            Class<?> craftItemStack = Reflection.getOBC("inventory.CraftItemStack");
            return (ItemStack) craftItemStack.getMethod("asBukkitCopy", nmsItem.getClass()).invoke(null, nmsItem);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get all available enchantments.
     */
    public static Set<String> getAvailableEnchantments() {
        Set<String> enchants = new HashSet<>();
        for (Enchantment enchant : Enchantment.values()) {
            enchants.add(enchant.getName());
            enchants.addAll(EnchantmentRegistry.aliasesOf(enchant.getName()));
        }
        return enchants;
    }

    /**
     * Check if enchantment exists.
     */
    public static boolean exists(String name) {
        return EnchantmentRegistry.resolve(name) != null;
    }

    // Getters
    public Enchantment getEnchantment() { return enchantment; }
    public String getNmsName() { return nmsName; }
}

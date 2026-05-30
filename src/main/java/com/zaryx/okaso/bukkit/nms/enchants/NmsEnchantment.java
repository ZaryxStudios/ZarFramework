package com.zaryx.okaso.bukkit.nms.enchants;

import com.zaryx.okaso.bukkit.utility.Reflection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class NmsEnchantment {

    // Wraps a Bukkit enchantment and provides version-aware NMS access.
    private static final String VERSION = Reflection.getNmsVersion();
    private static final boolean MODERN = Reflection.isModernNms();

    private static final Map<String, Object> nmsEnchantCache = new HashMap<>();

    // Stores the Bukkit enchantment, resolved NMS name, and namespaced key.
    private final Enchantment enchantment;
    private final String nmsName;
    private final String namespacedKey;

    public NmsEnchantment(Enchantment enchantment) {
        this.enchantment = enchantment;
        this.nmsName = getNmsName(enchantment);
        this.namespacedKey = buildNamespacedKey(this.nmsName);
    }

    public NmsEnchantment(String enchantmentName) {
        this.enchantment = EnchantmentRegistry.resolve(enchantmentName);
        this.nmsName = this.enchantment != null
                ? getNmsName(this.enchantment)
                : EnchantmentRegistry.canonicalName(enchantmentName);
        this.namespacedKey = buildNamespacedKey(this.nmsName);
    }

    private String getNmsName(Enchantment enchantment) {
        if (enchantment == null) return "";
        String name = EnchantmentRegistry.canonicalName(enchantment.getName());
        if (MODERN) {
            return "minecraft:" + name.toLowerCase();
        }
        return name;
    }

    private String buildNamespacedKey(String name) {
        if (name == null) return "";
        if (name.contains(":")) return name.toLowerCase();
        return "minecraft:" + name.toLowerCase();
    }

    public Class<?> getNmsEnchantClass() {
        if (MODERN) {
            return Reflection.getNMS("net.minecraft.world.item.enchantment.Enchantment");
        }
        return Reflection.getNMS("Enchantment");
    }

    public int getLevelFromNms(ItemStack item) {
        try {
            Object nmsItem = Reflection.toNmsItem(item);
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

        }
        return item.getEnchantments().getOrDefault(enchantment, 0);
    }

    public static Map<NmsEnchantment, Integer> getAllFromNms(ItemStack item) {
        Map<NmsEnchantment, Integer> result = new LinkedHashMap<>();
        try {
            Object nmsItem = Reflection.toNmsItem(item);
            if (nmsItem == null) return result;

            Class<?> itemClass = nmsItem.getClass();
            Method getEnchantmentMap = itemClass.getMethod("getEnchantments");
            Object enchantments = getEnchantmentMap.invoke(nmsItem);

            if (enchantments instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) enchantments).entrySet()) {
                    Object key = entry.getKey();
                    Integer level = (Integer) entry.getValue();
                    if (key != null && level != null) {
                        String keyName = key.toString();
                        Enchantment bukkitEnchant = EnchantmentRegistry.resolve(keyName);
                        if (bukkitEnchant != null) {
                            result.put(new NmsEnchantment(bukkitEnchant), level);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    public ItemStack applyNms(ItemStack item, int level) {
        try {
            Object nmsItem = Reflection.toNmsItem(item);
            if (nmsItem == null) return item;

            Class<?> itemClass = nmsItem.getClass();
            Method addEnchantment = itemClass.getMethod("addEnchantment", getNmsEnchantClass(), int.class);

            Object nmsEnchant = getNmsEnchantFromRegistry();
            if (nmsEnchant != null) {
                addEnchantment.invoke(nmsItem, nmsEnchant, level);
            }

            return Reflection.fromNmsItem(nmsItem);
        } catch (Exception e) {

            ItemStack cloned = item.clone();
            cloned.addEnchantment(enchantment, level);
            return cloned;
        }
    }

    public static ItemStack applyAllNms(ItemStack item, Map<Enchantment, Integer> enchantments) {
        ItemStack result = item.clone();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (entry.getKey() != null) {
                result = new NmsEnchantment(entry.getKey()).applyNms(result, entry.getValue());
            }
        }
        return result;
    }

    public ItemStack removeNms(ItemStack item) {
        try {
            Object nmsItem = Reflection.toNmsItem(item);
            if (nmsItem == null) return item;

            Class<?> itemClass = nmsItem.getClass();
            Method removeEnchantment = itemClass.getMethod("removeEnchantment", getNmsEnchantClass());

            Object nmsEnchant = getNmsEnchantFromRegistry();
            if (nmsEnchant != null) {
                removeEnchantment.invoke(nmsItem, nmsEnchant);
            }

            return Reflection.fromNmsItem(nmsItem);
        } catch (Exception e) {

            ItemStack cloned = item.clone();
            cloned.removeEnchantment(enchantment);
            return cloned;
        }
    }

    private Object getNmsEnchantFromRegistry() {
        String cacheKey = nmsName;
        if (nmsEnchantCache.containsKey(cacheKey)) {
            return nmsEnchantCache.get(cacheKey);
        }

        try {
            Object result = null;
            if (MODERN) {

                Class<?> enchantmentManagerClass = Reflection.getNMS(
                    "net.minecraft.world.item.enchantment.Enchantments"
                );
                for (String alias : EnchantmentRegistry.aliasesOf(nmsName)) {
                    try {
                        result = enchantmentManagerClass.getField(alias.toUpperCase()).get(null);
                        break;
                    } catch (Throwable ignored) {}
                }

                if (result == null) {

                    try {
                        Class<?> builtInRegistries = Reflection.getNMS("net.minecraft.core.registries.BuiltInRegistries");
                        Field enchantmentField = builtInRegistries.getDeclaredField("ENCHANTMENT");
                        enchantmentField.setAccessible(true);
                        Object registry = enchantmentField.get(null);
                        Method get = registry.getClass().getMethod("get", Reflection.getNMS("net.minecraft.resources.ResourceLocation"));
                        Object key = Reflection.newInstance(
                            Reflection.getNMS("net.minecraft.resources.ResourceLocation"),
                            namespacedKey
                        );
                        result = get.invoke(registry, key);
                    } catch (Throwable ignored) {}
                }
            } else {

                Class<?> enchantmentClass = getNmsEnchantClass();
                for (String alias : EnchantmentRegistry.aliasesOf(nmsName)) {
                    try {
                        Field field = enchantmentClass.getField(alias.toUpperCase());
                        result = field.get(null);
                        break;
                    } catch (Throwable ignored) {}
                }
            }

            if (result != null) {
                nmsEnchantCache.put(cacheKey, result);
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public static Set<String> getAvailableEnchantments() {
        Set<String> enchants = new HashSet<>();
        for (Enchantment enchant : Enchantment.values()) {
            enchants.add(enchant.getName());
            enchants.addAll(EnchantmentRegistry.aliasesOf(enchant.getName()));
        }
        return enchants;
    }

    public static boolean exists(String name) {
        return EnchantmentRegistry.resolve(name) != null;
    }

    public static void clearCache() {
        nmsEnchantCache.clear();
    }

    public Enchantment getEnchantment() { return enchantment; }
    public String getNmsName() { return nmsName; }
    public String getNamespacedKey() { return namespacedKey; }

    @Override
    public String toString() {
        return "NmsEnchantment{bukkit=" + (enchantment != null ? enchantment.getName() : "null") +
                ", nms=" + nmsName + ", key=" + namespacedKey + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NmsEnchantment)) return false;
        NmsEnchantment that = (NmsEnchantment) o;
        return Objects.equals(namespacedKey, that.namespacedKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespacedKey);
    }
}

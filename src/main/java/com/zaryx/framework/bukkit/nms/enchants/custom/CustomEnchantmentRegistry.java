package com.zaryx.framework.bukkit.nms.enchants.custom;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomEnchantmentRegistry {

    private static final Map<String, CustomEnchantment> ENCHANTMENTS = new ConcurrentHashMap<String, CustomEnchantment>();
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<String, String>();

    private CustomEnchantmentRegistry() {
    }

    public static boolean register(CustomEnchantment enchantment) {
        if (enchantment == null) {
            return false;
        }

        String key = enchantment.getKey();
        if (ENCHANTMENTS.containsKey(key) || ALIASES.containsKey(key)) {
            return false;
        }

        for (String alias : enchantment.getAliases()) {
            if (ENCHANTMENTS.containsKey(alias) || ALIASES.containsKey(alias)) {
                return false;
            }
        }

        ENCHANTMENTS.put(key, enchantment);
        for (String alias : enchantment.getAliases()) {
            ALIASES.put(alias, key);
        }
        return true;
    }

    public static CustomEnchantment resolve(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }

        CustomEnchantment enchantment = ENCHANTMENTS.get(normalized);
        if (enchantment != null) {
            return enchantment;
        }

        String alias = ALIASES.get(normalized);
        return alias == null ? null : ENCHANTMENTS.get(alias);
    }

    public static boolean unregister(String value) {
        CustomEnchantment enchantment = resolve(value);
        if (enchantment == null) {
            return false;
        }

        ENCHANTMENTS.remove(enchantment.getKey());
        ALIASES.entrySet().removeIf(entry -> enchantment.getKey().equals(entry.getValue()));
        return true;
    }

    public static Collection<CustomEnchantment> values() {
        return Collections.unmodifiableCollection(new ArrayList<CustomEnchantment>(ENCHANTMENTS.values()));
    }

    public static void clear() {
        ENCHANTMENTS.clear();
        ALIASES.clear();
    }

    public static Collection<CustomEnchantment> getApplicableEnchantments(ItemStack item) {
        return CustomEnchantmentHelper.getApplicableEnchantments(item);
    }

    public static Collection<CustomEnchantment> getEnchantingTableCandidates(ItemStack item) {
        return CustomEnchantmentHelper.getEnchantingTableCandidates(item);
    }

    public static List<CustomEnchantment> findConflictingEnchantments(CustomEnchantment enchantment) {
        if (enchantment == null) {
            return Collections.emptyList();
        }

        List<CustomEnchantment> conflicts = new ArrayList<CustomEnchantment>();
        for (String conflictKey : enchantment.getConflicts()) {
            CustomEnchantment conflict = resolve(conflictKey);
            if (conflict != null) {
                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    public static Collection<CustomEnchantment> findByItem(ItemStack item) {
        if (item == null) {
            return Collections.emptyList();
        }

        return new LinkedHashSet<CustomEnchantment>(getApplicableEnchantments(item));
    }

    private static String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
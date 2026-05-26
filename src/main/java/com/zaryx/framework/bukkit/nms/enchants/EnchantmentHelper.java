package com.zaryx.framework.bukkit.nms.enchants;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Enchantment helper utilities.
 */
public class EnchantmentHelper {

    /**
     * Apply multiple enchantments at once.
     */
    public static ItemStack applyAll(ItemStack item, Map<Enchantment, Integer> enchantments) {
        if (item == null) return null;
        
        ItemStack cloned = item.clone();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (entry.getKey() == null) continue;
            cloned.addEnchantment(entry.getKey(), Math.max(1, entry.getValue()));
        }
        return cloned;
    }

    /**
     * Apply enchantments safely (respecting level limits).
     */
    public static ItemStack applySafely(ItemStack item, Map<Enchantment, Integer> enchantments) {
        if (item == null) return null;
        
        ItemStack cloned = item.clone();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment ench = entry.getKey();
            if (ench == null) continue;
            int requested = Math.max(1, entry.getValue());
            int level = Math.min(requested, ench.getMaxLevel());
            cloned.addEnchantment(ench, level);
        }
        return cloned;
    }
    /**
     * Apply enchantments with options.
     * @param unsafe when true uses unsafe enchantments (bypasses max level checks)
     * @param overwriteConflicting when true will overwrite existing conflicting enchantments
     */
    public static ItemStack applyWithOptions(ItemStack item, Map<Enchantment, Integer> enchantments, boolean unsafe, boolean overwriteConflicting) {
        if (item == null) return null;

        ItemStack cloned = item.clone();
        ItemMeta meta = cloned.getItemMeta();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment ench = entry.getKey();
            if (ench == null) continue;
            int level = Math.max(1, entry.getValue());

            if (!overwriteConflicting) {
                boolean conflict = false;
                for (Enchantment existing : cloned.getEnchantments().keySet()) {
                    if (existing.conflictsWith(ench)) {
                        conflict = true;
                        break;
                    }
                }
                if (conflict) continue;
            }

            if (unsafe) {
                cloned.addUnsafeEnchantment(ench, level);
            } else {
                int applied = Math.min(level, ench.getMaxLevel());
                cloned.addEnchantment(ench, applied);
            }
        }

        // If the item is an enchanted book, try to apply stored enchants when appropriate
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) meta;
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                Enchantment ench = entry.getKey();
                if (ench == null) continue;
                int level = Math.max(1, entry.getValue());
                esm.addStoredEnchant(ench, level, false);
            }
            cloned.setItemMeta(esm);
        }

        return cloned;
    }


    /**
     * Get all enchantments as formatted strings.
     */
    public static List<String> getEnchantmentList(ItemStack item) {
        List<String> list = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return list;

        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String name = formatEnchantmentName(entry.getKey().getName());
            int level = entry.getValue();
            String roman = toRoman(level);
            list.add(name + " " + roman);
        }

        // Stored enchantments (enchanted books)
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) meta;
            Map<Enchantment, Integer> stored = esm.getStoredEnchants();
            for (Map.Entry<Enchantment, Integer> entry : stored.entrySet()) {
                String name = formatEnchantmentName(entry.getKey().getName());
                String roman = toRoman(entry.getValue());
                list.add(name + " " + roman + " (stored)");
            }
        }

        return list;
    }

    /**
     * Format enchantment name to readable format.
     */
    public static String formatEnchantmentName(String name) {
        if (name == null) return "";
        
        StringBuilder formatted = new StringBuilder();
        for (String part : name.toLowerCase().split("_")) {
            formatted.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }
        return formatted.toString().trim();
    }

    /**
     * Convert number to Roman numerals.
     */
    public static String toRoman(int number) {
        if (number <= 0) return "0";

        if (number <= 20) {
            String[] romanNumerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                    "XI","XII","XIII","XIV","XV","XVI","XVII","XVIII","XIX","XX"};
            return romanNumerals[number];
        }

        if (number <= 3999) {
            int[] vals = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
            String[] syms = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
            StringBuilder out = new StringBuilder();
            int i = 0;
            while (number > 0) {
                while (number >= vals[i]) {
                    number -= vals[i];
                    out.append(syms[i]);
                }
                i++;
            }
            return out.toString();
        }

        return String.valueOf(number);
    }

    /**
     * Check if item can receive enchantment.
     */
    public static boolean canEnchant(ItemStack item, Enchantment enchantment) {
        if (item == null) return false;
        if (enchantment == null) return false;
        try {
            return enchantment.canEnchantItem(item);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Get compatible enchantments for item.
     */
    public static List<Enchantment> getCompatibleEnchantments(ItemStack item) {
        List<Enchantment> compatible = new ArrayList<>();
        for (Enchantment enchant : Enchantment.values()) {
            try {
                if (enchant.canEnchantItem(item)) {
                    compatible.add(enchant);
                }
            } catch (Throwable ignored) { }
        }
        return compatible;
    }

    /**
     * Calculate experience cost for enchantment.
     */
    public static int calculateExpCost(int level) {
        if (level <= 0) return 0;
        return level * (10 + Math.min(level, 10));
    }

    /**
     * Create treasure enchantment set.
     */
    public static Set<Enchantment> getTreasureEnchantments() {
        Set<Enchantment> treasures = new HashSet<>();
        for (String alias : Arrays.asList("MENDING", "BINDING_CURSE", "VANISHING_CURSE", "SOUL_SPEED", "SWIFT_SNEAK")) {
            Enchantment enchantment = EnchantmentRegistry.resolve(alias);
            if (enchantment != null) {
                treasures.add(enchantment);
            }
        }
        return treasures;
    }
}
package com.zaryx.framework.bukkit.nms.enchants;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for working with Bukkit enchantments.
 * Rewritten to be defensive and clearer while remaining Java 1.8-compatible.
 */
public final class EnchantmentHelper {

    private EnchantmentHelper() {}

    public static ItemStack applyAll(ItemStack item, Map<Enchantment, Integer> enchantments) {
        if (item == null || enchantments == null || enchantments.isEmpty()) return item;

        ItemStack cloned = item.clone();
        for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
            Enchantment ench = e.getKey();
            if (ench == null) continue;
            int lvl = Math.max(1, safeInt(e.getValue()));
            try {
                cloned.addEnchantment(ench, lvl);
            } catch (Throwable ignored) {
                // skip problematic enchantments
            }
        }
        return cloned;
    }

    public static ItemStack applySafely(ItemStack item, Map<Enchantment, Integer> enchantments) {
        if (item == null || enchantments == null || enchantments.isEmpty()) return item;

        ItemStack cloned = item.clone();
        for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
            Enchantment ench = e.getKey();
            if (ench == null) continue;
            int requested = Math.max(1, safeInt(e.getValue()));
            int level = Math.min(requested, ench.getMaxLevel());
            try {
                cloned.addEnchantment(ench, level);
            } catch (Throwable ignored) {}
        }
        return cloned;
    }

    public static ItemStack applyWithOptions(ItemStack item, Map<Enchantment, Integer> enchantments, boolean unsafe, boolean overwriteConflicting) {
        if (item == null || enchantments == null || enchantments.isEmpty()) return item;

        ItemStack cloned = item.clone();
        ItemMeta meta = cloned.getItemMeta();

        // Pre-calc existing enchantments to check conflicts
        Map<Enchantment, Integer> existing = cloned.getEnchantments();

        for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
            Enchantment ench = e.getKey();
            if (ench == null) continue;
            int lvl = Math.max(1, safeInt(e.getValue()));

            if (!overwriteConflicting) {
                boolean conflict = false;
                for (Enchantment ex : existing.keySet()) {
                    try {
                        if (ex.conflictsWith(ench)) {
                            conflict = true;
                            break;
                        }
                    } catch (Throwable ignored) {}
                }
                if (conflict) continue;
            }

            try {
                if (unsafe) {
                    cloned.addUnsafeEnchantment(ench, lvl);
                } else {
                    int applied = Math.min(lvl, ench.getMaxLevel());
                    cloned.addEnchantment(ench, applied);
                }
            } catch (Throwable ignored) {}
        }

        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) meta;
            for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
                Enchantment ench = e.getKey();
                if (ench == null) continue;
                int lvl = Math.max(1, safeInt(e.getValue()));
                try {
                    esm.addStoredEnchant(ench, lvl, false);
                } catch (Throwable ignored) {}
            }
            cloned.setItemMeta(esm);
        }

        return cloned;
    }

    public static List<String> getEnchantmentList(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Collections.emptyList();

        List<String> out = new ArrayList<>();

        Map<Enchantment, Integer> enchants = item.getEnchantments();
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            Enchantment ench = e.getKey();
            if (ench == null) continue;
            String name = formatEnchantmentName(tryGetName(ench));
            String roman = toRoman(safeInt(e.getValue()));
            out.add(name + " " + roman);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta) meta;
            Map<Enchantment, Integer> stored = esm.getStoredEnchants();
            for (Map.Entry<Enchantment, Integer> e : stored.entrySet()) {
                Enchantment ench = e.getKey();
                if (ench == null) continue;
                String name = formatEnchantmentName(tryGetName(ench));
                String roman = toRoman(safeInt(e.getValue()));
                out.add(name + " " + roman + " (stored)");
            }
        }

        return out;
    }

    public static String formatEnchantmentName(String name) {
        if (name == null || name.isEmpty()) return "";
        StringBuilder b = new StringBuilder();
        String[] parts = name.toLowerCase().split("_");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            b.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) b.append(p.substring(1));
            b.append(' ');
        }
        return b.toString().trim();
    }

    public static String toRoman(int number) {
        if (number <= 0) return "0";
        // common small numbers
        if (number <= 20) {
            String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
            return r[number];
        }

        int[] vals = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] syms = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (number > 0 && i < vals.length) {
            while (number >= vals[i]) {
                number -= vals[i];
                sb.append(syms[i]);
            }
            i++;
        }
        return sb.toString();
    }

    public static boolean canEnchant(ItemStack item, Enchantment enchantment) {
        if (item == null || enchantment == null) return false;
        try {
            return enchantment.canEnchantItem(item);
        } catch (Throwable t) {
            return false;
        }
    }

    public static List<Enchantment> getCompatibleEnchantments(ItemStack item) {
        if (item == null) return Collections.emptyList();
        List<Enchantment> list = new ArrayList<>();
        for (Enchantment e : Enchantment.values()) {
            try {
                if (e.canEnchantItem(item)) list.add(e);
            } catch (Throwable ignored) {}
        }
        return list;
    }

    public static int calculateExpCost(int level) {
        if (level <= 0) return 0;
        return level * (10 + Math.min(level, 10));
    }

    public static Set<Enchantment> getTreasureEnchantments() {
        Set<Enchantment> treasures = new HashSet<>();
        String[] aliases = {"MENDING", "BINDING_CURSE", "VANISHING_CURSE", "SOUL_SPEED", "SWIFT_SNEAK"};
        for (String a : aliases) {
            Enchantment ench = EnchantmentRegistry.resolve(a);
            if (ench != null) treasures.add(ench);
        }
        return treasures;
    }

    // ----- Helpers -----
    private static int safeInt(Integer v) { return v == null ? 0 : v.intValue(); }

    private static String tryGetName(Enchantment e) {
        try {
            return e.getName();
        } catch (Throwable t) {
            return e != null ? e.toString() : "";
        }
    }
}

package com.zaryx.framework.bukkit.nms.enchants.custom;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CustomEnchantmentHelper {

    private static final String MARKER_PREFIX = "[ZCE:";

    private CustomEnchantmentHelper() {
    }

    public static ItemStack apply(ItemStack item, CustomEnchantment enchantment, int level) {
        if (item == null || enchantment == null) {
            return item;
        }

        return apply(item, java.util.Collections.singletonMap(enchantment, Integer.valueOf(level)));
    }

    public static ItemStack apply(ItemStack item, Map<CustomEnchantment, Integer> enchantments) {
        if (item == null || enchantments == null || enchantments.isEmpty()) {
            return item;
        }

        ItemStack cloned = item.clone();
        ItemMeta meta = cloned.getItemMeta();
        if (meta == null) {
            return cloned;
        }

        List<String> lore = meta.hasLore() ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
        for (Map.Entry<CustomEnchantment, Integer> entry : enchantments.entrySet()) {
            CustomEnchantment enchantment = entry.getKey();
            if (enchantment == null) {
                continue;
            }

            int level = Math.max(1, entry.getValue() == null ? 1 : entry.getValue().intValue());
            List<String> lines = enchantment.formatLoreLines(Math.min(level, enchantment.getMaxLevel()));
            for (String line : lines) {
                if (line != null && !lore.contains(line)) {
                    lore.add(line);
                }
            }

            String marker = buildMarker(enchantment, level);
            if (!lore.contains(marker)) {
                lore.add(marker);
            }
        }

        meta.setLore(lore);
        cloned.setItemMeta(meta);
        return cloned;
    }

    public static Map<CustomEnchantment, Integer> getEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Collections.emptyMap();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return Collections.emptyMap();
        }

        Map<CustomEnchantment, Integer> result = new LinkedHashMap<CustomEnchantment, Integer>();
        for (String line : meta.getLore()) {
            if (line == null) {
                continue;
            }

            String stripped = ChatColor.stripColor(line).trim();
            if (!stripped.startsWith(MARKER_PREFIX) || !stripped.endsWith("]")) {
                continue;
            }

            String payload = stripped.substring(MARKER_PREFIX.length(), stripped.length() - 1);
            String[] parts = payload.split(":");
            if (parts.length < 2) {
                continue;
            }

            CustomEnchantment enchantment = CustomEnchantmentRegistry.resolve(parts[0]);
            if (enchantment == null) {
                continue;
            }

            int level = 1;
            try {
                level = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }

            result.put(enchantment, Integer.valueOf(Math.max(1, level)));
        }

        return result;
    }

    public static boolean has(ItemStack item, CustomEnchantment enchantment) {
        if (item == null || enchantment == null) {
            return false;
        }

        return getEnchantments(item).containsKey(enchantment);
    }

    public static boolean canApplyTo(ItemStack item, CustomEnchantment enchantment) {
        if (item == null || enchantment == null) {
            return false;
        }

        return enchantment.canApplyTo(item);
    }

    public static Collection<CustomEnchantment> getApplicableEnchantments(ItemStack item) {
        if (item == null) {
            return Collections.emptyList();
        }

        List<CustomEnchantment> result = new ArrayList<CustomEnchantment>();
        for (CustomEnchantment enchantment : CustomEnchantmentRegistry.values()) {
            if (enchantment != null && enchantment.canApplyTo(item)) {
                result.add(enchantment);
            }
        }

        return result;
    }

    public static Collection<CustomEnchantment> getEnchantingTableCandidates(ItemStack item) {
        if (item == null) {
            return Collections.emptyList();
        }

        List<CustomEnchantment> result = new ArrayList<CustomEnchantment>();
        for (CustomEnchantment enchantment : CustomEnchantmentRegistry.values()) {
            if (enchantment != null && enchantment.canAppearInEnchantingTable(item)) {
                result.add(enchantment);
            }
        }

        return result;
    }

    private static String buildMarker(CustomEnchantment enchantment, int level) {
        return ChatColor.BLACK + "" + ChatColor.RESET + MARKER_PREFIX + enchantment.getKey() + ":" + Math.max(1, level) + "]";
    }
}
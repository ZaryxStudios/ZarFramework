package com.zaryx.okaso.api;

import com.zaryx.okaso.bukkit.nms.enchants.EnchantmentHelper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Public API facade for applying enchantments in a version-agnostic way.
 */
public final class EnchantmentsAPI {

    private EnchantmentsAPI() {}

    public static ItemStack apply(ItemStack item, Map<Enchantment, Integer> enchants) {
        return EnchantmentHelper.applyAll(item, enchants);
    }

    public static ItemStack applySafely(ItemStack item, Map<Enchantment, Integer> enchants) {
        return EnchantmentHelper.applySafely(item, enchants);
    }

    public static ItemStack applyWithOptions(ItemStack item, Map<Enchantment, Integer> enchants, boolean unsafe, boolean overwriteConflicting) {
        return EnchantmentHelper.applyWithOptions(item, enchants, unsafe, overwriteConflicting);
    }
}

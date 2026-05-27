package com.zaryx.framework.bukkit.menu.adapter;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for resolving materials and item stacks for menu items.
 * Handles legacy material names and null-safety.
 */
public final class MenuResolver {

    private static final Logger LOGGER = Logger.getLogger(MenuResolver.class.getName());

    private MenuResolver() {}

    /**
     * Resolve a material name string to a Material constant.
     * Returns {@link Material#AIR} for null or unrecognized names.
     */
    public static Material resolve(String name) {
        if (name == null || name.isEmpty()) return Material.AIR;

        String normalized = name.toUpperCase(Locale.ROOT).replace(' ', '_');

        // Try modern name first
        Material material = Material.matchMaterial(normalized);
        if (material != null) return material;

        // Try legacy name
        try {
            Material legacy = Material.getMaterial("LEGACY_" + normalized);
            if (legacy != null) return legacy;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to resolve legacy material: LEGACY_" + normalized, e);
        }

        return Material.AIR;
    }

    /**
     * Resolve an ItemStack, ensuring it is never null.
     * Returns a single AIR item if the input is null.
     * Clones the input to prevent external mutation.
     */
    public static ItemStack resolve(ItemStack itemStack) {
        if (itemStack == null) return new ItemStack(Material.AIR);

        Material resolved = resolve(itemStack.getType().name());
        ItemStack clone = new ItemStack(resolved, itemStack.getAmount());
        if (itemStack.hasItemMeta()) {
            clone.setItemMeta(itemStack.getItemMeta());
        }
        return clone;
    }

    /**
     * Create a basic ItemStack from a Material with amount 1.
     */
    public static ItemStack of(Material material) {
        return material != null ? new ItemStack(material, 1) : new ItemStack(Material.AIR);
    }

    /**
     * Create a basic ItemStack from a Material with the specified amount.
     */
    public static ItemStack of(Material material, int amount) {
        return material != null ? new ItemStack(material, amount) : new ItemStack(Material.AIR);
    }
}

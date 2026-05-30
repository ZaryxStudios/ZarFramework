package com.zaryx.okaso.bukkit.menu.adapter;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MenuResolver {

    private static final Logger LOGGER = Logger.getLogger(MenuResolver.class.getName());

    private MenuResolver() {}

    public static Material resolve(String name) {
        if (name == null || name.isEmpty()) return Material.AIR;

        String normalized = name.toUpperCase(Locale.ROOT).replace(' ', '_');

        Material material = Material.matchMaterial(normalized);
        if (material != null) return material;

        try {
            Material legacy = Material.getMaterial("LEGACY_" + normalized);
            if (legacy != null) return legacy;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to resolve legacy material: LEGACY_" + normalized, e);
        }

        return Material.AIR;
    }

    public static ItemStack resolve(ItemStack itemStack) {
        if (itemStack == null) return new ItemStack(Material.AIR);

        Material resolved = resolve(itemStack.getType().name());
        ItemStack clone = new ItemStack(resolved, itemStack.getAmount());
        if (itemStack.hasItemMeta()) {
            clone.setItemMeta(itemStack.getItemMeta());
        }
        return clone;
    }

    public static ItemStack of(Material material) {
        return material != null ? new ItemStack(material, 1) : new ItemStack(Material.AIR);
    }

    public static ItemStack of(Material material, int amount) {
        return material != null ? new ItemStack(material, amount) : new ItemStack(Material.AIR);
    }
}

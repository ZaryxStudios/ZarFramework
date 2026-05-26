package com.zaryx.framework.bukkit.menu.adapter;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class MenuResolver {

    private MenuResolver() {}

    public static Material resolve(String name) {
        if (name == null) return Material.AIR;

        name = name.toLowerCase(Locale.ROOT);

        Material material = Material.matchMaterial(name);
        if (material != null) return material;

        material = Material.getMaterial("LEGACY_" + name);
        if (material != null) return material;

        return Material.AIR;
    }

    public static ItemStack resolve(ItemStack itemStack) {
        if (itemStack == null) return new ItemStack(Material.AIR);

        Material resolved = resolve(itemStack.getType().name());

        ItemStack clone = new ItemStack(resolved, itemStack.getAmount());
        if (itemStack.hasItemMeta()) clone.setItemMeta(itemStack.getItemMeta());

        return clone;
    }
}
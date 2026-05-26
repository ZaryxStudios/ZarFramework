package com.zaryx.framework.api;

import com.zaryx.framework.bukkit.FrameworkPlugin;
import com.zaryx.framework.bukkit.nms.enchants.custom.CustomEnchantmentListener;
import com.zaryx.framework.bukkit.nms.enchants.custom.CustomEnchantment;
import com.zaryx.framework.bukkit.nms.enchants.custom.CustomEnchantmentHelper;
import com.zaryx.framework.bukkit.nms.enchants.custom.CustomEnchantmentRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;

public final class CustomEnchantmentsAPI {

    private static volatile CustomEnchantmentListener listener;

    private CustomEnchantmentsAPI() {
    }

    public static boolean register(CustomEnchantment enchantment) {
        return CustomEnchantmentRegistry.register(enchantment);
    }

    public static boolean unregister(String value) {
        return CustomEnchantmentRegistry.unregister(value);
    }

    public static CustomEnchantment get(String value) {
        return CustomEnchantmentRegistry.resolve(value);
    }

    public static ItemStack apply(ItemStack item, CustomEnchantment enchantment, int level) {
        return CustomEnchantmentHelper.apply(item, enchantment, level);
    }

    public static ItemStack apply(ItemStack item, Map<CustomEnchantment, Integer> enchantments) {
        return CustomEnchantmentHelper.apply(item, enchantments);
    }

    public static Collection<CustomEnchantment> getApplicableEnchantments(ItemStack item) {
        return CustomEnchantmentRegistry.getApplicableEnchantments(item);
    }

    public static Collection<CustomEnchantment> getEnchantingTableCandidates(ItemStack item) {
        return CustomEnchantmentRegistry.getEnchantingTableCandidates(item);
    }

    public static Map<CustomEnchantment, Integer> read(ItemStack item) {
        return CustomEnchantmentHelper.getEnchantments(item);
    }

    public static boolean canApply(ItemStack item, CustomEnchantment enchantment) {
        return CustomEnchantmentHelper.canApplyTo(item, enchantment);
    }

    public static void registerRuntimeListener(JavaPlugin plugin) {
        if (plugin == null) {
            return;
        }

        if (listener == null) {
            listener = new CustomEnchantmentListener();
        }

        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    public static void registerRuntimeListener() {
        Plugin plugin = FrameworkPlugin.getInstance();
        if (plugin instanceof JavaPlugin) {
            registerRuntimeListener((JavaPlugin) plugin);
        }
    }

    public static void unregisterRuntimeListener() {
        listener = null;
    }
}

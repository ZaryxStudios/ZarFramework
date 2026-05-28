package com.zaryx.okaso.bukkit.nms.materials;

import com.zaryx.okaso.api.CustomEnchantmentsAPI;
import com.zaryx.okaso.api.EnchantmentsAPI;
import com.zaryx.okaso.bukkit.nms.enchants.custom.CustomEnchantment;
import com.zaryx.okaso.bukkit.nms.enchants.custom.CustomEnchantmentRegistry;
import com.zaryx.okaso.bukkit.utility.Text;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ItemStack builder with reflection guards for modern-only features.
 */
public class ItemStackBuilder {

    private static final Logger LOGGER = Logger.getLogger(ItemStackBuilder.class.getName());

    private Material material = Material.STONE;
    private int amount = 1;
    private short durability = 0;
    private String displayName;
    private List<String> lore;
    private final Map<Enchantment, Integer> enchantments = new HashMap<Enchantment, Integer>();
    private final Map<Enchantment, Integer> unsafeEnchantments = new HashMap<Enchantment, Integer>();
    private final Map<CustomEnchantment, Integer> customEnchantments = new LinkedHashMap<CustomEnchantment, Integer>();
    private boolean unbreakable;
    private Integer customModelData;

    public ItemStackBuilder() {
    }

    public ItemStackBuilder(Material material) {
        this.material = material == null ? Material.STONE : material;
    }

    public ItemStackBuilder material(Material material) {
        this.material = material == null ? Material.STONE : material;
        return this;
    }

    public ItemStackBuilder amount(int amount) {
        this.amount = Math.max(1, amount);
        return this;
    }

    public ItemStackBuilder durability(short durability) {
        this.durability = durability;
        return this;
    }

    public ItemStackBuilder name(String name) {
        this.displayName = name;
        return this;
    }

    public ItemStackBuilder lore(String... lines) {
        this.lore = lines == null ? null : Arrays.asList(lines);
        return this;
    }

    public ItemStackBuilder lore(List<String> lines) {
        this.lore = lines;
        return this;
    }

    public ItemStackBuilder enchant(Enchantment enchantment, int level) {
        if (enchantment != null) {
            this.unsafeEnchantments.put(enchantment, level);
        }
        return this;
    }

    public ItemStackBuilder safeEnchant(Enchantment enchantment, int level) {
        if (enchantment != null) {
            this.enchantments.put(enchantment, level);
        }
        return this;
    }

    public ItemStackBuilder unsafeEnchant(Enchantment enchantment, int level) {
        if (enchantment != null) {
            this.unsafeEnchantments.put(enchantment, level);
        }
        return this;
    }

    public ItemStackBuilder customEnchant(CustomEnchantment enchantment, int level) {
        if (enchantment != null) {
            this.customEnchantments.put(enchantment, Math.max(1, level));
        }
        return this;
    }

    public ItemStackBuilder customEnchant(String enchantmentKey, int level) {
        CustomEnchantment enchantment = CustomEnchantmentRegistry.resolve(enchantmentKey);
        if (enchantment != null) {
            this.customEnchantments.put(enchantment, Math.max(1, level));
        }
        return this;
    }

    public ItemStackBuilder unbreakable(boolean value) {
        this.unbreakable = value;
        return this;
    }

    public ItemStackBuilder customModelData(int data) {
        this.customModelData = data;
        return this;
    }

    public ItemStack build() {
        ItemStack stack = new ItemStack(material, amount, durability);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(Text.text(displayName));
            }
            if (lore != null) {
                meta.setLore(Text.text(lore));
            }

            applyUnbreakable(meta, unbreakable);
            if (customModelData != null) {
                applyCustomModelData(meta, customModelData.intValue());
            }

            stack.setItemMeta(meta);
        }

        // Apply safe enchantments first, then unsafe ones using the compatibility API
        if (!enchantments.isEmpty()) {
            stack = EnchantmentsAPI.applySafely(stack, enchantments);
        }

        if (!unsafeEnchantments.isEmpty()) {
            stack = EnchantmentsAPI.applyWithOptions(stack, unsafeEnchantments, true, true);
        }

        if (!customEnchantments.isEmpty()) {
            stack = CustomEnchantmentsAPI.apply(stack, customEnchantments);
        }

        return stack;
    }

    public Object buildNms() {
        return new NmsMaterial(material).toNms(build());
    }

    private static void applyUnbreakable(ItemMeta meta, boolean value) {
        try {
            Method spigot = meta.getClass().getMethod("spigot");
            Object spigotMeta = spigot.invoke(meta);
            Method setUnbreakable = spigotMeta.getClass().getMethod("setUnbreakable", boolean.class);
            setUnbreakable.invoke(spigotMeta, value);
            return;
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Spigot setUnbreakable failed, falling back to standard", t);
        }

        try {
            Method setUnbreakable = meta.getClass().getMethod("setUnbreakable", boolean.class);
            setUnbreakable.invoke(meta, value);
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Standard setUnbreakable failed", t);
        }
    }

    /**
     * Applies custom model data via reflection with silent fallback.
     */
    private static void applyCustomModelData(ItemMeta meta, int value) {
        try {
            Method setCustomModelData = meta.getClass().getMethod("setCustomModelData", Integer.class);
            setCustomModelData.invoke(meta, Integer.valueOf(value));
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to set custom model data", t);
        }
    }
}

package com.zaryx.okaso.bukkit.utility;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class Item {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public Item(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemMeta = itemStack.getItemMeta();
    }

    public Item(Material material) {
        this(material, 1, 0);
    }

    public Item(Material material, int amount) {
        this(material, amount, 0);
    }

    public Item(Material material, int amount, int durability) {
        this.itemStack = new ItemStack(material, amount, (short) durability);
        this.itemMeta = itemStack.getItemMeta();
    }

    public Item setDurability(int durability) {
        this.itemStack.setDurability((short) durability);
        return this;
    }

    public Item setAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public Item addAmount(int amount) {
        this.itemStack.setAmount(this.itemStack.getAmount() + amount);
        return this;
    }

    public Item setName(String name) {
        this.itemMeta.setDisplayName(Convert.text(name));
        return this;
    }

    public Item setData(byte data) {
        MaterialData dataOld = this.itemStack.getData();
        dataOld.setData(data);
        return this;
    }

    public Item setSkullOwner(String owner) {
        try {
            ((SkullMeta) this.itemMeta).setOwner(owner);
        } catch(ClassCastException ignored) { }

        return this;
    }

    public Item setLore(List<String> lore) {
        this.itemMeta.setLore(Convert.text(lore));
        return this;
    }

    public Item setLore(String... lore) {
        this.itemMeta.setLore(Convert.text(Arrays.asList(lore)));
        return this;
    }

    public Item addLoreLine(String line) {
        List<String> lore = this.itemMeta.getLore();
        if (lore == null) {
            lore = new ArrayList<String>();
        }

        lore.add(Convert.text(line));
        this.itemMeta.setLore(lore);

        return this;
    }

    public Item enchant(Enchantment enchantment, int level) {
        return this.addEnchantment(enchantment, level);
    }

    public Item unsafeEnchant(Enchantment enchantment, int level) {
        return this.addUnsafeEnchantment(enchantment, level);
    }

    public Item addStoredEnchantment(Enchantment enchantment, int level) {
        try {
            ((EnchantmentStorageMeta) this.itemMeta).addStoredEnchant(enchantment, level, false);
        } catch(ClassCastException ignored) { }

        return this;
    }

    public Item addEnchantment(Enchantment enchantment, int level) {
        if (enchantment != null && level > 0) {
            this.itemStack.addEnchantment(enchantment, level);
        }
        return this;
    }

    public Item addUnsafeEnchantment(Enchantment enchantment, int level) {
        if (enchantment != null && level > 0) {
            this.itemStack.addUnsafeEnchantment(enchantment, level);
        }
        return this;
    }

    public ItemStack build() {
        if (this.itemMeta != null) {
            this.itemStack.setItemMeta(this.itemMeta);
        }
        return this.itemStack;
    }
}

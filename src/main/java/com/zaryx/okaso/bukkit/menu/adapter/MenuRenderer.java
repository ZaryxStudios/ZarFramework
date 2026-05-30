package com.zaryx.okaso.bukkit.menu.adapter;

import com.zaryx.okaso.bukkit.menu.core.Menu;
import com.zaryx.okaso.bukkit.menu.extra.MenuItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MenuRenderer {

    private static final Logger LOGGER = Logger.getLogger(MenuRenderer.class.getName());

    private MenuRenderer() {}

    public static void render(Menu menu) {
        if (menu == null) return;
        Inventory inventory = menu.getInventory();
        MenuItem[] items = menu.getItems();
        if (inventory == null || items == null) return;

        int limit = Math.min(items.length, inventory.getSize());
        for (int i = 0; i < limit; i++) {
            ItemStack newItem = items[i] != null ? items[i].getItemStack() : null;
            ItemStack oldItem = inventory.getItem(i);
            if (!isSame(oldItem, newItem)) {
                inventory.setItem(i, newItem);
            }
        }
    }

    public static void render(Menu menu, BitSet dirtySlots) {
        if (menu == null || dirtySlots == null || dirtySlots.isEmpty()) return;
        Inventory inventory = menu.getInventory();
        MenuItem[] items = menu.getItems();
        if (inventory == null || items == null) return;

        int size = Math.min(items.length, inventory.getSize());
        for (int i = dirtySlots.nextSetBit(0); i >= 0 && i < size; i = dirtySlots.nextSetBit(i + 1)) {
            ItemStack newItem = items[i] != null ? items[i].getItemStack() : null;
            ItemStack oldItem = inventory.getItem(i);
            if (!isSame(oldItem, newItem)) {
                inventory.setItem(i, newItem);
            }
        }
    }

    private static boolean isSame(ItemStack a, ItemStack b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
    }
}

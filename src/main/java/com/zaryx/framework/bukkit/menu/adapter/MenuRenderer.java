package com.zaryx.framework.bukkit.menu.adapter;

import com.zaryx.framework.bukkit.menu.core.Menu;
import com.zaryx.framework.bukkit.menu.extra.MenuItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MenuRenderer {

    private MenuRenderer() {}

    public static void render(Menu menu) {
        if (menu == null) {
            return;
        }

        Inventory inventory = menu.getInventory();
        MenuItem[] items = menu.getItems();

        if (inventory == null || items == null) {
            return;
        }

        for (int i = 0; i < items.length; i++) {
            ItemStack newItem = items[i] != null ? items[i].getItemStack() : null;
            ItemStack oldItem = i < inventory.getSize() ? inventory.getItem(i) : null;

            if (!isSame(oldItem, newItem)) {
                if (i < inventory.getSize()) {
                    inventory.setItem(i, newItem);
                }
            }
        }
    }

    private static boolean isSame(ItemStack a, ItemStack b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
    }
}

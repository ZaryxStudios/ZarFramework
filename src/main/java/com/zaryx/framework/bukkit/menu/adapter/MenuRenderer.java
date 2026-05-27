package com.zaryx.framework.bukkit.menu.adapter;

import com.zaryx.framework.bukkit.menu.core.Menu;
import com.zaryx.framework.bukkit.menu.extra.MenuItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renders menu items into the backing Bukkit {@link Inventory}.
 * <p>
 * Supports two modes:
 * <ul>
 *   <li>{@link #render(Menu)} – full render: writes every slot.</li>
 *   <li>{@link #render(Menu, BitSet)} – differential render: writes only
 *       the slots flagged in the given {@link BitSet}.</li>
 * </ul>
 * Both modes skip writes when the new {@link ItemStack} is identical to the
 * existing one, minimising network packets.
 */
public final class MenuRenderer {

    private static final Logger LOGGER = Logger.getLogger(MenuRenderer.class.getName());

    private MenuRenderer() {}

    /**
     * Full render: re-renders all slots in the menu inventory.
     *
     * @param menu the menu to render (null-safe)
     */
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

    /**
     * Differential render: only re-renders slots flagged as dirty.
     * <p>
     * Skips slots where the item is already identical to avoid unnecessary
     * inventory writes and network traffic.
     *
     * @param menu       the menu to render (null-safe)
     * @param dirtySlots a {@link BitSet} whose set bits indicate dirty slots
     */
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

    /**
     * Compare two {@link ItemStack}s for visual and quantity equality.
     * <p>
     * Two stacks are considered the same when they are both null, or when
     * {@link ItemStack#isSimilar} returns {@code true} <em>and</em> the
     * stack sizes match.
     *
     * @param a the first stack (may be null)
     * @param b the second stack (may be null)
     * @return {@code true} if both stacks represent the same item with the same amount
     */
    private static boolean isSame(ItemStack a, ItemStack b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
    }
}

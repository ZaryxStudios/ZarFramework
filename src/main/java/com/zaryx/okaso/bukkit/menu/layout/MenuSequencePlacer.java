package com.zaryx.okaso.bukkit.menu.layout;

import com.zaryx.okaso.bukkit.menu.core.Menu;
import com.zaryx.okaso.bukkit.menu.extra.MenuItem;

import java.util.*;

/**
 * Places a sequence of {@link MenuItem}s into a menu following a defined slot order,
 * optionally excluding specific slots.
 * <p>
 * Useful for filling content areas that skip decoration slots, borders, or
 * navigation positions.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * // Place items into slots 1–7 and 10–16, skipping slot 13
 * List<Integer> slots = Arrays.asList(1,2,3,4,5,6,7,10,11,12,13,14,15,16);
 * Set<Integer> excluded = MenuSequencePlacer.excludeSlots(13);
 * MenuSequencePlacer.place(menu, slots, items, excluded);
 * }</pre>
 */
public final class MenuSequencePlacer {

    private MenuSequencePlacer() {}

    /**
     * Place items into the given available slots, in order, skipping excluded slots.
     * <p>
     * Slots that have no corresponding item are set to {@code null} (cleared).
     * If there are more items than available slots, an {@link IllegalArgumentException}
     * is thrown.
     *
     * @param menu           the target menu (null-safe)
     * @param availableSlots the ordered slot indices to fill (null-safe)
     * @param items          the items to place (null-safe; null is treated as empty)
     * @param excludedSlots  slots to skip (null-safe; null is treated as empty)
     * @throws IllegalArgumentException if a slot is out of bounds or there are too many items
     */
    public static void place(Menu menu, List<Integer> availableSlots, List<MenuItem> items, Set<Integer> excludedSlots) {
        if (menu == null || availableSlots == null) {
            return;
        }

        List<MenuItem> source = items != null ? items : Collections.<MenuItem>emptyList();
        Set<Integer> exclusions = excludedSlots != null ? excludedSlots : Collections.<Integer>emptySet();

        int size = Math.max(0, menu.getRows() * 9);
        for (Integer slot : exclusions) {
            if (slot == null) {
                continue;
            }
            if (slot < 0 || slot >= size) {
                throw new IllegalArgumentException("Excluded slot " + slot + " is out of bounds [0, " + (size - 1) + "]");
            }
        }

        int index = 0;
        for (Integer slot : availableSlots) {
            if (slot == null) {
                continue;
            }
            if (slot < 0 || slot >= size) {
                throw new IllegalArgumentException("Slot " + slot + " is out of bounds [0, " + (size - 1) + "]");
            }
            if (exclusions.contains(slot)) {
                continue;
            }

            menu.setItem(slot, index < source.size() ? source.get(index++) : null);
        }

        if (index < source.size()) {
            throw new IllegalArgumentException("Not enough available slots for all menu items (" + source.size() + " items, " + index + " placed)");
        }
    }

    /**
     * Convenience method to create an exclusion set from var-args slot indices.
     *
     * @param slots the slot indices to exclude (null-safe)
     * @return a mutable {@link HashSet} of the given slots
     */
    public static Set<Integer> excludeSlots(int... slots) {
        Set<Integer> set = new HashSet<>();
        if (slots == null) {
            return set;
        }
        for (int slot : slots) {
            set.add(slot);
        }
        return set;
    }
}

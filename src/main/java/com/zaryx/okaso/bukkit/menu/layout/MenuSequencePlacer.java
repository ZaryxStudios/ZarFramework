package com.zaryx.okaso.bukkit.menu.layout;

import com.zaryx.okaso.bukkit.menu.core.Menu;
import com.zaryx.okaso.bukkit.menu.extra.MenuItem;

import java.util.*;

public final class MenuSequencePlacer {

    private MenuSequencePlacer() {}

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

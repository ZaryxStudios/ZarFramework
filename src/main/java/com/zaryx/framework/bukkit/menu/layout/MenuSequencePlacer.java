package com.zaryx.framework.bukkit.menu.layout;

import com.zaryx.framework.bukkit.menu.core.Menu;
import com.zaryx.framework.bukkit.menu.extra.MenuItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MenuSequencePlacer {

    public  void place(Menu menu, List<Integer> availableSlots, List<MenuItem> items, Set<Integer> excludedSlots) {
        int size = menu.getRows() * 9;

        for (int slot : excludedSlots) {
            if (slot < 0 || slot >= size) {
                throw new IllegalArgumentException("Slot " + slot + " is out of bounds");
            }
        }

        int index = 0;

        for (int slot : availableSlots) {
            if (excludedSlots.contains(slot)) continue;

            if (index >= items.size()) {
                menu.setItem(slot, null);
                continue;
            }

            menu.setItem(slot, items.get(index++));
        }

        if (index < items.size()) {
            throw new IllegalArgumentException("Slot " + index + " is out of bounds");
        }
    }

    public static Set<Integer> excludeSlots(int... slots) {
        Set<Integer> set = new HashSet<>();
        for (int slot : slots) set.add(slot);
        return set;
    }
}

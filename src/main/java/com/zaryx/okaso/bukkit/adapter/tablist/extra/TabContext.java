package com.zaryx.okaso.bukkit.adapter.tablist.extra;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class TabContext {

    private final Map<Integer, TabEntry> entries = new HashMap<>();

    public void set(int column, int row, TabEntry entry) {
        entries.put(this.getSlot(column, row), entry);
    }

    public TabEntry get(int slot) {
        return entries.get(slot);
    }

    public TabEntry get(int column, int row) {
        return entries.get(getSlot(column, row));
    }

    /* ================= SLOT ================= */

    public int getSlot(int column, int row) {

        if (column < 0 || column > 3) {
            throw new IllegalArgumentException("Column must be 0–3");
        }

        if (row < 0 || row > 19) {
            throw new IllegalArgumentException("Row must be 0–19");
        }

        return column * 20 + row;
    }

    /* ================= UTIL ================= */

    public boolean contains(int column, int row) {
        return entries.containsKey(getSlot(column, row));
    }

    public void clear() {
        entries.clear();
    }
}

package com.zaryx.okaso.bukkit.menu.adapter;

public final class MenuColumn {

    public static final int INVENTORY_WIDTH = 9;

    private MenuColumn() {}

    public static int convert(int column, int row) {
        if (column < 0 || column >= INVENTORY_WIDTH) {
            throw new IllegalArgumentException("Column must be between 0 and " + (INVENTORY_WIDTH - 1));
        }
        if (row < 0) {
            throw new IllegalArgumentException("Row must be non-negative");
        }
        return (row * INVENTORY_WIDTH) + column;
    }

    public static int toColumn(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must be non-negative");
        }
        return slot % INVENTORY_WIDTH;
    }

    public static int toRow(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must be non-negative");
        }
        return slot / INVENTORY_WIDTH;
    }
}

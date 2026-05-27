package com.zaryx.framework.bukkit.menu.adapter;

/**
 * Utility for converting between (column, row) grid coordinates and flat slot indices.
 * <p>
 * Bukkit inventories use a flat slot index where slot {@code 0} is the top-left.
 * This helper translates two-dimensional coordinates into that flat index.
 *
 * <pre>
 *   col 0  1  2  3  4  5  6  7  8
 * row 0 [  ][  ][  ][  ][  ][  ][  ][  ][  ]
 * row 1 [  ][  ][  ][  ][  ][  ][  ][  ][  ]
 * row 2 [  ][  ][  ][  ][  ][  ][  ][  ][  ]
 * </pre>
 */
public final class MenuColumn {

    /** Number of columns in every Bukkit chest inventory row. */
    public static final int INVENTORY_WIDTH = 9;

    private MenuColumn() {}

    /**
     * Convert a (column, row) pair to a flat slot index.
     *
     * @param column zero-based column (0–8)
     * @param row    zero-based row (must be &ge; 0)
     * @return the flat slot index
     * @throws IllegalArgumentException if column is outside [0, 8] or row is negative
     */
    public static int convert(int column, int row) {
        if (column < 0 || column >= INVENTORY_WIDTH) {
            throw new IllegalArgumentException("Column must be between 0 and " + (INVENTORY_WIDTH - 1));
        }
        if (row < 0) {
            throw new IllegalArgumentException("Row must be non-negative");
        }
        return (row * INVENTORY_WIDTH) + column;
    }

    /**
     * Extract the column (0–8) from a flat slot index.
     *
     * @param slot the flat slot index (must be &ge; 0)
     * @return the column component
     * @throws IllegalArgumentException if slot is negative
     */
    public static int toColumn(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must be non-negative");
        }
        return slot % INVENTORY_WIDTH;
    }

    /**
     * Extract the row from a flat slot index.
     *
     * @param slot the flat slot index (must be &ge; 0)
     * @return the row component
     * @throws IllegalArgumentException if slot is negative
     */
    public static int toRow(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must be non-negative");
        }
        return slot / INVENTORY_WIDTH;
    }
}

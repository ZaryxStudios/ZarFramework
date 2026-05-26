package com.zaryx.framework.bukkit.menu.adapter;

public final class MenuColumn {

    private MenuColumn() {}

    public static int convert(int column, int row) {
        if (column < 0 || row >= 9) {
            throw new IllegalArgumentException("Invalid column or row");
        }

        if (row < 0) {
            throw new IllegalArgumentException("Invalid row");
        }

        return (row * 9) + column;
    }
}

package com.zaryx.framework.bukkit.menu.extra;

import org.bukkit.event.inventory.InventoryType;

/**
 * Supported Bukkit inventory types for menus.
 * <p>
 * Each variant wraps a corresponding {@link InventoryType} that determines
 * the layout and behaviour of the backing inventory.
 */
public enum MenuType {

    /** Standard chest inventory with configurable rows (default). */
    CHEST(InventoryType.CHEST),

    /** 5-slot hopper inventory. */
    HOPPER(InventoryType.HOPPER),

    /** 9-slot dropper inventory. */
    DROPPER(InventoryType.DROPPER),

    /** 9-slot dispenser inventory. */
    DISPENSER(InventoryType.DISPENSER),

    /** Furnace inventory (input, fuel, output). */
    FURNACE(InventoryType.FURNACE);

    private final InventoryType type;

    MenuType(InventoryType type) {
        this.type = type;
    }

    /**
     * Get the underlying Bukkit inventory type.
     *
     * @return the Bukkit {@link InventoryType}
     */
    public InventoryType getType() {
        return this.type;
    }
}

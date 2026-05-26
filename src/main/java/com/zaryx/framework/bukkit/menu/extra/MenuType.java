package com.zaryx.framework.bukkit.menu.extra;

import org.bukkit.event.inventory.InventoryType;

public enum MenuType {

    CHEST(InventoryType.CHEST),
    HOPPER(InventoryType.HOPPER),
    DROPPER(InventoryType.DROPPER),
    DISPENSER(InventoryType.DISPENSER),
    FURNACE(InventoryType.FURNACE);

    private final InventoryType type;

    MenuType(InventoryType type) {
        this.type = type;
    }

    public InventoryType getType() {
        return this.type;
    }
}

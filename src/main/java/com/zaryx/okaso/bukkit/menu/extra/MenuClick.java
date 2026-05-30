package com.zaryx.okaso.bukkit.menu.extra;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Objects;

public final class MenuClick {
    private final Player player;
    private final ClickType clickType;

    public MenuClick(Player player, ClickType clickType) {
        this.player = Objects.requireNonNull(player, "player");
        this.clickType = Objects.requireNonNull(clickType, "clickType");
    }

    public Player getPlayer() {
        return this.player;
    }

    public ClickType getClickType() {
        return this.clickType;
    }

    public boolean isLeftClick() {
        return clickType == ClickType.LEFT;
    }

    public boolean isRightClick() {
        return clickType == ClickType.RIGHT;
    }

    public boolean isShiftClick() {
        return clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MenuClick)) return false;
        MenuClick that = (MenuClick) o;
        return player.equals(that.player) && clickType == that.clickType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, clickType);
    }

    @Override
    public String toString() {
        return "MenuClick{player=" + player.getName() + ", clickType=" + clickType + "}";
    }
}

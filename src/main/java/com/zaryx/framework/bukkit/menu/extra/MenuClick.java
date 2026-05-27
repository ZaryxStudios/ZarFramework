package com.zaryx.framework.bukkit.menu.extra;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Objects;

/**
 * Immutable snapshot of a menu click event.
 * <p>
 * Captures the player who clicked and the type of click performed.
 * Provides convenience methods for common click-type checks.
 *
 * @see MenuItem
 */
public final class MenuClick {
    private final Player player;
    private final ClickType clickType;

    /**
     * Construct a click snapshot.
     *
     * @param player    the clicking player (must not be null)
     * @param clickType the click type (must not be null)
     * @throws NullPointerException if either argument is null
     */
    public MenuClick(Player player, ClickType clickType) {
        this.player = Objects.requireNonNull(player, "player");
        this.clickType = Objects.requireNonNull(clickType, "clickType");
    }

    /**
     * Get the player who performed the click.
     *
     * @return the clicking player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Get the type of click performed.
     *
     * @return the click type
     */
    public ClickType getClickType() {
        return this.clickType;
    }

    /**
     * Check whether this was a left click.
     *
     * @return true if {@link ClickType#LEFT}
     */
    public boolean isLeftClick() {
        return clickType == ClickType.LEFT;
    }

    /**
     * Check whether this was a right click.
     *
     * @return true if {@link ClickType#RIGHT}
     */
    public boolean isRightClick() {
        return clickType == ClickType.RIGHT;
    }

    /**
     * Check whether this was a shift click (left or right).
     *
     * @return true if {@link ClickType#SHIFT_LEFT} or {@link ClickType#SHIFT_RIGHT}
     */
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

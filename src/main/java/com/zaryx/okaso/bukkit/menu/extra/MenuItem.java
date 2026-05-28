package com.zaryx.okaso.bukkit.menu.extra;

import com.zaryx.okaso.bukkit.menu.adapter.MenuResolver;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents an item in a menu with an optional click action.
 * Immutable by design: each factory method creates a new instance.
 *
 * @see MenuClick
 * @see com.zaryx.okaso.bukkit.menu.adapter.MenuResolver
 */
public final class MenuItem {

    private final ItemStack itemStack;
    private final Consumer<MenuClick> action;
    private final boolean movable;

    /**
     * Construct a menu item.
     *
     * @param itemStack the item to display (will be cloned internally)
     * @param action    the click handler (may be null)
     * @param movable   whether the item can be taken by the player
     */
    public MenuItem(ItemStack itemStack, Consumer<MenuClick> action, boolean movable) {
        this.itemStack = itemStack != null ? itemStack.clone() : null;
        this.action = action;
        this.movable = movable;
    }

    /**
     * Execute the click action for this item, if present.
     *
     * @param click the click context (null-safe)
     */
    public void handle(MenuClick click) {
        if (this.action == null || click == null) return;
        this.action.accept(click);
    }

    /**
     * Get a defensive copy of the displayed item stack.
     *
     * @return a cloned ItemStack, or null if this item has no stack
     */
    public ItemStack getItemStack() {
        return this.itemStack != null ? this.itemStack.clone() : null;
    }

    /**
     * Get the click action consumer.
     *
     * @return the action, or null if no action is set
     */
    public Consumer<MenuClick> getAction() {
        return this.action;
    }

    /**
     * Check whether this item can be moved by the player.
     *
     * @return true if the item is movable
     */
    public boolean isMovable() {
        return this.movable;
    }

    /**
     * Check whether this item has a click action registered.
     *
     * @return true if a non-null action is present
     */
    public boolean hasAction() {
        return this.action != null;
    }

    // ---- Factory methods ----

    /**
     * Create a non-movable display item with no action.
     *
     * @param itemStack the item to display (null-safe, resolved via {@link MenuResolver})
     * @return a new display-only menu item
     */
    public static MenuItem empty(ItemStack itemStack) {
        return new MenuItem(MenuResolver.resolve(itemStack), null, false);
    }

    /**
     * Create an item with a click action and specified movability.
     *
     * @param itemStack the item to display (null-safe, resolved via {@link MenuResolver})
     * @param action    the click handler (may be null)
     * @param movable   whether the player can take the item
     * @return a new menu item
     */
    public static MenuItem of(ItemStack itemStack, Consumer<MenuClick> action, boolean movable) {
        return new MenuItem(MenuResolver.resolve(itemStack), action, movable);
    }

    /**
     * Create a non-movable item with a click action.
     *
     * @param itemStack the item to display (null-safe, resolved via {@link MenuResolver})
     * @param action    the click handler (may be null)
     * @return a new clickable, non-movable menu item
     */
    public static MenuItem clickable(ItemStack itemStack, Consumer<MenuClick> action) {
        return new MenuItem(MenuResolver.resolve(itemStack), action, false);
    }

    /**
     * Create a movable item with a click action (e.g. for player inventory items).
     *
     * @param itemStack the item to display (null-safe, resolved via {@link MenuResolver})
     * @param action    the click handler (may be null)
     * @return a new movable menu item
     */
    public static MenuItem movable(ItemStack itemStack, Consumer<MenuClick> action) {
        return new MenuItem(MenuResolver.resolve(itemStack), action, true);
    }

    // ---- equals / hashCode ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MenuItem)) return false;
        MenuItem that = (MenuItem) o;
        return this.movable == that.movable
            && Objects.equals(this.itemStack, that.itemStack)
            && Objects.equals(this.action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemStack, action, movable);
    }

    @Override
    public String toString() {
        return "MenuItem{movable=" + movable + ", hasAction=" + (action != null) + "}";
    }
}

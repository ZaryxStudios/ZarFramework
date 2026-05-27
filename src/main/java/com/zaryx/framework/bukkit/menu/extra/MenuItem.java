package com.zaryx.framework.bukkit.menu.extra;

import com.zaryx.framework.bukkit.menu.adapter.MenuResolver;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents an item in a menu with an optional click action.
 * Immutable by design: each factory method creates a new instance.
 */
public final class MenuItem {

    private final ItemStack itemStack;
    private final Consumer<MenuClick> action;
    private final boolean movable;

    public MenuItem(ItemStack itemStack, Consumer<MenuClick> action, boolean movable) {
        this.itemStack = itemStack != null ? itemStack.clone() : null;
        this.action = action;
        this.movable = movable;
    }

    public void handle(MenuClick click) {
        if (this.action == null || click == null) return;
        this.action.accept(click);
    }

    public ItemStack getItemStack() {
        return this.itemStack != null ? this.itemStack.clone() : null;
    }

    public Consumer<MenuClick> getAction() {
        return this.action;
    }

    public boolean isMovable() {
        return this.movable;
    }

    public boolean hasAction() {
        return this.action != null;
    }

    // ---- Factory methods ----

    /**
     * Create a non-movable display item with no action.
     */
    public static MenuItem empty(ItemStack itemStack) {
        return new MenuItem(MenuResolver.resolve(itemStack), null, false);
    }

    /**
     * Create an item with a click action and specified movability.
     */
    public static MenuItem of(ItemStack itemStack, Consumer<MenuClick> action, boolean movable) {
        return new MenuItem(MenuResolver.resolve(itemStack), action, movable);
    }

    /**
     * Create a non-movable item with a click action.
     */
    public static MenuItem clickable(ItemStack itemStack, Consumer<MenuClick> action) {
        return new MenuItem(MenuResolver.resolve(itemStack), action, false);
    }

    /**
     * Create a movable item with a click action (e.g. for player inventory items).
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

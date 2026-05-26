package com.zaryx.framework.bukkit.menu.extra;

import com.zaryx.framework.bukkit.menu.adapter.MenuResolver;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class MenuItem {

    private final ItemStack itemStack;
    private final Consumer<MenuClick> action;
    private final boolean movable;

    public MenuItem(ItemStack itemStack, Consumer<MenuClick> action, boolean movable) {
        this.itemStack = itemStack != null ? itemStack.clone() : null;
        this.action = action;
        this.movable = movable;
    }

    public void handle(MenuClick click) {
        if (this.action != null) action.accept(click);
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

    public static MenuItem empty(ItemStack itemStack) {
        return new MenuItem(MenuResolver.resolve(itemStack), null, false);
    }

    public static MenuItem of(ItemStack itemStack, Consumer<MenuClick> action, boolean movable) {
        return new MenuItem(MenuResolver.resolve(itemStack), action, movable);
    }

    public static MenuItem movable(ItemStack itemStack, Consumer<MenuClick> action) {
        return new MenuItem(MenuResolver.resolve(itemStack), action, true);
    }
}
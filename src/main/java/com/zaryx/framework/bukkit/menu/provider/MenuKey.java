package com.zaryx.framework.bukkit.menu.provider;

import java.util.Arrays;

/**
 * Immutable key for menu provider cache lookups.
 * Wraps an array of parameters with proper equals/hashCode semantics.
 */
public final class MenuKey {
    private final Object[] params;
    private final int hash;

    public MenuKey(Object... params) {
        this.params = params != null ? Arrays.copyOf(params, params.length) : new Object[0];
        this.hash = Arrays.deepHashCode(this.params);
    }

    public Object get(int index) {
        if (index < 0 || index >= params.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + params.length);
        }
        return params[index];
    }

    public int size() {
        return params.length;
    }

    public boolean isEmpty() {
        return params.length == 0;
    }

    public Object[] toArray() {
        return Arrays.copyOf(params, params.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MenuKey)) return false;
        MenuKey key = (MenuKey) o;
        return Arrays.deepEquals(this.params, key.params);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "MenuKey" + Arrays.toString(params);
    }
}

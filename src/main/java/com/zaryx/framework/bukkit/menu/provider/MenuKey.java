package com.zaryx.framework.bukkit.menu.provider;

import java.util.Arrays;

public final class MenuKey {
    private final Object[] params;
    private final int hash;

    public MenuKey(Object... params) {
        this.params = params != null ? Arrays.copyOf(params, params.length) : new Object[0];
        this.hash = Arrays.hashCode(this.params);
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

    public Object[] toArray() {
        return Arrays.copyOf(params, params.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MenuKey)) return false;
        MenuKey key = (MenuKey) o;
        return Arrays.equals(this.params, key.params);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}

package com.zaryx.framework.bukkit.menu.provider;

import java.util.Arrays;

public class MenuKey {
    private final Object[] params;

    public MenuKey(Object... params) {
        this.params = params;
    }

    public Object get(int index) {
        return params[index];
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
        return Arrays.hashCode(params);
    }
}
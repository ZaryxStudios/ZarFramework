package com.zaryx.okaso.core;

public interface Module {

    String getName();

    void initialize();

    void disable();

    boolean isEnabled();

    default boolean isCritical() {
        return false;
    }
}

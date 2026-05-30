package com.zaryx.okaso.core;

import java.util.logging.Logger;

public interface ManagedComponent extends Module {

    Logger getLogger();

    @Override
    String getName();

    boolean isInitialized();

    default String getStats() {
        return "Component: " + getName() + " | Enabled: " + isEnabled();
    }

    default boolean validate() {
        return isEnabled() && isInitialized();
    }

    default void cleanup() {

    }
}

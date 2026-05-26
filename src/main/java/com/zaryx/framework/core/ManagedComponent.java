package com.zaryx.framework.core;

import java.util.logging.Logger;

/**
 * Base interface for all framework managers.
 * Defines the contract every manager must satisfy.
 */
public interface ManagedComponent extends Module {

    /**
     * Returns the component logger
     * @return component logger
     */
    Logger getLogger();

    /**
     * Returns the component name
     * @return unique identifier name
     */
    @Override
    String getName();

    /**
     * Checks whether the component is initialized
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Returns component statistics
     * @return statistics string
     */
    default String getStats() {
        return "Component: " + getName() + " | Enabled: " + isEnabled();
    }

    /**
     * Performs internal component validation
     * @return true if all validations pass
     */
    default boolean validate() {
        return isEnabled() && isInitialized();
    }

    /**
     * Cleans up component resources (called before disable)
     */
    default void cleanup() {
        // Override if needed
    }
}

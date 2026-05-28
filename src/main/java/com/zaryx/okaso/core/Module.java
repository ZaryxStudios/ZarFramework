package com.zaryx.okaso.core;

/**
 * Base interface for all okaso modules.
 * Each module must implement this contract to be managed by the system.
 */
public interface Module {

    /**
     * Unique module name
     * @return module name
     */
    String getName();

    /**
     * Initializes the module. Called when the plugin loads.
     */
    void initialize();

    /**
     * Disables the module. Called when the plugin unloads.
     */
    void disable();

    /**
     * Checks whether the module is enabled
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Defines whether the module is critical (loading should stop if it fails)
     * @return true if critical
     */
    default boolean isCritical() {
        return false;
    }
}

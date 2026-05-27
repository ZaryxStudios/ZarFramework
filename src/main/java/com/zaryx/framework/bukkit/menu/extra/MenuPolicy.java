package com.zaryx.framework.bukkit.menu.extra;

/**
 * Defines the back-navigation behaviour when a menu is closed.
 *
 * <ul>
 *   <li>{@link #NONE} – no automatic back-navigation; the menu simply closes.</li>
 *   <li>{@link #AUTO} – automatically navigates to the previous menu on close.</li>
 *   <li>{@link #MANUAL} – back-navigation must be triggered explicitly by the developer.</li>
 * </ul>
 */
public enum MenuPolicy {
    /** No automatic back-navigation. */
    NONE,

    /** Automatically navigate back when the menu is closed. */
    AUTO,

    /** Back-navigation requires explicit {@code menu.back(player)} calls. */
    MANUAL
}

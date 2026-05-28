package com.zaryx.okaso.bukkit.menu.extra;

/**
 * Defines the visual transition style when navigating between menus.
 *
 * <ul>
 *   <li>{@link #NONE} – instant switch, no animation.</li>
 *   <li>{@link #FADE} – fade-out / fade-in effect.</li>
 *   <li>{@link #DELAYED} – brief delay before opening the next menu.</li>
 *   <li>{@link #CLOSE_AND_OPEN} – fully close the current menu before opening the next.</li>
 * </ul>
 */
public enum MenuTransition {
    /** Instant transition with no visual effect. */
    NONE,

    /** Fade animation between menus. */
    FADE,

    /** Delayed transition with a short pause. */
    DELAYED,

    /** Close the current menu completely before opening the next. */
    CLOSE_AND_OPEN
}

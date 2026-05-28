package com.zaryx.okaso.bukkit.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link com.zaryx.okaso.bukkit.command.core.BaseCommand} subclass with metadata
 * that is automatically applied during construction.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Info {
    /** The primary command name (required). */
    String name();
    /** Permission node required to execute this command. Empty string means no permission. */
    String permission() default "";
    /** Whether this command can only be used by players (not console). */
    boolean playerOnly() default false;
    /** Whether this command can only be used from the console (not players). */
    boolean consoleOnly() default false;
}

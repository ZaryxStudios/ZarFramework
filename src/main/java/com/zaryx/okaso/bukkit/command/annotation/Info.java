package com.zaryx.okaso.bukkit.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Info {

    String name();

    String permission() default "";

    boolean playerOnly() default false;

    boolean consoleOnly() default false;
}

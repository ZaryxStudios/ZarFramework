package com.zaryx.okaso.bungee.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BungeeConfigFile {
    String value();

    /**
     * When false (default), the config stays in-memory and is never written to the plugin data folder.
     */
    boolean exposeToDisk() default false;
}

package com.zaryx.framework.bukkit.placeholder;

import java.util.Map;

public final class PlaceholderResolver {

    private PlaceholderResolver() {}

    public static String apply(String message, PlaceholderContext context) {
        if (context == null || context.getValues().isEmpty()) {
            return message;
        }

        String result = message;
        for (Map.Entry<String, String> entry : context.getValues().entrySet()) {
            result = result.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return result;
    }

}

package com.zaryx.framework.bukkit.utility;

import com.zaryx.framework.bukkit.placeholder.PlaceholderContext;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data conversion utilities (ItemStack serialization, etc).
 * For text coloring, use Text utility.
 */
public final class Convert {

    private Convert() {}

    /**
     * @deprecated Use Text.text() instead for text coloring and placeholders
     */
    @Deprecated
    public static String text(String text) {
        return Text.text(text);
    }

    /**
     * @deprecated Use Text.text() instead for text coloring and placeholders
     */
    @Deprecated
    public static List<String> text(List<String> t) {
        return Text.text(t);
    }

    /**
     * @deprecated Use Text.text(String, PlaceholderContext) instead
     */
    @Deprecated
    public static String text(String t, PlaceholderContext context) {
        return Text.text(t, context);
    }

    /**
     * @deprecated Use Text.text(List, PlaceholderContext) instead
     */
    @Deprecated
    public static List<String> text(List<String> t, PlaceholderContext context) {
        return Text.text(t, context);
    }

    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        if(items.length == 0) {
            return "";
        }
        // Try native Bukkit object stream first (may not exist on very old/new platforms)
        try {
            Class<?> bosClass = Class.forName("org.bukkit.util.io.BukkitObjectOutputStream");
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                OutputStream os = outputStream;
                Constructor<?> ctor = bosClass.getConstructor(java.io.OutputStream.class);
                Object bos = ctor.newInstance(os);
                java.lang.reflect.Method writeInt = bosClass.getMethod("writeInt", int.class);
                writeInt.invoke(bos, items.length);

                java.lang.reflect.Method writeObject = bosClass.getMethod("writeObject", Object.class);
                for (ItemStack item : items) {
                    writeObject.invoke(bos, item);
                }

                // close if possible
                try { bosClass.getMethod("close").invoke(bos); } catch (Throwable ignored) {}

                return Base64Coder.encodeLines(outputStream.toByteArray());
            }
        } catch (ClassNotFoundException cnf) {
            // Fallback: use ConfigurationSerializable-based JSON
            try {
                com.google.gson.Gson gson = new Gson();
                List<Map<String, Object>> serialized = new ArrayList<>();
                for (ItemStack item : items) {
                    if (item == null) {
                        serialized.add(null);
                    } else {
                        serialized.add(item.serialize());
                    }
                }
                String json = gson.toJson(serialized);
                return Base64Coder.encodeLines(json.getBytes(StandardCharsets.UTF_8));
            } catch (Throwable t) {
                throw new IllegalStateException("Unable to save items (fallback).", t);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to save items.", t);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) {
        if(data == null || data.isEmpty()) {
            return new ItemStack[0];
        }
        byte[] raw = Base64Coder.decodeLines(data);

        // Try native Bukkit object stream first
        try {
            Class<?> bisClass = Class.forName("org.bukkit.util.io.BukkitObjectInputStream");
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(raw)) {
                Constructor<?> ctor = bisClass.getConstructor(InputStream.class);
                Object bis = ctor.newInstance(inputStream);
                Method readInt = bisClass.getMethod("readInt");
                int len = (Integer) readInt.invoke(bis);

                ItemStack[] items = new ItemStack[len];
                Method readObject = bisClass.getMethod("readObject");
                for (int i = 0; i < len; i++) {
                    Object obj = readObject.invoke(bis);
                    items[i] = (ItemStack) obj;
                }

                try { bisClass.getMethod("close").invoke(bis); } catch (Throwable ignored) {}
                return items;
            }
        } catch (ClassNotFoundException cnf) {
            // Fallback: JSON via ConfigurationSerializable
            try {
                String json = new String(raw, StandardCharsets.UTF_8);
                Gson gson = new Gson();
                List<Map<String, Object>> list = gson.fromJson(json, new TypeToken<List<Map<String, Object>>>(){}.getType());
                ItemStack[] items = new ItemStack[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Map<String, Object> map = list.get(i);
                    if (map == null) {
                        items[i] = null;
                    } else {
                        items[i] = ItemStack.deserialize(map);
                    }
                }
                return items;
            } catch (Throwable t) {
                throw new IllegalStateException("Unable to load items (fallback).", t);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to load items.", t);
        }
    }
}

package com.zaryx.framework.bukkit.utility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Reflection {

    private static final String NMS_VERSION;
    private static final boolean MODERN;

    // Cache to improve reflection performance
    private static final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Field> fieldCache = new ConcurrentHashMap<>();
    private static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    static {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        NMS_VERSION = name.substring(name.lastIndexOf('.') + 1);
        MODERN = exists("net.minecraft.network.protocol.Packet");
    }

    private Reflection() {}

    /* ================= CLASS CACHE ================= */

    public static boolean exists(String clazz) {
        // Check cache first
        if (classCache.containsKey(clazz)) {
            return true;
        }

        try {
            Class.forName(clazz);
            // Store in cache
            classCache.put(clazz, Class.forName(clazz));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Class<?> getNMS(String... candidates) {
        for (String name : candidates) {
            // Check cache
            if (classCache.containsKey(name)) {
                return classCache.get(name);
            }

            try {
                Class<?> clazz = Class.forName(name);
                // Store in cache
                classCache.put(name, clazz);
                return clazz;
            } catch (ClassNotFoundException ignored) {}

            try {
                Class<?> clazz = Class.forName("net.minecraft.server." + NMS_VERSION + "." + name);
                classCache.put(name, clazz);
                return clazz;
            } catch (ClassNotFoundException ignored) {}

            try {
                Class<?> clazz = Class.forName("net.minecraft.server." + NMS_VERSION + "." + name.replace('$', '.'));
                classCache.put(name, clazz);
                return clazz;
            } catch (ClassNotFoundException ignored) {}
        }

        throw new RuntimeException("NMS class not found: " + Arrays.toString(candidates));
    }

    public static String getNmsVersion() {
        return NMS_VERSION;
    }

    public static boolean isModernNms() {
        return MODERN;
    }

    /* ================= ENUM ================= */

    public static Enum<?> getEnum(String enumClass, String value) {
        Class<?> clazz = getNMS(enumClass);
        for (Object e : clazz.getEnumConstants()) {
            if (((Enum<?>) e).name().equals(value)) {
                return (Enum<?>) e;
            }
        }
        throw new IllegalStateException("Enum value not found: " + enumClass + "." + value);
    }

    /**
     * Get a CraftBukkit (OBC) class by simple path under org.bukkit.craftbukkit.VERSION
     */
    public static Class<?> getOBC(String path) {
        String key = "obc:" + path;
        if (classCache.containsKey(key)) return classCache.get(key);

        String candidate = "org.bukkit.craftbukkit." + NMS_VERSION + "." + path;
        try {
            Class<?> c = Class.forName(candidate);
            classCache.put(key, c);
            return c;
        } catch (ClassNotFoundException ignored) {}

        // Try without version (for some embedded builds)
        try {
            Class<?> c = Class.forName("org.bukkit.craftbukkit." + path);
            classCache.put(key, c);
            return c;
        } catch (ClassNotFoundException ignored) {}

        throw new RuntimeException("OBC class not found: " + path);
    }

    /* ================= INSTANCE ================= */

    public static Object newInstance(Class<?> clazz, Object... args) {
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.getParameterTypes().length == args.length) {
                try {
                    c.setAccessible(true);
                    return c.newInstance(args);
                } catch (Exception ignored) {}
            }
        }
        throw new IllegalStateException("Constructor not found: " + clazz.getName());
    }

    /* ================= FIELD ================= */

    public static void set(Object obj, String field, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Field set failed: " + field, e);
        }
    }

    /* ================= METHOD ================= */

    public static Object invokeStatic(Class<?> clazz, String method, Object arg) {
        try {
            Method m = clazz.getDeclaredMethod(method, String.class);
            m.setAccessible(true);
            return m.invoke(null, arg);
        } catch (Exception e) {
            throw new RuntimeException("Static invoke failed: " + method, e);
        }
    }

    /* ================= PACKET ================= */

    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle = invoke(player, "getHandle");

            Object connection;

            // ===== MODERN (1.17+) =====
            if (exists("net.minecraft.server.network.ServerGamePacketListenerImpl")) {
                connection = get(handle, "connection");

                Method send = connection.getClass().getMethod(
                        "send", getNMS("net.minecraft.network.protocol.Packet")
                );

                send.invoke(connection, packet);
                return;
            }

            // ===== LEGACY (1.8-1.16) =====
            connection = get(handle, "playerConnection");

            Method send = connection.getClass().getMethod(
                    "sendPacket", getNMS("Packet")
            );

            send.invoke(connection, packet);

        } catch (Exception e) {
            throw new RuntimeException("Packet send failed", e);
        }
    }

    public static Object invoke(Object instance, String method, Object... args) {
        try {
            Class<?> clazz = instance.getClass();

            for (Method m : clazz.getMethods()) {
                if (!m.getName().equals(method)) continue;
                if (m.getParameterCount() != args.length) continue;

                m.setAccessible(true);
                return m.invoke(instance, args);
            }

            throw new NoSuchMethodException(method);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Reflection invoke failed: " + instance.getClass().getName() + "." + method, e
            );
        }
    }

    public static Object get(Object instance, String field) {
        try {
            Class<?> clazz = instance.getClass();

            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField(field);
                    f.setAccessible(true);
                    return f.get(instance);
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }

            throw new NoSuchFieldException(field);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Reflection get failed: " + instance.getClass().getName() + "." + field, e
            );
        }
    }

    public static Class<?> getInner(Class<?> parent, String simpleName) {
        for (Class<?> inner : parent.getDeclaredClasses()) {
            if (inner.getSimpleName().equals(simpleName)) {
                return inner;
            }
        }
        throw new RuntimeException(
                "Inner class '" + simpleName + "' not found in " + parent.getName()
        );
    }
}

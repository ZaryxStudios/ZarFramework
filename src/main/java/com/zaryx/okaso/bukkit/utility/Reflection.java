package com.zaryx.okaso.bukkit.utility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Reflection {

    private static final Logger LOGGER = Logger.getLogger(Reflection.class.getName());

    private static final String NMS_VERSION;
    private static final boolean MODERN;
    private static final int CACHE_MAX_SIZE = 500;

    private static final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Field> fieldCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
    private static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private static final Map<String, Long> cacheStats = new ConcurrentHashMap<>();

    static {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        NMS_VERSION = name.substring(name.lastIndexOf('.') + 1);
        // Detect the modern packet layout by probing a stable NMS class.
        MODERN = exists("net.minecraft.network.protocol.Packet");
    }

    private Reflection() {}

    public static boolean exists(String clazz) {
        if (classCache.containsKey(clazz)) {
            recordCacheHit("exists");
            return true;
        }

        try {
            Class<?> resolved = Class.forName(clazz);
            putClassCache(clazz, resolved);
            recordCacheHit("exists");
            return true;
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Failed to resolve class: " + clazz, e);
            recordCacheMiss("exists");
            return false;
        }
    }

    public static Class<?> getNMS(String... candidates) {
        for (String name : candidates) {

            if (classCache.containsKey(name)) {
                recordCacheHit("getNMS");
                return classCache.get(name);
            }

            Class<?> clazz = tryLoadClass(name);
            if (clazz != null) {
                putClassCache(name, clazz);
                recordCacheMiss("getNMS");
                return clazz;
            }

            clazz = tryLoadClass("net.minecraft.server." + NMS_VERSION + "." + name);
            if (clazz != null) {
                putClassCache(name, clazz);
                recordCacheMiss("getNMS");
                return clazz;
            }

            clazz = tryLoadClass("net.minecraft.server." + NMS_VERSION + "." + name.replace('$', '.'));
            if (clazz != null) {
                putClassCache(name, clazz);
                recordCacheMiss("getNMS");
                return clazz;
            }

            clazz = tryLoadClass("net.minecraft." + name);
            if (clazz != null) {
                putClassCache(name, clazz);
                recordCacheMiss("getNMS");
                return clazz;
            }
        }

        throw new RuntimeException("NMS class not found: " + Arrays.toString(candidates));
    }

    public static Class<?> getOBC(String path) {
        String key = "obc:" + path;
        if (classCache.containsKey(key)) {
            recordCacheHit("getOBC");
            return classCache.get(key);
        }

        Class<?> clazz = tryLoadClass("org.bukkit.craftbukkit." + NMS_VERSION + "." + path);
        if (clazz != null) {
            putClassCache(key, clazz);
            recordCacheMiss("getOBC");
            return clazz;
        }

        clazz = tryLoadClass("org.bukkit.craftbukkit." + path);
        if (clazz != null) {
            putClassCache(key, clazz);
            recordCacheMiss("getOBC");
            return clazz;
        }

        throw new RuntimeException("OBC class not found: " + path);
    }

    public static Class<?> getInner(Class<?> parent, String simpleName) {
        for (Class<?> inner : parent.getDeclaredClasses()) {
            if (inner.getSimpleName().equals(simpleName)) {
                return inner;
            }
        }
        throw new RuntimeException("Inner class '" + simpleName + "' not found in " + parent.getName());
    }

    public static Class<?> getClass(String name) {
        if (classCache.containsKey(name)) {
            recordCacheHit("getClass");
            return classCache.get(name);
        }
        Class<?> clazz = tryLoadClass(name);
        if (clazz == null) {
            throw new RuntimeException("Class not found: " + name);
        }
        putClassCache(name, clazz);
        recordCacheMiss("getClass");
        return clazz;
    }

    public static String getNmsVersion() {
        return NMS_VERSION;
    }

    public static boolean isModernNms() {
        return MODERN;
    }

    public static boolean isVersionAtLeast(int major, int minor) {
        try {
            String[] parts = NMS_VERSION.replace("R", "").split("_");
            if (parts.length >= 2) {
                int serverMajor = Integer.parseInt(parts[0]);
                int serverMinor = Integer.parseInt(parts[1]);
                return serverMajor > major || (serverMajor == major && serverMinor >= minor);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to parse version: " + NMS_VERSION, e);
        }
        return false;
    }

    public static boolean isLegacy() {
        return !MODERN;
    }

    public static Enum<?> getEnum(String enumClass, String value) {
        Class<?> clazz = getNMS(enumClass);
        return getEnumValue(clazz, value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getEnumValue(Class<?> enumClass, String value) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("Class " + enumClass.getName() + " is not an enum");
        }
        for (Object e : enumClass.getEnumConstants()) {
            if (((Enum<?>) e).name().equals(value)) {
                return (T) e;
            }
        }
        throw new IllegalStateException("Enum value not found: " + enumClass.getName() + "." + value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getBukkitEnum(Class<T> enumClass, String value) {
        return Enum.valueOf(enumClass, value.toUpperCase().replace('-', '_').replace(' ', '_'));
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        String cacheKey = buildConstructorKey(clazz, args);
        if (constructorCache.containsKey(cacheKey)) {
            try {
                return constructorCache.get(cacheKey).newInstance(args);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Cached constructor failed for " + clazz.getName(), e);
            }
        }

        Class<?>[] paramTypes = toParamTypes(args);
        try {
            Constructor<?> c = clazz.getDeclaredConstructor(paramTypes);
            c.setAccessible(true);
            constructorCache.put(cacheKey, c);
            return c.newInstance(args);
        } catch (NoSuchMethodException e) {
            LOGGER.log(Level.FINE, "Exact constructor not found for " + clazz.getName(), e);
        }

        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (isCompatible(c.getParameterTypes(), paramTypes)) {
                try {
                    c.setAccessible(true);
                    constructorCache.put(cacheKey, c);
                    return c.newInstance(args);
                } catch (Exception ex) {
                    LOGGER.log(Level.FINE, "Compatible constructor failed for " + clazz.getName(), ex);
                }
            }
        }

        throw new IllegalStateException("Constructor not found: " + clazz.getName() + " with args " + Arrays.toString(args));
    }

    public static Object newInstanceEmpty(Class<?> clazz) {
        return newInstance(clazz);
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        String key = buildFieldKey(clazz, fieldName);
        if (fieldCache.containsKey(key)) {
            recordCacheHit("getField");
            return fieldCache.get(key);
        }

        Class<?> current = clazz;
        while (current != null) {
            try {
                Field f = current.getDeclaredField(fieldName);
                f.setAccessible(true);
                fieldCache.put(key, f);
                recordCacheMiss("getField");
                return f;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }

        throw new RuntimeException("Field '" + fieldName + "' not found in " + clazz.getName() + " or superclasses");
    }

    public static Object get(Object instance, String field) {
        try {
            Field f = getField(instance.getClass(), field);
            return f.get(instance);
        } catch (Exception e) {
            throw new RuntimeException("Reflection get failed: " + instance.getClass().getName() + "." + field, e);
        }
    }

    public static Object getStatic(Class<?> clazz, String field) {
        try {
            Field f = getField(clazz, field);
            return f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Reflection static get failed: " + clazz.getName() + "." + field, e);
        }
    }

    public static void set(Object obj, String field, Object value) {
        try {
            Field f = getField(obj.getClass(), field);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Field set failed: " + field + " on " + obj.getClass().getName(), e);
        }
    }

    public static void setStatic(Class<?> clazz, String field, Object value) {
        try {
            Field f = getField(clazz, field);
            f.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException("Static field set failed: " + clazz.getName() + "." + field, e);
        }
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        String key = buildMethodKey(clazz, methodName, paramTypes);
        if (methodCache.containsKey(key)) {
            recordCacheHit("getMethod");
            return methodCache.get(key);
        }

        Class<?> current = clazz;
        while (current != null) {
            try {
                Method m = current.getDeclaredMethod(methodName, paramTypes);
                m.setAccessible(true);
                methodCache.put(key, m);
                recordCacheMiss("getMethod");
                return m;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }

        throw new RuntimeException("Method '" + methodName + "' not found in " + clazz.getName());
    }

    public static Method getPublicMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        String key = buildMethodKey(clazz, methodName, paramTypes) + ":public";
        if (methodCache.containsKey(key)) {
            recordCacheHit("getPublicMethod");
            return methodCache.get(key);
        }

        try {
            Method m = clazz.getMethod(methodName, paramTypes);
            methodCache.put(key, m);
            recordCacheMiss("getPublicMethod");
            return m;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Public method '" + methodName + "' not found in " + clazz.getName(), e);
        }
    }

    public static Object invoke(Object instance, String method, Object... args) {
        try {
            Class<?>[] paramTypes = toParamTypes(args);
            Method m = getMethod(instance.getClass(), method, paramTypes);
            return m.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Reflection invoke failed: " + instance.getClass().getName() + "." + method, e
            );
        }
    }

    public static Object invokeStatic(Class<?> clazz, String method, Object... args) {
        try {
            Class<?>[] paramTypes = toParamTypes(args);
            Method m = getMethod(clazz, method, paramTypes);
            return m.invoke(null, args);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Static invoke failed: " + clazz.getName() + "." + method, e
            );
        }
    }

    public static Object invokeExact(Object instance, String method, Class<?>[] paramTypes, Object[] args) {
        try {
            Method m = getMethod(instance.getClass(), method, paramTypes);
            return m.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Exact invoke failed: " + instance.getClass().getName() + "." + method, e
            );
        }
    }

    public static void sendPacket(Player player, Object packet) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packet, "packet");

        try {
            Object handle = invoke(player, "getHandle");
            Object connection;

            if (MODERN) {

                connection = get(handle, "connection");
                Method send = getPublicMethod(
                        connection.getClass(),
                        "send",
                        getNMS("net.minecraft.network.protocol.Packet")
                );
                send.invoke(connection, packet);
            } else {

                connection = get(handle, "playerConnection");
                Method send = getPublicMethod(
                        connection.getClass(),
                        "sendPacket",
                        getNMS("Packet")
                );
                send.invoke(connection, packet);
            }
        } catch (Exception e) {
            throw new RuntimeException("Packet send failed for player " + player.getName(), e);
        }
    }

    public static void sendPacket(Collection<? extends Player> players, Object packet) {
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }

    public static void sendPackets(Player player, Object... packets) {
        for (Object packet : packets) {
            sendPacket(player, packet);
        }
    }

    public static void sendPackets(Collection<? extends Player> players, Object... packets) {
        for (Player player : players) {
            for (Object packet : packets) {
                sendPacket(player, packet);
            }
        }
    }

    public static Object createChatComponent(String json) {
        try {
            Class<?> chatComponent = getNMS(
                    "net.minecraft.network.chat.IChatBaseComponent",
                    "IChatBaseComponent"
            );
            return invokeStatic(chatComponent, "a", json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create chat component", e);
        }
    }

    public static Object createChatComponentText(String text) {
        try {
            Class<?> chatComponent = getNMS(
                    "net.minecraft.network.chat.IChatBaseComponent",
                    "IChatBaseComponent"
            );
            Method m = getMethod(chatComponent, "b", String.class);
            return m.invoke(null, text);
        } catch (Exception e) {

            return createChatComponent("{\"text\":\"" + escapeJson(text) + "\"}");
        }
    }

    public static Object toNmsItem(org.bukkit.inventory.ItemStack item) {
        try {
            Class<?> craftItemStack = getOBC("inventory.CraftItemStack");
            return invokeStatic(craftItemStack, "asNMSCopy", item);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Bukkit item to NMS", e);
        }
    }

    public static org.bukkit.inventory.ItemStack fromNmsItem(Object nmsItem) {
        try {
            Class<?> craftItemStack = getOBC("inventory.CraftItemStack");
            return (org.bukkit.inventory.ItemStack) invokeStatic(craftItemStack, "asBukkitCopy", nmsItem);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert NMS item to Bukkit", e);
        }
    }

    public static Object getNmsWorld(org.bukkit.World world) {
        try {
            return invoke(world, "getHandle");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NMS world handle", e);
        }
    }

    public static Object getNmsEntity(org.bukkit.entity.Entity entity) {
        try {
            return invoke(entity, "getHandle");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NMS entity handle", e);
        }
    }

    public static Object getNmsChunk(org.bukkit.Chunk chunk) {
        try {
            Class<?> craftChunk = getOBC("CraftChunk");
            Field handle = getField(craftChunk, "handle");
            return handle.get(chunk);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NMS chunk", e);
        }
    }

    public static void clearCache() {
        cacheLock.writeLock().lock();
        try {
            classCache.clear();
            methodCache.clear();
            fieldCache.clear();
            constructorCache.clear();
            cacheStats.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public static Map<String, Long> getCacheStats() {
        return Collections.unmodifiableMap(new HashMap<>(cacheStats));
    }

    public static int getClassCacheSize() {
        return classCache.size();
    }

    public static int getMethodCacheSize() {
        return methodCache.size();
    }

    public static int getFieldCacheSize() {
        return fieldCache.size();
    }

    private static Class<?> tryLoadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static void putClassCache(String key, Class<?> clazz) {
        if (classCache.size() >= CACHE_MAX_SIZE) {

            String firstKey = classCache.keys().nextElement();
            classCache.remove(firstKey);
        }
        classCache.put(key, clazz);
    }

    private static Class<?>[] toParamTypes(Object... args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        return types;
    }

    private static boolean isCompatible(Class<?>[] declared, Class<?>[] given) {
        if (declared.length != given.length) return false;
        for (int i = 0; i < declared.length; i++) {
            if (!declared[i].isAssignableFrom(given[i]) && given[i] != Object.class) {
                return false;
            }
        }
        return true;
    }

    private static String buildFieldKey(Class<?> clazz, String fieldName) {
        return clazz.getName() + "#" + fieldName;
    }

    private static String buildMethodKey(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        StringBuilder key = new StringBuilder(clazz.getName());
        key.append(".").append(methodName).append("(");
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) key.append(",");
            key.append(paramTypes[i].getName());
        }
        key.append(")");
        return key.toString();
    }

    private static String buildConstructorKey(Class<?> clazz, Object... args) {
        StringBuilder key = new StringBuilder(clazz.getName());
        key.append(".<init>(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) key.append(",");
            key.append(args[i] != null ? args[i].getClass().getName() : "null");
        }
        key.append(")");
        return key.toString();
    }

    private static void recordCacheHit(String operation) {
        cacheStats.merge(operation + ".hits", 1L, Long::sum);
    }

    private static void recordCacheMiss(String operation) {
        cacheStats.merge(operation + ".misses", 1L, Long::sum);
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}

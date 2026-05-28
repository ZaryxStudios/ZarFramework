package com.zaryx.framework.bukkit.nms.materials;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Skull utilities with legacy-safe owner handling.
 */
public final class SkullHandler {

    private static final Logger LOGGER = Logger.getLogger(SkullHandler.class.getName());

    private SkullHandler() {
    }

    public static ItemStack createPlayerHead(String owner) {
        ItemStack skull = new ItemStack(resolveHeadMaterial(), 1, (short) 3);
        return setOwner(skull, owner);
    }

    public static ItemStack setOwner(ItemStack skull, String playerName) {
        if (skull == null || !(skull.getItemMeta() instanceof SkullMeta)) {
            return skull;
        }

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        try {
            meta.setOwner(playerName);
            skull.setItemMeta(meta);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Failed to set skull owner through the legacy API.", e);
        }
        return skull;
    }

    public static ItemStack setOwnerByUuid(ItemStack skull, UUID uuid) {
        // Legacy APIs only support owner name directly; keep no-op safe fallback.
        return skull;
    }

    public static ItemStack setTexture(ItemStack skull, String base64Texture) {
        if (skull == null || !(skull.getItemMeta() instanceof SkullMeta)) {
            return skull;
        }

        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object profile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), "textures");

            Object propertyMap = gameProfileClass.getMethod("getProperties").invoke(profile);
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", base64Texture);
            propertyMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", property);

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
            skull.setItemMeta(meta);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Failed to apply a custom skull texture.", e);
        }

        return skull;
    }

    private static Material resolveHeadMaterial() {
        Material modern = Material.getMaterial("PLAYER_HEAD");
        if (modern != null) {
            return modern;
        }
        Material legacy = Material.getMaterial("SKULL_ITEM");
        return legacy == null ? Material.STONE : legacy;
    }
}

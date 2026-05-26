package com.zaryx.framework.bukkit.adapter.tablist.extra;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.zaryx.framework.bukkit.nms.NmsService;
import com.zaryx.framework.bukkit.utility.Reflection;
import org.bukkit.entity.Player;

import java.util.*;

public final class TabPacket {

    private TabPacket() {}

    /* ====================================================== */
    /* ===================== FLAGS ========================== */
    /* ====================================================== */

    private static final boolean MODERN;
    private static final Map<String, Class<?>> NMS_CACHE = new HashMap<>();
    private static final NmsService NMS = new NmsService();

    static {
        MODERN = Reflection.exists(
                "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket"
        );
    }

    /* ====================================================== */
    /* ===================== PUBLIC API ===================== */
    /* ====================================================== */

    public static void add(Player viewer, int slot, TabEntry entry) {
        if (MODERN) {
            send(viewer, createModernAdd(slot, entry));
        } else {
            send(viewer, createLegacy("ADD_PLAYER", slot));
        }
    }

    /** UPDATE TEXT */
    public static void updateText(Player viewer, int slot, String text) {
        if (!MODERN) {
            send(viewer, createLegacy("UPDATE_DISPLAY_NAME", slot));
        }
        else {
            TabEntry entry = new TabEntry(text, 0);
            send(viewer, createModernAdd(slot, entry));
        }
    }

    /** UPDATE PING */
    public static void updatePing(Player viewer, int slot, int ping) {
        if (!MODERN) {
            send(viewer, createLegacy("UPDATE_LATENCY", slot));
        }
    }

    /** REMOVE SLOT */
    public static void remove(Player viewer, int slot) {
        if (MODERN) {
            send(viewer, createModernRemove(slot));
        } else {
            send(viewer, createLegacy("REMOVE_PLAYER", slot));
        }
    }

    /** HEADER / FOOTER */
    public static void headerFooter(Player player, List<String> header, List<String> footer) {
        try {
            String h = join(header);
            String f = join(footer);

            Object packet;

            if (MODERN) {
                Class<?> component = NMS.resolve("net.minecraft.network.chat.Component");

                Object headerComp = Reflection.invokeStatic(component, "literal", h);
                Object footerComp = Reflection.invokeStatic(component, "literal", f);

                packet = Reflection.newInstance(
                        NMS.resolve("net.minecraft.network.protocol.game.ClientboundTabListPacket"),
                        headerComp,
                        footerComp
                );
            } else {
                packet = Reflection.newInstance(
                        NMS.resolveLegacy("PacketPlayOutPlayerListHeaderFooter")
                );

                Reflection.set(packet, "a", chat(h));
                Reflection.set(packet, "b", chat(f));
            }

            send(player, packet);

        } catch (Exception e) {
            throw new RuntimeException("Tab Header/Footer failed", e);
        }
    }

    /* ====================================================== */
    /* =================== MODERN =========================== */
    /* ====================================================== */

    private static Object createModernAdd(int slot, TabEntry entry) {

        GameProfile profile = profileOf(slot, entry);

        Class<?> packetClass = nms("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
        if (packetClass == null) throw new RuntimeException("NMS packet class not found");

        Class<?> actionEnum = Reflection.getInner(packetClass, "Action");
        EnumSet<?> actions = EnumSet.of(
            Enum.valueOf((Class<Enum>) actionEnum, "ADD_PLAYER")
        );

        Object entryObj = createModernEntry(profile, entry);
        List<Object> list = new ArrayList<>();
        list.add(entryObj);

        return Reflection.newInstance(packetClass, actions, list);
    }

    private static Object createModernRemove(int slot) {

        List<UUID> ids = new ArrayList<>();
        ids.add(fakeUUID(slot));

        Class<?> removePacket = nms("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
        if (removePacket == null) throw new RuntimeException("NMS remove packet not found");

        return Reflection.newInstance(removePacket, ids);
    }

    private static Object createModernEntry(GameProfile profile, TabEntry entry) {
        Class<?> entryClass = nms("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
        if (entryClass == null) throw new RuntimeException("NMS entry class not found");

        return Reflection.newInstance(
            entryClass,
            profile.getId(),
            profile,
            false,
            entry.getPing(),
            Reflection.getEnum("net.minecraft.world.level.EnumGamemode", "SURVIVAL"),
            chat(entry.getText()),
            null
        );
    }

    /* ====================================================== */
    /* =================== LEGACY =========================== */
    /* ====================================================== */

    private static Object createLegacy(String action, int slot) {

        Class<?> packetClass = nms("PacketPlayOutPlayerInfo");
        if (packetClass == null) throw new RuntimeException("Legacy packet class not found");

        Class<?> actionEnum = Reflection.getInner(packetClass, "EnumPlayerInfoAction");
        Enum<?> enumAction = Enum.valueOf((Class<Enum>) actionEnum, action);

        List<Object> list = new ArrayList<>();
        list.add(createLegacyEntry(slot));
        return Reflection.newInstance(packetClass, enumAction, list);
    }

    private static Object createLegacyEntry(int slot) {

        GameProfile profile = new GameProfile(fakeUUID(slot), " ");

        return Reflection.newInstance(
                NMS.resolveLegacy("PacketPlayOutPlayerInfo$PlayerInfoData"),
                profile,
                0,
                Reflection.getEnum("EnumGamemode", "SURVIVAL"),
                chat(" ")
        );
    }

    /* ====================================================== */
    /* =================== HELPERS ========================== */
    /* ====================================================== */

    private static GameProfile profileOf(int slot, TabEntry entry) {

        GameProfile profile = new GameProfile(fakeUUID(slot), entry.getText());

        if (entry.getValue() != null && entry.getSignature() != null) {
            profile.getProperties().put(
                    "textures",
                    new Property("textures", entry.getValue(), entry.getSignature())
            );
        }

        return profile;
    }

    private static UUID fakeUUID(int slot) {
        return new UUID(0L, slot + 1L);
    }

    private static Object chat(String text) {
        try {

            if (Reflection.exists("net.minecraft.network.chat.Component")) {
                Class<?> component = NMS.resolve("net.minecraft.network.chat.Component");
                return Reflection.invokeStatic(component, "literal", text);
            }

            // ===== LEGACY (1.8–1.16) =====
            Class<?> ichat = NMS.resolveLegacy("IChatBaseComponent");
            return Reflection.invokeStatic(
                    ichat,
                    "a",
                    "{\"text\":\"" + text.replace("\"", "") + "\"}"
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to create chat component", e);
        }
    }

    private static void send(Player player, Object packet) {
        Reflection.sendPacket(player, packet);
    }

    private static String join(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        return String.join("\n", lines);
    }

    private static Class<?> nms(String name) {
        if (NMS_CACHE.containsKey(name)) return NMS_CACHE.get(name);
        try {
            Class<?> c = NMS.resolve(name);
            NMS_CACHE.put(name, c);
            return c;
        } catch (Throwable t) {
            NMS_CACHE.put(name, null);
            return null;
        }
    }
}
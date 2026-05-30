package com.zaryx.okaso.bukkit.nms;

import com.zaryx.okaso.bukkit.utility.Reflection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Objects;

public class NmsService {

    private final String version;
    private final boolean modern;
    private final int majorVersion;
    private final int minorVersion;

    public NmsService() {
        // Cache the server version and layout once during service creation.
        this.version = Reflection.getNmsVersion();
        this.modern = Reflection.isModernNms();
        this.majorVersion = parseMajor();
        this.minorVersion = parseMinor();
    }

    public String getVersion() {
        return version;
    }

    public boolean isModern() {
        return modern;
    }

    public boolean isLegacy() {
        return !modern;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public boolean isAtLeast(int major, int minor) {
        return majorVersion > major || (majorVersion == major && minorVersion >= minor);
    }

    public boolean isBetween(int fromMajor, int fromMinor, int toMajor, int toMinor) {
        return isAtLeast(fromMajor, fromMinor) && !isAtLeast(toMajor, toMinor);
    }

    public boolean isVersion(int major, int minor) {
        return majorVersion == major && minorVersion == minor;
    }

    public Class<?> resolve(String... candidates) {
        return Reflection.getNMS(candidates);
    }

    public Class<?> resolveLegacy(String simpleName) {
        return Reflection.getNMS(simpleName);
    }

    public Class<?> resolveOBC(String path) {
        return Reflection.getOBC(path);
    }

    public Class<?> resolveClass(String fullyQualifiedName) {
        return Reflection.getClass(fullyQualifiedName);
    }

    public boolean classExists(String fullyQualifiedName) {
        return Reflection.exists(fullyQualifiedName);
    }

    public void sendPacket(Player player, Object packet) {
        Reflection.sendPacket(player, packet);
    }

    public void sendPacket(Collection<? extends Player> players, Object packet) {
        Reflection.sendPacket(players, packet);
    }

    public void sendPackets(Player player, Object... packets) {
        Reflection.sendPackets(player, packets);
    }

    public void sendPackets(Collection<? extends Player> players, Object... packets) {
        Reflection.sendPackets(players, packets);
    }

    public Object toNmsItem(org.bukkit.inventory.ItemStack item) {
        return Reflection.toNmsItem(item);
    }

    public org.bukkit.inventory.ItemStack fromNmsItem(Object nmsItem) {
        return Reflection.fromNmsItem(nmsItem);
    }

    public Object createChatComponent(String json) {
        return Reflection.createChatComponent(json);
    }

    public Object createChatComponentText(String text) {
        return Reflection.createChatComponentText(text);
    }

    public Object getNmsWorld(org.bukkit.World world) {
        return Reflection.getNmsWorld(world);
    }

    public Object getNmsEntity(org.bukkit.entity.Entity entity) {
        return Reflection.getNmsEntity(entity);
    }

    public Object getNmsChunk(org.bukkit.Chunk chunk) {
        return Reflection.getNmsChunk(chunk);
    }

    private int parseMajor() {
        try {
            String[] parts = version.replace("R", "").split("_");
            if (parts.length >= 1) {
                return Integer.parseInt(parts[0]);
            }
        } catch (Exception ignored) {}
        return 1;
    }

    private int parseMinor() {
        try {
            String[] parts = version.replace("R", "").split("_");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    public String describe() {
        return "NmsService{" +
                "version='" + version + '\'' +
                ", parsed=" + majorVersion + "." + minorVersion +
                ", modern=" + modern +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NmsService)) return false;
        NmsService that = (NmsService) o;
        return modern == that.modern && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, modern);
    }
}

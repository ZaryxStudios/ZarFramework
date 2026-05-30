package com.zaryx.okaso.bukkit.utility;

import com.zaryx.okaso.bukkit.placeholder.PlaceholderContext;
import com.zaryx.okaso.bukkit.placeholder.PlaceholderResolver;
import com.zaryx.okaso.bukkit.utility.color.ColorParser;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Text {

    private static final Logger LOGGER = Logger.getLogger(Text.class.getName());

    private Text() {
    }

    public static String colorize(String message) {
        return ColorParser.parse(message);
    }

    public static String text(String message) {
        return colorize(message);
    }

    public static String text(String message, PlaceholderContext context) {
        if (message == null) return "";
        String processed = PlaceholderResolver.apply(message, context);
        return colorize(processed);
    }

    public static List<String> colorize(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<String>(messages.size());
        for (String line : messages) {
            out.add(colorize(line));
        }
        return out;
    }

    public static List<String> text(List<String> messages) {
        return colorize(messages);
    }

    public static List<String> text(List<String> messages, PlaceholderContext context) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<String>(messages.size());
        for (String line : messages) {
            out.add(text(line, context));
        }
        return out;
    }

    public static String[] colorize(String... messages) {
        if (messages == null) {
            return new String[0];
        }

        String[] out = new String[messages.length];
        for (int i = 0; i < messages.length; i++) {
            out[i] = colorize(messages[i]);
        }
        return out;
    }

    public static void send(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(colorize(message));
        }
    }

    public static void send(CommandSender sender, String message) {
        if (sender != null && message != null) {
            sender.sendMessage(colorize(message));
        }
    }

    public static void send(Player player, String... messages) {
        if (player == null || messages == null) {
            return;
        }

        for (String message : messages) {
            send(player, message);
        }
    }

    public static void send(CommandSender sender, String... messages) {
        if (sender == null || messages == null) {
            return;
        }

        for (String message : messages) {
            send(sender, message);
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }

        String colored = colorize(message);
        try {
            Object spigot = player.spigot();

            try {
                Method m = spigot.getClass().getMethod("sendMessage", ChatMessageType.class, net.md_5.bungee.api.chat.BaseComponent[].class);
                m.invoke(spigot, ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(colored));
                return;
            } catch (NoSuchMethodException e) {
                LOGGER.log(Level.FINE, "Spigot action bar signature with ChatMessageType was not available.", e);
            }

            try {
                Method m2 = spigot.getClass().getMethod("sendMessage", net.md_5.bungee.api.chat.BaseComponent[].class);
                m2.invoke(spigot, new Object[]{TextComponent.fromLegacyText(colored)});
                return;
            } catch (NoSuchMethodException e) {
                LOGGER.log(Level.FINE, "Spigot action bar varargs signature was not available.", e);
            }

        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Spigot action bar delivery failed, falling back to NMS.", e);
        }

        try {
            String version = Reflection.getNmsVersion();
            Class<?> iChatBase = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> serializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");

            String json = "{\"text\":\"" + escapeJson(colored) + "\"}";
            Object component = serializer.getMethod("a", String.class).invoke(null, json);
            Object packet = packetClass.getConstructor(iChatBase, byte.class).newInstance(component, (byte) 2);
            Reflection.sendPacket(player, packet);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "NMS action bar delivery failed, falling back to chat.", e);
            player.sendMessage(colored);
        }
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }

        String t = colorize(title == null ? "" : title);
        String s = colorize(subtitle == null ? "" : subtitle);

        try {
            Method sendTitle = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            sendTitle.invoke(player, t, s, fadeIn, stay, fadeOut);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Title delivery failed, falling back to chat messages.", e);
            if (t.length() > 0) {
                player.sendMessage(t);
            }
            if (s.length() > 0) {
                player.sendMessage(s);
            }
        }
    }

    public static void sendTitle(Player player, String title) {
        sendTitle(player, title, "", 10, 50, 10);
    }

    public static String strip(String message) {
        return message == null ? "" : net.md_5.bungee.api.ChatColor.stripColor(colorize(message));
    }

    public static String separator(char character, int length) {
        if (length <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(character);
        }
        return sb.toString();
    }

    public static String center(String message, int maxWidth) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        int plainLength = strip(message).length();
        int spaces = Math.max(0, (maxWidth - plainLength) / 2);

        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            pad.append(' ');
        }

        return pad + message;
    }

    public static String progressBar(double current, double max, int length, String filled, String empty) {
        if (length <= 0) {
            return "";
        }

        double ratio = max <= 0 ? 0.0 : current / max;
        if (ratio < 0.0) ratio = 0.0;
        if (ratio > 1.0) ratio = 1.0;

        int full = (int) Math.round(ratio * length);
        if (full > length) full = length;

        StringBuilder sb = new StringBuilder();
        String fullPart = colorize(filled == null ? "" : filled);
        String emptyPart = colorize(empty == null ? "" : empty);

        for (int i = 0; i < full; i++) {
            sb.append(fullPart);
        }
        for (int i = full; i < length; i++) {
            sb.append(emptyPart);
        }

        return sb.toString();
    }

    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

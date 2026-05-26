package com.zaryx.framework.bukkit.utility.color;

import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorParser {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern BRACKET_HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern VANILLA_HEX_PATTERN = Pattern.compile("&x(?:&[A-Fa-f0-9]){6}");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#?[A-Fa-f0-9]{6}),(#?[A-Fa-f0-9]{6})>(.*?)</gradient>", Pattern.DOTALL);
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("<rainbow>(.*?)</rainbow>", Pattern.DOTALL);
    private static final Pattern FORMAT_TAG_PATTERN = Pattern.compile("<(bold|italic|underline|strikethrough|magic)>(.*?)</\\1>", Pattern.DOTALL);

    private static Method bungeeOffMethod;
    private static boolean hexSupported;

    static {
        try {
            Class<?> chatColor = Class.forName("net.md_5.bungee.api.ChatColor");
            bungeeOffMethod = chatColor.getMethod("of", String.class);
            hexSupported = true;
        } catch (Exception e) {
            hexSupported = false;
        }
    }

    private ColorParser() {}

    public static String parse(String text) {
        if (text == null) return "";

        text = applyFormatTags(text);
        text = applyGradient(text);
        text = applyRainbow(text);
        text = normalizeVanillaHex(text);
        text = applyHex(text, HEX_PATTERN, 1);
        text = applyHex(text, BRACKET_HEX_PATTERN, 1);

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String applyFormatTags(String text) {
        Matcher matcher = FORMAT_TAG_PATTERN.matcher(text);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase();
            String content = matcher.group(2);
            String replacement = legacyCodeForTag(tag) + content + ChatColor.RESET;
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private static String legacyCodeForTag(String tag) {
        if ("bold".equals(tag)) return ChatColor.BOLD.toString();
        if ("italic".equals(tag)) return ChatColor.ITALIC.toString();
        if ("underline".equals(tag)) return ChatColor.UNDERLINE.toString();
        if ("strikethrough".equals(tag)) return ChatColor.STRIKETHROUGH.toString();
        if ("magic".equals(tag)) return ChatColor.MAGIC.toString();
        return "";
    }

    private static String applyGradient(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String start = normalizeHex(matcher.group(1));
            String end = normalizeHex(matcher.group(2));
            String content = matcher.group(3);
            matcher.appendReplacement(out, Matcher.quoteReplacement(gradient(content, start, end)));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private static String applyRainbow(String text) {
        Matcher matcher = RAINBOW_PATTERN.matcher(text);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            matcher.appendReplacement(out, Matcher.quoteReplacement(rainbow(content)));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private static String gradient(String input, String startHex, String endHex) {
        if (input.isEmpty()) return "";

        int[] start = toRgb(startHex);
        int[] end = toRgb(endHex);
        StringBuilder sb = new StringBuilder();
        int len = input.length();

        for (int i = 0; i < len; i++) {
            float ratio = len == 1 ? 0f : (float) i / (float) (len - 1);
            int r = start[0] + Math.round((end[0] - start[0]) * ratio);
            int g = start[1] + Math.round((end[1] - start[1]) * ratio);
            int b = start[2] + Math.round((end[2] - start[2]) * ratio);
            sb.append(colorToken(String.format("%02X%02X%02X", r, g, b))).append(input.charAt(i));
        }

        return sb.append(ChatColor.RESET).toString();
    }

    private static String rainbow(String input) {
        if (input.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int len = input.length();
        for (int i = 0; i < len; i++) {
            float hue = (float) i / (float) len;
            Color color = Color.getHSBColor(hue, 0.85f, 0.95f);
            sb.append(colorToken(String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue())));
            sb.append(input.charAt(i));
        }

        return sb.append(ChatColor.RESET).toString();
    }

    private static String normalizeVanillaHex(String text) {
        Matcher matcher = VANILLA_HEX_PATTERN.matcher(text);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            MatchResult mr = matcher.toMatchResult();
            String token = mr.group();
            String hex = "" + token.charAt(3) + token.charAt(5) + token.charAt(7) + token.charAt(9) + token.charAt(11) + token.charAt(13);
            matcher.appendReplacement(out, Matcher.quoteReplacement(colorToken(hex)));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private static String applyHex(String text, Pattern pattern, int groupHex) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(groupHex);
            matcher.appendReplacement(out, Matcher.quoteReplacement(colorToken(hex)));
        }

        matcher.appendTail(out);
        return out.toString();
    }

    private static String colorToken(String hex) {
        String clean = normalizeHex(hex);
        if (hexSupported) {
            try {
                Object color = bungeeOffMethod.invoke(null, clean);
                return String.valueOf(color);
            } catch (Exception ignored) {
                // Fall through to legacy approximation.
            }
        }

        int[] rgb = toRgb(clean);
        return nearestLegacyColor(rgb[0], rgb[1], rgb[2]);
    }

    private static String normalizeHex(String hex) {
        return hex.startsWith("#") ? hex : "#" + hex;
    }

    private static int[] toRgb(String hex) {
        String clean = normalizeHex(hex).substring(1);
        return new int[]{
                Integer.parseInt(clean.substring(0, 2), 16),
                Integer.parseInt(clean.substring(2, 4), 16),
                Integer.parseInt(clean.substring(4, 6), 16)
        };
    }

    private static String nearestLegacyColor(int r, int g, int b) {
        int[][] colors = {
                {0, 0, 0}, {0, 0, 170}, {0, 170, 0}, {0, 170, 170},
                {170, 0, 0}, {170, 0, 170}, {255, 170, 0}, {170, 170, 170},
                {85, 85, 85}, {85, 85, 255}, {85, 255, 85}, {85, 255, 255},
                {255, 85, 85}, {255, 85, 255}, {255, 255, 85}, {255, 255, 255}
        };

        ChatColor[] codes = {
                ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
                ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
                ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
                ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
        };

        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < colors.length; i++) {
            int dr = r - colors[i][0];
            int dg = g - colors[i][1];
            int db = b - colors[i][2];
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }

        return codes[best].toString();
    }
}

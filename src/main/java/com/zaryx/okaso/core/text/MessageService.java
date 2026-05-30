package com.zaryx.okaso.core.text;

import com.zaryx.okaso.bukkit.placeholder.PlaceholderContext;
import com.zaryx.okaso.bukkit.placeholder.PlaceholderResolver;
import com.zaryx.okaso.bukkit.utility.color.ColorParser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageService {

    private static final Logger LOGGER = Logger.getLogger(MessageService.class.getName());

    private final Map<String, Map<String, String>> translations = new ConcurrentHashMap<String, Map<String, String>>();
    private volatile String defaultLocale = "en_US";

    // Applies color formatting to a raw message.
    public String format(String message) {
        return ColorParser.parse(message);
    }

    // Applies placeholders first, then color formatting.
    public String format(String message, PlaceholderContext context) {
        return format(PlaceholderResolver.apply(message, context));
    }

    // Sends a formatted message to any command sender.
    public void send(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(format(message));
    }

    public void send(CommandSender sender, String message, PlaceholderContext context) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(format(message, context));
    }

    public void send(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        player.sendMessage(format(message));
    }

    public void send(Player player, String message, PlaceholderContext context) {
        if (player == null || message == null) {
            return;
        }
        player.sendMessage(format(message, context));
    }

    // Sends each line one by one so formatting stays consistent.
    public void sendLines(CommandSender sender, Collection<String> lines) {
        if (sender == null || lines == null) {
            return;
        }
        for (String line : lines) {
            send(sender, line);
        }
    }

    // Broadcasts the same formatted message to every online player.
    public void broadcast(String message) {
        if (message == null) {
            return;
        }
        Bukkit.broadcastMessage(format(message));
    }

    public void broadcast(String message, PlaceholderContext context) {
        if (message == null) {
            return;
        }
        Bukkit.broadcastMessage(format(message, context));
    }

    // Uses Spigot's action bar API when available, with chat fallback.
    public void sendActionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }

        String formatted = format(message);
        try {
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            Class<?> chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object actionBar = null;
            Object[] constants = chatMessageType.getEnumConstants();
            if (constants != null) {
                for (Object constant : constants) {
                    if (constant != null && "ACTION_BAR".equals(constant.toString())) {
                        actionBar = constant;
                        break;
                    }
                }
            }
            if (actionBar == null) {
                throw new IllegalStateException("ACTION_BAR not available");
            }
            Method fromLegacyText = textComponent.getMethod("fromLegacyText", String.class);
            Object components = fromLegacyText.invoke(null, formatted);
            Method sendMessage = spigot.getClass().getMethod("sendMessage", chatMessageType, Class.forName("net.md_5.bungee.api.chat.BaseComponent[]"));
            sendMessage.invoke(spigot, actionBar, components);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Failed to send action bar through the Spigot API, falling back to chat.", e);
            player.sendMessage(formatted);
        }
    }

    // Uses the player title API and falls back to chat if needed.
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }

        String formattedTitle = title == null ? "" : format(title);
        String formattedSubtitle = subtitle == null ? "" : format(subtitle);

        try {
            Method sendTitle = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            sendTitle.invoke(player, formattedTitle, formattedSubtitle, fadeIn, stay, fadeOut);
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "Failed to send title through the player API, falling back to chat messages.", e);
            if (!formattedTitle.isEmpty()) {
                player.sendMessage(formattedTitle);
            }
            if (!formattedSubtitle.isEmpty()) {
                player.sendMessage(formattedSubtitle);
            }
        }
    }

    // Prefixes the text before routing it through the normal sender flow.
    public void sendPrefixed(CommandSender sender, String prefix, String message) {
        if (sender == null || message == null) {
            return;
        }
        send(sender, prefix + message);
    }

    // Sets the locale used by register() and translate().
    public void setDefaultLocale(String defaultLocale) {
        if (defaultLocale == null || defaultLocale.trim().isEmpty()) {
            return;
        }

        this.defaultLocale = defaultLocale.trim();
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    // Registers a message in the default locale.
    public void register(String key, String message) {
        register(defaultLocale, key, message);
    }

    // Registers a message bundle for a specific locale.
    public void register(String locale, String key, String message) {
        String normalizedLocale = normalizeLocale(locale);
        String normalizedKey = normalizeKey(key);

        if (normalizedLocale == null || normalizedKey == null || message == null) {
            return;
        }

        Map<String, String> bundle = translations.get(normalizedLocale);
        if (bundle == null) {
            bundle = new ConcurrentHashMap<String, String>();
            Map<String, String> previous = translations.putIfAbsent(normalizedLocale, bundle);
            if (previous != null) {
                bundle = previous;
            }
        }

        bundle.put(normalizedKey, message);
    }

    // Registers all messages from the provided map.
    public void register(String locale, Map<String, String> messages) {
        String normalizedLocale = normalizeLocale(locale);
        if (normalizedLocale == null || messages == null || messages.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : messages.entrySet()) {
            register(normalizedLocale, entry.getKey(), entry.getValue());
        }
    }

    // Returns an immutable view of the locale bundle.
    public Map<String, String> getTranslations(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        if (normalizedLocale == null) {
            return Collections.emptyMap();
        }

        Map<String, String> bundle = translations.get(normalizedLocale);
        if (bundle == null || bundle.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(bundle);
    }

    // Translates using the default locale.
    public String translate(String key) {
        return translate(defaultLocale, key, null);
    }

    public String translate(String key, PlaceholderContext context) {
        return translate(defaultLocale, key, context);
    }

    public String translate(String locale, String key) {
        return translate(locale, key, null);
    }

    public String translate(String locale, String key, PlaceholderContext context) {
        String resolved = resolve(locale, key);
        return context == null ? format(resolved) : format(resolved, context);
    }

    public void sendKey(CommandSender sender, String key) {
        sendKey(sender, defaultLocale, key, null);
    }

    public void sendKey(CommandSender sender, String key, PlaceholderContext context) {
        sendKey(sender, defaultLocale, key, context);
    }

    public void sendKey(CommandSender sender, String locale, String key, PlaceholderContext context) {
        if (sender == null || key == null) {
            return;
        }

        sender.sendMessage(translate(locale, key, context));
    }

    public void sendKey(Player player, String key) {
        sendKey(player, defaultLocale, key, null);
    }

    public void sendKey(Player player, String key, PlaceholderContext context) {
        sendKey(player, defaultLocale, key, context);
    }

    public void sendKey(Player player, String locale, String key, PlaceholderContext context) {
        if (player == null || key == null) {
            return;
        }

        player.sendMessage(translate(locale, key, context));
    }

    public void broadcastKey(String key) {
        broadcastKey(defaultLocale, key, null);
    }

    public void broadcastKey(String locale, String key, PlaceholderContext context) {
        if (key == null) {
            return;
        }

        Bukkit.broadcastMessage(translate(locale, key, context));
    }

    private String resolve(String locale, String key) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null) {
            return "";
        }

        String normalizedLocale = normalizeLocale(locale);
        if (normalizedLocale != null) {
            Map<String, String> bundle = translations.get(normalizedLocale);
            if (bundle != null) {
                String translated = bundle.get(normalizedKey);
                if (translated != null) {
                    return translated;
                }
            }
        }

        Map<String, String> fallback = translations.get(defaultLocale);
        if (fallback != null) {
            String translated = fallback.get(normalizedKey);
            if (translated != null) {
                return translated;
            }
        }

        return normalizedKey;
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.trim().isEmpty()) {
            return null;
        }

        return locale.trim();
    }

    private String normalizeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        return key.trim();
    }
}

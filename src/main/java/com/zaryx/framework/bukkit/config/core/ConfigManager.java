package com.zaryx.framework.bukkit.config.core;

import com.zaryx.framework.bukkit.config.annotation.ConfigFile;
import com.zaryx.framework.bukkit.config.annotation.ConfigPath;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ConfigManager {

    private static ConfigManager instance;

    private final Map<Class<?>, FileConfiguration> cache = new HashMap<>();
    private final Map<Class<?>, File> files = new HashMap<>();
    private final Map<Class<?>, Boolean> persistence = new HashMap<>();

    public ConfigManager() {
        instance = this;
    }

    public static ConfigManager getInstance() {
        return instance;
    }

    public void load(Class<?> clazz, Plugin plugin) {
        ConfigFile ann = clazz.getAnnotation(ConfigFile.class);
        if (ann == null) {
            throw new IllegalStateException("Missing @ConfigFile on " + clazz.getName());
        }

        FileConfiguration yaml;
        if (ann.exposeToDisk()) {
            this.create(clazz, plugin);
            File file = new File(plugin.getDataFolder(), ann.value());
            yaml = YamlConfiguration.loadConfiguration(file);
            this.files.put(clazz, file);
        } else {
            yaml = this.loadInMemory(ann, plugin);
            this.files.remove(clazz);
        }

        this.cache.put(clazz, yaml);
        this.persistence.put(clazz, ann.exposeToDisk());
        this.injectValues(clazz, yaml);
    }

        public void save(Class<?> clazz) {
        FileConfiguration yaml = this.cache.get(clazz);
            boolean persistent = this.persistence.getOrDefault(clazz, false);

            if (yaml == null) {
            throw new IllegalStateException(
                "Config not loaded: " + clazz.getSimpleName()
            );
        }

        this.applyValuesToConfig(clazz, yaml);

            if (!persistent) {
                return;
            }

            File file = this.files.get(clazz);
            if (file == null) {
                throw new IllegalStateException("Config file reference missing for " + clazz.getSimpleName());
            }

        try {
            yaml.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save " + file.getName(), e);
        }
    }

    public void reload(Class<?> clazz, Plugin plugin) {
        this.cache.remove(clazz);
        this.files.remove(clazz);
        this.persistence.remove(clazz);
        this.load(clazz, plugin);
    }

    private FileConfiguration loadInMemory(ConfigFile ann, Plugin plugin) {
        InputStream stream = plugin.getResource(ann.value());
        if (stream == null) {
            return new YamlConfiguration();
        }

        try (InputStream in = stream; InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load in-memory config " + ann.value(), e);
        }
    }

    private void create(Class<?> clazz, Plugin plugin) {
        ConfigFile ann = clazz.getAnnotation(ConfigFile.class);
        if (ann == null) {
            throw new IllegalStateException("Missing @ConfigFile on " + clazz.getName());
        }

        File file = new File(plugin.getDataFolder(), ann.value());
        if (file.exists()) return;

        plugin.getDataFolder().mkdirs();

        try {
            if (plugin.getResource(ann.value()) != null) {
                plugin.saveResource(ann.value(), false);
            } else {
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create " + ann.value(), e);
        }
    }

    private void injectValues(Class<?> clazz, FileConfiguration yaml) {
        for (Field field : clazz.getDeclaredFields()) {

            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!field.isAnnotationPresent(ConfigPath.class)) continue;

            try {
                field.setAccessible(true);

                String path = field.getAnnotation(ConfigPath.class).value();
                Object value = yaml.get(path);

                if (value == null) {
                    yaml.set(path, field.get(null));
                } else {
                    field.set(null, value);
                }

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error injecting " + field.getName(), e);
            }
        }
    }

    private void applyValuesToConfig(Class<?> clazz, FileConfiguration yaml) {
        for (Field field : clazz.getDeclaredFields()) {

            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!field.isAnnotationPresent(ConfigPath.class)) continue;

            try {
                field.setAccessible(true);
                String path = field.getAnnotation(ConfigPath.class).value();
                yaml.set(path, field.get(null));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error saving " + field.getName(), e);
            }
        }
    }
}
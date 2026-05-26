package com.zaryx.framework.bungee.config.core;

import com.google.common.io.ByteStreams;
import com.zaryx.framework.bungee.config.annotation.BungeeConfigFile;
import com.zaryx.framework.bungee.config.annotation.BungeeConfigPath;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Getter
public class BungeeConfigManager {

    @Getter
    private static BungeeConfigManager instance;

    private final Map<Class<?>, Configuration> cache = new HashMap<>();
    private final Map<Class<?>, File> files = new HashMap<>();
    private final Map<Class<?>, Boolean> persistence = new HashMap<>();

    public BungeeConfigManager() {
        instance = this;
    }

    public void load(Class<?> clazz, Plugin plugin) {
        BungeeConfigFile ann = clazz.getAnnotation(BungeeConfigFile.class);
        if (ann == null) {
            throw new IllegalStateException("Missing @ConfigFile on " + clazz.getName());
        }

        try {
            Configuration yaml;
            if (ann.exposeToDisk()) {
                this.create(clazz, plugin);
                File file = new File(plugin.getDataFolder(), ann.value());
                yaml = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                this.files.put(clazz, file);
            } else {
                yaml = this.loadInMemory(ann, plugin);
                this.files.remove(clazz);
            }

            this.cache.put(clazz, yaml);
            this.persistence.put(clazz, ann.exposeToDisk());
            this.injectValues(clazz, yaml);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config for " + clazz.getSimpleName(), e);
        }
    }

    public void save(Class<?> clazz) {
        Configuration yaml = this.cache.get(clazz);
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
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(yaml, file);
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

    private Configuration loadInMemory(BungeeConfigFile ann, Plugin plugin) throws IOException {
        if (plugin.getResourceAsStream(ann.value()) == null) {
            return new Configuration();
        }

        byte[] data = ByteStreams.toByteArray(plugin.getResourceAsStream(ann.value()));
        return ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(new ByteArrayInputStream(data));
    }


    private void create(Class<?> clazz, Plugin plugin) {
        BungeeConfigFile ann = clazz.getAnnotation(BungeeConfigFile.class);
        if (ann == null) {
            throw new IllegalStateException("Missing @ConfigFile on " + clazz.getName());
        }

        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(plugin.getDataFolder(), ann.value());
        if (file.exists()) return;

        plugin.getDataFolder().mkdirs();

        try {
            if (plugin.getResourceAsStream(ann.value()) != null) {
                plugin.getDataFolder().mkdirs();
                ByteStreams.copy(plugin.getResourceAsStream(ann.value()), Files.newOutputStream(file.toPath()));
            } else {
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create " + ann.value(), e);
        }
    }

    private void injectValues(Class<?> clazz, Configuration yaml) {
        for (Field field : clazz.getDeclaredFields()) {

            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!field.isAnnotationPresent(BungeeConfigPath.class)) continue;

            try {
                field.setAccessible(true);

                String path = field.getAnnotation(BungeeConfigPath.class).value();
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

    private void applyValuesToConfig(Class<?> clazz, Configuration yaml) {
        for (Field field : clazz.getDeclaredFields()) {

            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!field.isAnnotationPresent(BungeeConfigPath.class)) continue;

            try {
                field.setAccessible(true);
                String path = field.getAnnotation(BungeeConfigPath.class).value();
                yaml.set(path, field.get(null));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error saving " + field.getName(), e);
            }
        }
    }
}
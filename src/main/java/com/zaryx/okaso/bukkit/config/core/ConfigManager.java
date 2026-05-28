package com.zaryx.okaso.bukkit.config.core;

import com.zaryx.okaso.bukkit.config.annotation.ConfigFile;
import com.zaryx.okaso.bukkit.config.annotation.ConfigPath;
import com.zaryx.okaso.core.config.AbstractConfigManager;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

@Getter
public class ConfigManager extends AbstractConfigManager<FileConfiguration> {

    private static ConfigManager instance;

    public ConfigManager() {
        instance = this;
    }

    public static ConfigManager getInstance() {
        return instance;
    }

    @Override
    protected ConfigDescriptor describe(Class<?> clazz) {
        ConfigFile ann = clazz.getAnnotation(ConfigFile.class);
        if (ann == null) {
            throw new IllegalStateException("Missing @ConfigFile on " + clazz.getName());
        }

        return new ConfigDescriptor(ann.value(), ann.exposeToDisk());
    }

    @Override
    protected FileConfiguration loadPersistent(Class<?> clazz, Object runtime, ConfigDescriptor descriptor) {
        Plugin plugin = (Plugin) runtime;
        File file = this.create(plugin, descriptor.path);
        return YamlConfiguration.loadConfiguration(file);
    }

    @Override
    protected FileConfiguration loadTransient(Class<?> clazz, Object runtime, ConfigDescriptor descriptor) {
        Plugin plugin = (Plugin) runtime;
        InputStream stream = plugin.getResource(descriptor.path);
        if (stream == null) {
            return new YamlConfiguration();
        }

        try (InputStream in = stream; InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load in-memory config " + descriptor.path, e);
        }
    }

    @Override
    protected void savePersistent(Class<?> clazz, FileConfiguration configuration, Object runtime, String path) {
        try {
            Plugin plugin = (Plugin) runtime;
            File file = new File(plugin.getDataFolder(), path);
            configuration.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config " + path, e);
        }
    }

    @Override
    protected Object readValue(FileConfiguration configuration, String path) {
        return configuration.get(path);
    }

    @Override
    protected void writeValue(FileConfiguration configuration, String path, Object value) {
        configuration.set(path, value);
    }

    @Override
    protected Object defaultValue(Class<?> clazz, Field field) {
        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error reading default value for " + field.getName(), e);
        }
    }

    @Override
    protected boolean isBindableField(Field field) {
        return field.isAnnotationPresent(ConfigPath.class);
    }

    @Override
    protected String path(Field field) {
        return field.getAnnotation(ConfigPath.class).value();
    }

    private File create(Plugin plugin, String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);
        if (file.exists()) {
            return file;
        }

        plugin.getDataFolder().mkdirs();

        try {
            if (plugin.getResource(relativePath) != null) {
                plugin.saveResource(relativePath, false);
            } else {
                file.createNewFile();
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create " + relativePath, e);
        }
    }
}

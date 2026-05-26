package com.zaryx.framework.bungee.config.core;

import com.google.common.io.ByteStreams;
import com.zaryx.framework.bungee.config.annotation.BungeeConfigFile;
import com.zaryx.framework.bungee.config.annotation.BungeeConfigPath;
import com.zaryx.framework.core.config.AbstractConfigManager;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Getter
public class BungeeConfigManager extends AbstractConfigManager<Configuration> {

    @Getter
    private static BungeeConfigManager instance;

    public BungeeConfigManager() {
        instance = this;
    }

    @Override
    protected ConfigDescriptor describe(Class<?> clazz) {
        BungeeConfigFile ann = clazz.getAnnotation(BungeeConfigFile.class);
        if (ann == null) {
            throw new IllegalStateException("Missing @ConfigFile on " + clazz.getName());
        }

        return new ConfigDescriptor(ann.value(), ann.exposeToDisk());
    }

    @Override
    protected Configuration loadPersistent(Class<?> clazz, Object runtime, ConfigDescriptor descriptor) {
        Plugin plugin = (Plugin) runtime;
        File file = this.create(plugin, descriptor.path);

        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config for " + clazz.getSimpleName(), e);
        }
    }

    @Override
    protected Configuration loadTransient(Class<?> clazz, Object runtime, ConfigDescriptor descriptor) {
        Plugin plugin = (Plugin) runtime;
        InputStream resource = plugin.getResourceAsStream(descriptor.path);
        if (resource == null) {
            return new Configuration();
        }

        try (InputStream in = resource) {
            byte[] data = ByteStreams.toByteArray(in);
            return ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new ByteArrayInputStream(data));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load in-memory config " + descriptor.path, e);
        }
    }

    @Override
    protected void savePersistent(Class<?> clazz, Configuration configuration, Object runtime, String path) {
        try {
            Plugin plugin = (Plugin) runtime;
            ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .save(configuration, new File(plugin.getDataFolder(), path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save " + path, e);
        }
    }

    @Override
    protected Object readValue(Configuration configuration, String path) {
        return configuration.get(path);
    }

    @Override
    protected void writeValue(Configuration configuration, String path, Object value) {
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
        return field.isAnnotationPresent(BungeeConfigPath.class);
    }

    @Override
    protected String path(Field field) {
        return field.getAnnotation(BungeeConfigPath.class).value();
    }

    private File create(Plugin plugin, String relativePath) {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(plugin.getDataFolder(), relativePath);
        if (file.exists()) {
            return file;
        }

        try {
            InputStream resource = plugin.getResourceAsStream(relativePath);
            if (resource != null) {
                try (InputStream in = resource) {
                    ByteStreams.copy(in, Files.newOutputStream(file.toPath()));
                }
            } else {
                file.createNewFile();
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create " + relativePath, e);
        }
    }
}

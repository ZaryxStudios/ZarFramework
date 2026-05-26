package com.zaryx.framework.core.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractConfigManager<C> {

    private final java.util.concurrent.ConcurrentMap<Class<?>, ConfigBinding<C>> bindings = new java.util.concurrent.ConcurrentHashMap<>();

    public final void load(Class<?> clazz, Object runtime) {
        ConfigDescriptor descriptor = this.describe(clazz);
        C configuration = descriptor.persistent
                ? this.loadPersistent(clazz, runtime, descriptor)
                : this.loadTransient(clazz, runtime, descriptor);

        this.bindings.put(clazz, new ConfigBinding<>(configuration, runtime, descriptor.path, descriptor.persistent));
        this.injectValues(clazz, configuration);
    }

    public final void save(Class<?> clazz) {
        ConfigBinding<C> binding = this.requireBinding(clazz);
        this.applyValues(clazz, binding.configuration);

        if (!binding.persistent) {
            return;
        }

        this.savePersistent(clazz, binding.configuration, binding.runtime, binding.path);
    }

    public final void reload(Class<?> clazz, Object runtime) {
        this.bindings.remove(clazz);
        this.load(clazz, runtime);
    }

    protected final C getConfiguration(Class<?> clazz) {
        return this.requireBinding(clazz).configuration;
    }

    protected abstract ConfigDescriptor describe(Class<?> clazz);

    protected abstract C loadPersistent(Class<?> clazz, Object runtime, ConfigDescriptor descriptor);

    protected abstract C loadTransient(Class<?> clazz, Object runtime, ConfigDescriptor descriptor);

    protected abstract void savePersistent(Class<?> clazz, C configuration, Object runtime, String path);

    protected abstract Object readValue(C configuration, String path);

    protected abstract void writeValue(C configuration, String path, Object value);

    protected abstract Object defaultValue(Class<?> clazz, Field field);

    protected void injectValues(Class<?> clazz, C configuration) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !this.isBindableField(field)) {
                continue;
            }

            try {
                field.setAccessible(true);
                String path = this.path(field);
                Object value = this.readValue(configuration, path);

                if (value == null) {
                    this.writeValue(configuration, path, this.defaultValue(clazz, field));
                } else {
                    field.set(null, value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error injecting " + field.getName(), e);
            }
        }
    }

    protected void applyValues(Class<?> clazz, C configuration) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !this.isBindableField(field)) {
                continue;
            }

            try {
                field.setAccessible(true);
                this.writeValue(configuration, this.path(field), field.get(null));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error saving " + field.getName(), e);
            }
        }
    }

    protected abstract boolean isBindableField(Field field);

    protected abstract String path(Field field);

    protected final ConfigBinding<C> requireBinding(Class<?> clazz) {
        ConfigBinding<C> binding = this.bindings.get(clazz);
        if (binding == null) {
            throw new IllegalStateException("Config not loaded: " + clazz.getSimpleName());
        }

        return binding;
    }

    protected static final class ConfigDescriptor {
        public final String path;
        public final boolean persistent;

        public ConfigDescriptor(String path, boolean persistent) {
            this.path = path;
            this.persistent = persistent;
        }
    }

    protected static final class ConfigBinding<C> {
        public final C configuration;
        public final Object runtime;
        public final String path;
        public final boolean persistent;

        public ConfigBinding(C configuration, Object runtime, String path, boolean persistent) {
            this.configuration = configuration;
            this.runtime = runtime;
            this.path = path;
            this.persistent = persistent;
        }
    }
}

package com.zaryx.framework.bukkit.storage.extra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.zaryx.framework.bukkit.storage.core.StorageContext;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JsonStorage implements StorageContext {

    private final File folder;
    private final Gson gson;
    private JsonObject root;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    public JsonStorage(String folderPath) {
        this.folder = new File(folderPath);
        if (!this.folder.exists()) {
            this.folder.mkdirs();
        }

        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

        this.loadFile();
    }

    private void loadFile() {
        try {
            if (!this.folder.exists()) {
                this.folder.getParentFile().mkdirs();
                this.folder.createNewFile();

                this.root = new JsonObject();
                this.saveFile();
            }

            try (Reader reader = new FileReader(this.folder)) {
                this.root = gson.fromJson(reader, JsonObject.class);
                if (this.root == null) this.root = new JsonObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception ignored) {
        }
    }

    public void saveFile() {
        lock.writeLock().lock();
        try {
            try (Writer writer = new FileWriter(this.folder)) {
                gson.toJson(this.root, writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private File file(String key) {
        return new File(this.folder, key + ".json");
    }

    @Override
    public <T> void save(String key, T value) {
        // Almacenar en caché antes de guardar
        cache.put(key, value);
        
        File file = this.file(key);

        try (Writer writer = new OutputStreamWriter(
                Files.newOutputStream(file.toPath()),
                StandardCharsets.UTF_8
        )) {
            this.gson.toJson(value, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save json key: " + key, e);
        }
    }

    @Override
    public <T> T load(String key, Class<T> clazz) {
        // Verificar en caché primero
        Object cached = cache.get(key);
        if (clazz.isInstance(cached)) {
            return clazz.cast(cached);
        }
        
        File file = this.file(key);
        if (!file.exists()) return null;

        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            T result = this.gson.fromJson(reader, clazz);
            // Almacenar en caché el resultado
            cache.put(key, result);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json key: " + key, e);
        }
    }

    @Override
    public <T> T load(String key, Type type) {
        File file = this.file(key);
        if (!file.exists()) return null;

        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            return this.gson.fromJson(reader, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json key: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return cache.containsKey(key) || this.file(key).exists();
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
        // Eliminar archivo físico
        this.file(key).delete();
    }
}
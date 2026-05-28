package com.zaryx.okaso.bukkit.storage.extra;

import com.zaryx.okaso.bukkit.storage.core.AbstractJsonStorageContext;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class JsonStorage extends AbstractJsonStorageContext {

    private final File directory;

    public JsonStorage(String folderPath) {
        this.directory = new File(folderPath);
        this.directory.mkdirs();
        this.connect();
    }

    @Override
    public boolean connect() {
        this.directory.mkdirs();
        return super.connect();
    }

    @Override
    protected void writeJson(String key, String json) {
        File file = this.file(key);
        try (Writer writer = new OutputStreamWriter(
                Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8
        )) {
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save json key: " + key, e);
        }
    }

    @Override
    protected String readJson(String key) {
        File file = this.file(key);
        if (!file.exists()) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1024];
            int read;

            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }

            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json key: " + key, e);
        }
    }

    @Override
    protected boolean existsJson(String key) {
        return this.file(key).exists();
    }

    @Override
    protected boolean deleteJson(String key) {
        return this.file(key).delete();
    }

    @Override
    protected Set<String> snapshotKeys() {
        Set<String> keys = new HashSet<>();
        File[] files = this.directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return keys;
        }

        for (File file : files) {
            String name = file.getName();
            keys.add(name.substring(0, name.length() - 5));
        }

        return keys;
    }

    private File file(String key) {
        return new File(this.directory, key + ".json");
    }
}

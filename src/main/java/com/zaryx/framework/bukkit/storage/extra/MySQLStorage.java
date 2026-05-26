package com.zaryx.framework.bukkit.storage.extra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaryx.framework.bukkit.storage.core.StorageContext;

import java.lang.reflect.Type;
import java.sql.*;

public class MySQLStorage implements StorageContext {

    private final Connection connection;
    private final Gson gson;

    public MySQLStorage(String host, int port, String database, String user, String password) {
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database +
                            "?useSSL=false&characterEncoding=utf8",
                    user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to MySQL", e);
        }

        this.gson = new GsonBuilder().serializeNulls().create();
        this.createTable();
    }

    private void createTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS storage_data (" +
                            "`key` VARCHAR(128) PRIMARY KEY," +
                            "`value` LONGTEXT NOT NULL" +
                            ")"
            );
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create storage table", e);
        }
    }

    @Override
    public <T> void save(String key, T value) {
        String json = gson.toJson(value);

        try (PreparedStatement stmt = connection.prepareStatement(
                "REPLACE INTO storage_data (`key`, `value`) VALUES (?, ?)"
        )) {
            stmt.setString(1, key);
            stmt.setString(2, json);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save key: " + key, e);
        }
    }

    @Override
    public <T> T load(String key, Class<T> clazz) {
        String json = getJson(key);
        return json == null ? null : gson.fromJson(json, clazz);
    }

    @Override
    public <T> T load(String key, Type type) {
        String json = getJson(key);
        return json == null ? null : gson.fromJson(json, type);
    }

    @Override
    public boolean exists(String key) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM storage_data WHERE `key`=?"
        )) {
            stmt.setString(1, key);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String key) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM storage_data WHERE `key`=?"
        )) {
            stmt.setString(1, key);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getJson(String key) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT `value` FROM storage_data WHERE `key`=?"
        )) {
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) return null;
            return rs.getString("value");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load key: " + key, e);
        }
    }
}
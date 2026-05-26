package com.zaryx.framework.bukkit.storage.extra;

import com.zaryx.framework.bukkit.storage.core.AbstractJsonStorageContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class MySQLStorage extends AbstractJsonStorageContext {

    private final Connection connection;

    public MySQLStorage(String host, int port, String database, String user, String password) {
        try {
            this.connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&characterEncoding=utf8", user, password
            );
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to MySQL", e);
        }

        this.createTable();
        this.connect();
    }

    @Override
    public boolean connect() {
        return super.connect();
    }

    @Override
    public void disconnect() {
        try {
            this.connection.close();
            super.disconnect();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot disconnect from MySQL", e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return super.isConnected() && !this.connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    protected void writeJson(String key, String json) {
        try (PreparedStatement stmt = this.connection.prepareStatement(
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
    protected String readJson(String key) {
        try (PreparedStatement stmt = this.connection.prepareStatement(
                "SELECT `value` FROM storage_data WHERE `key`=?"
        )) {
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return rs.getString("value");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load key: " + key, e);
        }
    }

    @Override
    protected boolean existsJson(String key) {
        try (PreparedStatement stmt = this.connection.prepareStatement(
                "SELECT 1 FROM storage_data WHERE `key`=?"
        )) {
            stmt.setString(1, key);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean deleteJson(String key) {
        try (PreparedStatement stmt = this.connection.prepareStatement(
                "DELETE FROM storage_data WHERE `key`=?"
        )) {
            stmt.setString(1, key);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Set<String> snapshotKeys() {
        Set<String> keys = new HashSet<>();
        try (Statement stmt = this.connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT `key` FROM storage_data")) {
            while (rs.next()) {
                keys.add(rs.getString("key"));
            }
            return keys;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list keys", e);
        }
    }

    private void createTable() {
        try (Statement stmt = this.connection.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS storage_data (" +
                            "`key` VARCHAR(128) PRIMARY KEY, " +
                            "`value` LONGTEXT NOT NULL" +
                            ")"
            );
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create storage table", e);
        }
    }
}

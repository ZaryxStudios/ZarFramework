package com.zaryx.okaso.core.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class OkasoSerializer {

    private final Gson gson;

    public OkasoSerializer(Gson gson) {
        if (gson == null) {
            throw new IllegalArgumentException("Gson cannot be null");
        }
        this.gson = gson;
    }

    public Gson getGson() {
        return gson;
    }

    public String toJson(Object value) {
        return gson.toJson(value);
    }

    public <T> T fromJson(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    public <T> T fromJson(String json, Type type) throws JsonSyntaxException {
        return gson.fromJson(json, type);
    }

    public <T> T fromJson(String json, TypeToken<T> token) {
        return gson.fromJson(json, token.getType());
    }

    public byte[] toBytes(Object value) {
        return toJson(value).getBytes(StandardCharsets.UTF_8);
    }

    public <T> T fromBytes(byte[] data, Class<T> type) {
        if (data == null) return null;
        return fromJson(new String(data, StandardCharsets.UTF_8), type);
    }
}

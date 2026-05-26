package com.zaryx.framework.bukkit.serialization.core;

import com.google.gson.*;

import java.lang.reflect.Type;

public class AdapterPair<T> implements JsonSerializer<T>, JsonDeserializer<T> {

    private final JsonSerializer<T> serializer;
    private final JsonDeserializer<T> deserializer;

    AdapterPair(JsonSerializer<T> serializer, JsonDeserializer<T> deserializer) {
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext ctx) {
        return this.serializer.serialize(src, typeOfSrc, ctx);
    }

    @Override
    public T deserialize(JsonElement json, Type typeOffSrc, JsonDeserializationContext ctx) throws JsonParseException {
        return this.deserializer.deserialize(json, typeOffSrc, ctx);
    }
}
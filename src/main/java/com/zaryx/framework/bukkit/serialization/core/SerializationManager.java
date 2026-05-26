package com.zaryx.framework.bukkit.serialization.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;

import java.util.function.Consumer;

@Getter
public class SerializationManager {

    private static SerializationManager instance;

    private final Gson gson;

    public SerializationManager(Consumer<GsonBuilder> consumer) {
        GsonBuilder builder = new GsonBuilder();
        consumer.accept(builder);
        this.gson = builder.serializeNulls().create();
        instance = this;
    }

    public static SerializationManager getInstance() {
        return instance;
    }

    public void init(Consumer<GsonBuilder> consumer) {
        if (instance != null) {
            throw new IllegalStateException("Serialization Manager already initialized");
        }

        instance = new SerializationManager(consumer);
    }
}
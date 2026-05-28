package com.zaryx.okaso.bukkit.serialization;

import com.google.gson.*;
import com.zaryx.okaso.bukkit.serialization.core.SerializationManager;

public final class SerializationAPI {

    private SerializationAPI() {}

    public static Gson getGson() {
        return SerializationManager.getInstance().getGson();
    }
}

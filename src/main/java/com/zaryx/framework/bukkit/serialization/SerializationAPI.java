package com.zaryx.framework.bukkit.serialization;

import com.google.gson.*;
import com.zaryx.framework.bukkit.serialization.core.SerializationManager;

public final class SerializationAPI {

    private SerializationAPI() {}

    public static Gson getGson() {
        return SerializationManager.getInstance().getGson();
    }
}

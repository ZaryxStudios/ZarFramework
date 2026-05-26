package com.zaryx.framework.bukkit.nms;

import com.zaryx.framework.bukkit.utility.Reflection;

public class NmsService {

    private final String version;
    private final boolean modern;

    public NmsService() {
        this.version = Reflection.getNmsVersion();
        this.modern = Reflection.isModernNms();
    }

    public String getVersion() {
        return version;
    }

    public boolean isModern() {
        return modern;
    }

    public boolean isLegacy() {
        return !modern;
    }

    public Class<?> resolve(String... candidates) {
        return Reflection.getNMS(candidates);
    }

    public Class<?> resolveLegacy(String simpleName) {
        return Reflection.getNMS(simpleName);
    }

    public String describe() {
        return "NMS{" +
                "version='" + version + '\'' +
                ", modern=" + modern +
                '}';
    }
}
package com.zaryx.framework.bukkit.storage.core;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StorageParam {

    private StorageType storageType;

    private String folder;

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    private String url;
    private String collection;
}
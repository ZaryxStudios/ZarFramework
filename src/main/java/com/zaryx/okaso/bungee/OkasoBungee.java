package com.zaryx.okaso.bungee;

import com.zaryx.okaso.bungee.config.core.BungeeConfigManager;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

@Getter
public class OkasoBungee extends Plugin {

    @Getter private static OkasoBungee instance;
    private BungeeConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new BungeeConfigManager();
    }
}

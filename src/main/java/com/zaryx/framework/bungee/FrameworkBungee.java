package com.zaryx.framework.bungee;

import com.zaryx.framework.bungee.config.core.BungeeConfigManager;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

@Getter
public class FrameworkBungee extends Plugin {

    @Getter private static FrameworkBungee instance;
    private BungeeConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new BungeeConfigManager();
    }
}

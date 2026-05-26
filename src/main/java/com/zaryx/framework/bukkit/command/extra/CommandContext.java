package com.zaryx.framework.bukkit.command.extra;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public class CommandContext {

    private final CommandSender sender;
    private final Map<String, Object> values;

    public CommandContext(CommandSender sender) {
        this.sender = sender;
        this.values = new HashMap<>();
    }

    public void put(String key, Object value) {
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }
}

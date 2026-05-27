package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A command argument that resolves an online player by name.
 */
public final class PlayerCommandArgument extends CommandArgument<Player> {

    public PlayerCommandArgument(String name) {
        super(name);
    }

    @Override
    public boolean validate(String input) {
        if (input == null) return false;
        return Bukkit.getPlayer(input.trim()) != null;
    }

    @Override
    public Player parse(String input) {
        return input != null ? Bukkit.getPlayer(input.trim()) : null;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        String prefix = input != null ? input.trim().toLowerCase() : "";
        List<String> results = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (name.toLowerCase().startsWith(prefix)) results.add(name);
        }
        return results;
    }
}

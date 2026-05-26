package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class PlayerCommandArgument extends CommandArgument<Player> {

    public PlayerCommandArgument(String name) {
        super(name);
    }

    @Override
    public boolean validate(String input) {
        return Bukkit.getPlayer(input) != null;
    }

    @Override
    public Player parse(String input) {
        return Bukkit.getPlayer(input);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

}

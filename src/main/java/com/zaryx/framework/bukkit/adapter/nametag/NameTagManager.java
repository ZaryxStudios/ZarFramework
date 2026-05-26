package com.zaryx.framework.bukkit.adapter.nametag;

import com.zaryx.framework.bukkit.adapter.scoreboard.Board;
import com.zaryx.framework.bukkit.adapter.scoreboard.BoardManager;
import com.zaryx.framework.bukkit.utility.Task;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public class NameTagManager {

    private static NameTagManager instance;

    private final BoardManager boardManager;
    private NameTagAdapter adapter;

    private final Map<Player, Map<Player, NameTag>> teams;

    public NameTagManager(BoardManager boardManager, NameTagAdapter adapter) {
        instance = this;
        this.boardManager = boardManager;
        this.teams = new HashMap<>();
        this.adapter = adapter;

        Task.syncTimer(this::update, 20L, 10L);
    }

    public static NameTagManager getInstance() {
        return instance;
    }

    public void update() {
        if (this.adapter == null) return;
        for (Board board : this.boardManager.getBoards().values()) {
            Player viewer = board.getPlayer();
            if (!viewer.isOnline()) continue;

            this.update(viewer);
        }
    }

    private void update(Player viewer) {
        if (this.adapter == null) return;

        Board board = this.boardManager.getBoards().get(viewer);
        if (board == null) return;

        Scoreboard scoreboard = board.getScoreboard();

        this.teams.putIfAbsent(viewer, new HashMap<>());
        for(Player target : viewer.getWorld().getPlayers()) {
            String prefix = this.adapter.getPrefix(viewer, target);
            String suffix = this.adapter.getSuffix(viewer, target);

            String teamName = ("nt_" + target.getName())
                    .substring(0, Math.min(16, target.getName().length() + 3));
            NameTag team = this.teams.get(viewer).computeIfAbsent(target,
                    p -> new NameTag(scoreboard, teamName));

            team.apply(prefix, suffix);
            team.addEntry(target.getName());
        }
    }

    public void remove(Player viewer) {
        Map<Player, NameTag> map = this.teams.remove(viewer);
        if (map == null) return;
        map.values().forEach(NameTag::destroy);
    }
}
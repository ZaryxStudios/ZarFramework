package com.zaryx.framework.bukkit.adapter.scoreboard;

import com.zaryx.framework.bukkit.FrameworkPlugin;
import com.zaryx.framework.bukkit.utility.Task;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public class BoardManager {

    private static BoardManager instance;
    private final Map<Player, Board> boards;
    private BoardAdapter adapter;

    public BoardManager(BoardAdapter adapter) {
        instance = this;

        this.adapter = adapter;
        this.boards = new HashMap<>();

        Task.syncTimer(this::update, 20L, 10L);

        Bukkit.getPluginManager().registerEvents(new BoardHandler(), FrameworkPlugin.getInstance());
    }

    public static BoardManager getInstance() {
        return instance;
    }

    public Map<Player, Board> getBoards() {
        return this.boards;
    }

    public void create(Player player) {
        this.boards.put(player, new Board(player));
    }

    public void remove(Player player) {
        Board board = boards.get(player);
        if (board != null) {
            board.destroy();
            boards.remove(player);
        }
    }

    public void update() {
        if (this.adapter == null) return;

        for (Map.Entry<Player, Board> entry : this.boards.entrySet()) {
            Player player = entry.getKey();
            Board board = entry.getValue();

            if (!player.isOnline()) continue;

            board.update(this.adapter.getTitle(player), this.adapter.getLines(player));
        }
    }
}

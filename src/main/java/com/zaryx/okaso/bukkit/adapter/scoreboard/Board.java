package com.zaryx.okaso.bukkit.adapter.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Board {

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;

    private final Map<Integer, String> lastLine = new HashMap<>();

    public Board(Player player) {
        this.player = player;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager.getNewScoreboard();

        this.objective = this.scoreboard.registerNewObjective("sidebar", "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(this.scoreboard);
    }

    public Player getPlayer() {
        return this.player;
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    public Objective getObjective() {
        return this.objective;
    }

    public void update(String title, List<String> lines) {
        if (!this.objective.getCriteria().equals(title)) {
            this.objective.setDisplayName(title);
        }

        for (int i : new HashSet<>(this.lastLine.keySet())) {
            if (i >= lines.size()) {
                this.scoreboard.resetScores(lastLine.get(i));
                this.lastLine.remove(i);
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (this.lastLine.containsKey(i) && this.lastLine.get(i).equals(line)) continue;
            if (this.lastLine.containsKey(i)) {
                this.scoreboard.resetScores(this.lastLine.get(i));
            }

            Score score = this.objective.getScore(line);
            score.setScore(lines.size() - i);

            this.lastLine.put(i, line);
        }
    }

    public void destroy() {
        this.player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}

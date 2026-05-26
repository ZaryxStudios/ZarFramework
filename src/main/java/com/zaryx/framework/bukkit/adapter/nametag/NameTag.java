package com.zaryx.framework.bukkit.adapter.nametag;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameTag {

    private final Team team;

    public NameTag(Scoreboard scoreboard, String name) {
        Team existing = scoreboard.getTeam(name);
        this.team = existing != null ? existing : scoreboard.registerNewTeam(name);
    }

    public void apply(String prefix, String suffix) {
        if (prefix != null) this.team.setPrefix(prefix);
        if (suffix != null) this.team.setSuffix(suffix);
    }

    public void addEntry(String entry) {
        if (!this.team.hasEntry(entry)) this.team.addEntry(entry);
    }

    public void removeEntry(String entry) {
        this.team.removeEntry(entry);
    }

    public void destroy() {
        team.unregister();
    }
}

package com.whim.starcraft8.domain;

import java.util.ArrayList;
import java.util.List;

/** Aggregate live world state. The engine mutates the lists; the UI reads under lock. */
public final class GameState {
    private final GameMap map;
    private final List<Player> players;
    private final List<Unit> units = new ArrayList<Unit>();
    private final List<Building> buildings = new ArrayList<Building>();
    private final List<Projectile> projectiles = new ArrayList<Projectile>();
    private long tick;
    private int winnerId = -1;

    public GameState(GameMap map, List<Player> players) {
        this.map = map;
        this.players = new ArrayList<Player>(players);
        this.tick = 0L;
    }

    public GameMap map() { return map; }
    public List<Player> players() { return players; }

    public Player player(int id) {
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (p.id() == id) return p;
        }
        return null;
    }

    public List<Unit> units() { return units; }
    public List<Building> buildings() { return buildings; }
    public List<Projectile> projectiles() { return projectiles; }

    public long tick() { return tick; }
    public void setTick(long t) { this.tick = t; }

    public int winnerId() { return winnerId; }
    public void setWinnerId(int id) { this.winnerId = id; }
}

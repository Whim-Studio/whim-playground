package com.whim.kenshi.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The aggregate mutable world: the terrain grid, every {@link Character} (by id
 * in insertion order), {@link Squad}s, the {@link FactionMatrix}, {@link WorldNode}s,
 * plus the simulation clock (tick + world-seconds) and a bounded event log.
 *
 * <p>Pure state container. The engine owns this object on its tick thread,
 * mutates it, and builds immutable Views snapshots from it for the UI.
 */
public final class WorldState {

    /** One immutable event-log entry. */
    public static final class LogEntry {
        private final long tick;
        private final String text;
        public LogEntry(long tick, String text) { this.tick = tick; this.text = text; }
        public long tick() { return tick; }
        public String text() { return text; }
    }

    /** Maximum retained log lines (ring buffer). */
    public static final int LOG_CAPACITY = 200;

    private final MapGrid map;
    private final FactionMatrix factions;

    // insertion-ordered so iteration is stable for the HUD
    private final Map<String, Character> characters = new LinkedHashMap<String, Character>();
    private final Map<String, Squad> squads = new LinkedHashMap<String, Squad>();
    private final Map<String, WorldNode> nodes = new LinkedHashMap<String, WorldNode>();

    private final Deque<LogEntry> log = new ArrayDeque<LogEntry>();

    private long tick = 0L;
    private double worldSeconds = 0.0;

    public WorldState(MapGrid map, FactionMatrix factions) {
        this.map = map;
        this.factions = factions;
    }

    // ---- static-ish structure ------------------------------------------
    public MapGrid map() { return map; }
    public FactionMatrix factions() { return factions; }

    // ---- characters -----------------------------------------------------
    public void addCharacter(Character c) { characters.put(c.id(), c); }
    public Character character(String id) { return characters.get(id); }
    public void removeCharacter(String id) { characters.remove(id); }

    /** Live characters in canonical (insertion) order. */
    public List<Character> charactersList() {
        return new ArrayList<Character>(characters.values());
    }
    public int characterCount() { return characters.size(); }

    // ---- squads ---------------------------------------------------------
    public void addSquad(Squad s) { squads.put(s.id(), s); }
    public Squad squad(String id) { return squads.get(id); }
    public List<Squad> squadsList() { return new ArrayList<Squad>(squads.values()); }

    // ---- nodes ----------------------------------------------------------
    public void addNode(WorldNode n) { nodes.put(n.id(), n); }
    public WorldNode node(String id) { return nodes.get(id); }
    public List<WorldNode> nodesList() { return new ArrayList<WorldNode>(nodes.values()); }

    // ---- clock ----------------------------------------------------------
    public long tick() { return tick; }
    public void setTick(long tick) { this.tick = tick; }
    /** Advance the tick counter by one and return the new value. */
    public long incrementTick() { return ++tick; }

    public double worldSeconds() { return worldSeconds; }
    public void setWorldSeconds(double s) { this.worldSeconds = s; }
    /** Advance the world clock by {@code delta} seconds. */
    public void advanceWorldSeconds(double delta) { this.worldSeconds += delta; }

    // ---- event log ------------------------------------------------------
    /** Append a log line stamped with the current tick (ring-buffered). */
    public void log(String text) {
        log.addLast(new LogEntry(tick, text));
        while (log.size() > LOG_CAPACITY) log.removeFirst();
    }

    /** Log lines oldest → newest (bounded to {@link #LOG_CAPACITY}). */
    public List<LogEntry> logList() {
        return new ArrayList<LogEntry>(log);
    }

    /** The most recent {@code n} log lines, oldest → newest. */
    public List<LogEntry> recentLog(int n) {
        List<LogEntry> all = logList();
        if (all.size() <= n) return all;
        return new ArrayList<LogEntry>(all.subList(all.size() - n, all.size()));
    }
}

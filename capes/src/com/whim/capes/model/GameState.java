package com.whim.capes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The root aggregate holding an entire session: the seated players (in fixed
 * clockwise order), the roster of all characters that exist, the Scene history,
 * the group's Comics Code, and the shared {@link EventLog}. This is the object
 * that Phase 5 will serialize to disk.
 *
 * <p>Turn geography helpers live here because clockwise rotation (next Scene
 * declarer, next Page Starter, Action order) is central to the rules and must
 * be deterministic and testable.
 */
public final class GameState implements java.io.Serializable {
    private final List<Player> players = new ArrayList<Player>();
    private final List<Character> roster = new ArrayList<Character>();
    private final List<Scene> scenes = new ArrayList<Scene>();
    private final List<String> comicsCode = new ArrayList<String>(); // absolute lines that can never happen (p.113)
    private final EventLog eventLog = new EventLog();

    private int lastDeclarerIndex = -1; // seat index of the previous Scene declarer

    public List<Player> players() { return players; }
    public List<Character> roster() { return roster; }
    public List<Scene> scenes() { return scenes; }
    public List<String> comicsCode() { return comicsCode; }
    public EventLog eventLog() { return eventLog; }

    public Player playerById(String id) {
        for (Player p : players) if (p.id().equals(id)) return p;
        return null;
    }

    public Character characterById(String id) {
        for (Character c : roster) if (c.id().equals(id)) return c;
        return null;
    }

    public int seatOf(String playerId) {
        for (int i = 0; i < players.size(); i++) if (players.get(i).id().equals(playerId)) return i;
        return -1;
    }

    /** The player one seat clockwise ("to the left") of the given seat. */
    public Player clockwiseFrom(int seatIndex) {
        if (players.isEmpty()) return null;
        return players.get((seatIndex + 1) % players.size());
    }

    /** Whoever should be offered the next Scene declaration: left of the last declarer (p.20). */
    public Player nextSceneDeclarer() {
        if (players.isEmpty()) return null;
        return players.get((lastDeclarerIndex + 1) % players.size());
    }

    public void recordSceneDeclared(String declaringPlayerId) {
        lastDeclarerIndex = seatOf(declaringPlayerId);
    }

    /**
     * Turn order for a Page: clockwise starting from the Starter (p.22). The
     * Starter for Page N advances one seat each Page; this returns the seat
     * order given a starter seat index.
     */
    public List<Player> turnOrderFrom(int starterSeat) {
        List<Player> order = new ArrayList<Player>();
        for (int i = 0; i < players.size(); i++) {
            order.add(players.get((starterSeat + i) % players.size()));
        }
        return order;
    }

    public Scene currentScene() {
        return scenes.isEmpty() ? null : scenes.get(scenes.size() - 1);
    }
}

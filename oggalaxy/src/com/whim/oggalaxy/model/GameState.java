package com.whim.oggalaxy.model;

import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.GameConfig;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The whole mutable game world and the root of the serializable save graph. Implements
 * {@link Views.GameStateView}; the engine mutates it on the tick thread and publishes it
 * as the current snapshot. All list-returning view methods hand back defensive copies so
 * the EDT can iterate safely while the engine ticks.
 */
public final class GameState implements Views.GameStateView, Serializable {

    private static final long serialVersionUID = 1L;

    public int tick;
    public long masterSeed;
    public Ids.Phase phase = Ids.Phase.RUNNING;
    public String winnerName;

    public Empire player;
    public final List<Empire> empires = new ArrayList<Empire>();     // includes the player
    public final List<FleetMovement> fleets = new ArrayList<FleetMovement>();
    public final List<LogEntry> log = new ArrayList<LogEntry>();
    public final List<CombatReport> combatReports = new ArrayList<CombatReport>();
    public final List<ExpeditionReport> expeditionReports = new ArrayList<ExpeditionReport>();
    public final Map<String, Cost> debrisFields = new HashMap<String, Cost>();

    public String selectedPlanetId;
    public int usedFleetSlots;
    public int maxFleetSlots = 1;

    public GameState() {
    }

    public static String coordKey(int g, int s, int p) {
        return g + ":" + s + ":" + p;
    }

    public Planet planetAt(int g, int s, int p) {
        for (Empire e : empires) {
            for (Planet pl : e.planets) {
                if (!pl.moon && pl.galaxy == g && pl.system == s && pl.position == p) return pl;
            }
        }
        return null;
    }

    public Planet findPlanet(String id) {
        if (id == null) return null;
        for (Empire e : empires) {
            for (Planet pl : e.planets) {
                if (id.equals(pl.id)) return pl;
            }
        }
        return null;
    }

    public Empire ownerOf(Planet pl) {
        for (Empire e : empires) {
            if (e.planets.contains(pl)) return e;
        }
        return null;
    }

    public Empire findEmpire(String id) {
        for (Empire e : empires) if (e.id.equals(id)) return e;
        return null;
    }

    public void addLog(Ids.LogCategory cat, String msg) {
        log.add(new LogEntry(tick, cat, msg));
        while (log.size() > 300) log.remove(0);
    }

    public Cost debrisAt(int g, int s, int p) {
        Cost c = debrisFields.get(coordKey(g, s, p));
        return c == null ? Cost.ZERO : c;
    }

    public void addDebris(int g, int s, int p, Cost extra) {
        String k = coordKey(g, s, p);
        Cost cur = debrisFields.get(k);
        debrisFields.put(k, cur == null ? extra : cur.plus(extra));
    }

    // ------------------------------------------------------------------ views
    @Override public int currentTick() { return tick; }
    @Override public String formattedTime() {
        int days = tick / 24;
        int hours = tick % 24;
        return days > 0 ? ("Day " + days + " " + hours + "h") : ("T+" + hours + "h");
    }
    @Override public Ids.Phase phase() { return phase; }
    @Override public Views.EmpireView player() { return player; }
    @Override public List<Views.EmpireView> empires() {
        return new ArrayList<Views.EmpireView>(empires);
    }
    @Override public List<Views.FleetMovementView> fleets() {
        return new ArrayList<Views.FleetMovementView>(fleets);
    }
    @Override public String selectedPlanetId() { return selectedPlanetId; }
    @Override public Views.PlanetView selectedPlanet() { return findPlanet(selectedPlanetId); }
    @Override public List<Views.LogEntryView> log() {
        return new ArrayList<Views.LogEntryView>(log);
    }
    @Override public List<Views.GalaxyCellView> galaxyRow(int galaxy, int system) {
        List<Views.GalaxyCellView> row = new ArrayList<Views.GalaxyCellView>();
        for (int pos = 1; pos <= GameConfig.POSITIONS_PER_SYSTEM; pos++) {
            row.add(new Cell(galaxy, system, pos));
        }
        return row;
    }
    @Override public List<Views.CombatReportView> combatReports() {
        return new ArrayList<Views.CombatReportView>(combatReports);
    }
    @Override public List<Views.ExpeditionReportView> expeditionReports() {
        return new ArrayList<Views.ExpeditionReportView>(expeditionReports);
    }
    @Override public int usedFleetSlots() { return usedFleetSlots; }
    @Override public int maxFleetSlots() { return maxFleetSlots; }
    @Override public String winnerName() { return winnerName; }

    /** A lazily-computed galaxy cell. Not serialized (built per poll). */
    private final class Cell implements Views.GalaxyCellView {
        private final int g, s, p;
        private final Planet planet;
        private final Empire owner;
        private final Cost debris;

        Cell(int g, int s, int p) {
            this.g = g; this.s = s; this.p = p;
            this.planet = planetAt(g, s, p);
            this.owner = planet == null ? null : ownerOf(planet);
            this.debris = debrisAt(g, s, p);
        }

        @Override public int galaxy() { return g; }
        @Override public int system() { return s; }
        @Override public int position() { return p; }
        @Override public boolean empty() { return planet == null; }
        @Override public String ownerName() { return owner == null ? "" : owner.name; }
        @Override public boolean ownedByPlayer() { return owner != null && owner.player; }
        @Override public boolean isAI() { return owner != null && owner.ai; }
        @Override public String planetName() { return planet == null ? "" : planet.name; }
        @Override public boolean hasMoon() { return planet != null && planet.hasMoon; }
        @Override public boolean hasDebris() { return debris.structurePoints() > 0; }
        @Override public Cost debris() { return debris; }
    }
}

package com.whim.warroom.ui;

import com.whim.warroom.domain.Biome;
import com.whim.warroom.domain.Faction;
import com.whim.warroom.domain.MapMarker;
import com.whim.warroom.domain.MapState;
import com.whim.warroom.domain.Route;
import com.whim.warroom.domain.SandboxState;
import com.whim.warroom.domain.SimEngine;
import com.whim.warroom.domain.SimListener;
import com.whim.warroom.domain.SimSnapshot;
import com.whim.warroom.domain.Stance;
import com.whim.warroom.domain.Unit;
import com.whim.warroom.domain.UnitCatalog;
import com.whim.warroom.domain.UnitType;
import com.whim.warroom.domain.Vec2;
import com.whim.warroom.domain.Waypoint;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Central coordinator. Owns the {@link SandboxState} and {@link SimEngine},
 * holds the editor's current tool selection, and implements {@link SimListener}
 * so engine frames flow in on the background thread and get marshaled onto the
 * EDT for rendering. The UI panels talk to the model exclusively through here;
 * they never touch engine internals or compute a game rule.
 */
public final class SandboxController implements SimListener {

    /** Top-level app mode. */
    public enum Mode { SIMULATION, BATTLE }

    /** Which battlefield mouse tool is active in the editor. */
    public enum Tool { PLACE_UNIT, ROUTE, SELECT, MARKER, PAINT_TERRAIN }

    private final SandboxState state;
    private final SimEngine engine;

    // Panels — wired after construction.
    private BattlefieldPanel field;
    private EditorPanel editor;
    private PlaybackBar bar;
    private WarRoomFrame frame;

    private Mode mode = Mode.SIMULATION;

    // Latest snapshot published by the engine thread (Battle Mode render source).
    private volatile SimSnapshot latest;

    // ---- editor tool state ----
    private Tool tool = Tool.PLACE_UNIT;
    private UnitType brushType;
    private Faction brushFaction = Faction.BLUE;
    private Stance brushStance = Stance.DEFENSIVE;
    private Biome brushBiome = Biome.GRASSLAND;
    private Biome dominantBiome = Biome.GRASSLAND;
    private String markerLabel = "Objective";

    // Selection (unit ids) maintained by the editor.
    private final List<Integer> selection = new ArrayList<Integer>();

    public SandboxController(SandboxState state, SimEngine engine) {
        this.state = state;
        this.engine = engine;
        List<UnitType> all = UnitCatalog.all();
        if (!all.isEmpty()) brushType = all.get(0);
        engine.addListener(this);
        engine.loadScenario(state);
    }

    // ---- wiring ----
    public void attach(WarRoomFrame frame, BattlefieldPanel field, EditorPanel editor, PlaybackBar bar) {
        this.frame = frame;
        this.field = field;
        this.editor = editor;
        this.bar = bar;
    }

    // ---- accessors ----
    public SandboxState getState() { return state; }
    public SimEngine getEngine() { return engine; }
    public Mode getMode() { return mode; }
    public SimSnapshot getLatestSnapshot() { return latest; }
    public BattlefieldPanel getField() { return field; }

    public Tool getTool() { return tool; }
    public void setTool(Tool t) { tool = t; if (field != null) field.repaint(); }
    public UnitType getBrushType() { return brushType; }
    public void setBrushType(UnitType t) { brushType = t; }
    public Faction getBrushFaction() { return brushFaction; }
    public void setBrushFaction(Faction f) { brushFaction = f; }
    public Stance getBrushStance() { return brushStance; }
    public void setBrushStance(Stance s) { brushStance = s; }
    public Biome getBrushBiome() { return brushBiome; }
    public void setBrushBiome(Biome b) { brushBiome = b; }
    public Biome getDominantBiome() { return dominantBiome; }
    public void setDominantBiome(Biome b) { dominantBiome = b; }
    public String getMarkerLabel() { return markerLabel; }
    public void setMarkerLabel(String s) { markerLabel = s; }
    public List<Integer> getSelection() { return selection; }

    // ---- SimListener (engine thread!) ----
    public void onFrame(final SimSnapshot snap) {
        latest = snap;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (field != null) field.repaint();
                if (bar != null) bar.syncFromEngine();
                if (mode == Mode.BATTLE && snap.isFinished() && bar != null) bar.onSimFinished();
            }
        });
    }

    // ================= editor actions (called from BattlefieldPanel/EDT) =================

    /** Place a new unit of the current brush at a world position. Returns its id, or -1. */
    public int placeUnit(double wx, double wy) {
        if (brushType == null) return -1;
        int id = state.nextUnitId();
        Unit u = new Unit(id, brushType, brushFaction, new Vec2(wx, wy));
        u.setStance(brushStance);
        state.addUnit(u);
        return id;
    }

    /** Drop a marker at a world position using the current brush faction color. */
    public void dropMarker(double wx, double wy) {
        Color c = brushFaction.color();
        state.getMarkers().add(new MapMarker(new Vec2(wx, wy), markerLabel, c));
    }

    /** Paint the biome brush onto the tile under a world position. */
    public void paintTerrain(double wx, double wy) {
        MapState map = state.getMap();
        int col = (int) (wx / MapState.TILE_SIZE);
        int row = (int) (wy / MapState.TILE_SIZE);
        if (map.inBounds(col, row)) map.tile(col, row).setBiome(brushBiome);
    }

    /** Regenerate the whole map around the current dominant biome (deterministic per seed). */
    public MapState regenerateMap() {
        MapState old = state.getMap();
        MapState fresh = MapState.generate(old.getCols(), old.getRows(), state.getSeed(), dominantBiome);
        // Copy tiles into the existing map instance so references held elsewhere stay valid.
        for (int c = 0; c < old.getCols(); c++) {
            for (int r = 0; r < old.getRows(); r++) {
                old.tile(c, r).setBiome(fresh.tile(c, r).getBiome());
                old.tile(c, r).setElevation(fresh.tile(c, r).getElevation());
            }
        }
        return old;
    }

    /**
     * Assign a freshly drawn route to a unit. Waypoints carry arrival ticks paced
     * from the unit's own speed, so the engine can honor them.
     */
    public void assignRoute(int unitId, List<Vec2> points) {
        Unit u = state.unit(unitId);
        if (u == null || points.isEmpty()) return;
        Route route = new Route();
        double spd = Math.max(1.0, u.getType().getSpeed());
        int tick = 0;
        Vec2 prev = u.getPos();
        for (int i = 0; i < points.size(); i++) {
            Vec2 p = points.get(i);
            double d = prev.dist(p);
            int dt = (int) Math.ceil(d / spd * SimEngine.TICKS_PER_SECOND);
            tick += Math.max(1, dt);
            route.add(new Waypoint(p, tick));
            prev = p;
        }
        u.setRoute(route);
    }

    /** Nearest living unit to a world point within a pixel-ish radius, or -1. */
    public int pickUnit(double wx, double wy, double worldRadius) {
        int best = -1;
        double bestD = worldRadius;
        for (Unit u : state.getUnits()) {
            double d = u.getPos().dist(new Vec2(wx, wy));
            if (d <= bestD) { bestD = d; best = u.getId(); }
        }
        return best;
    }

    /** Rebuild selection from a world-space box (editor drag-box). */
    public void selectInBox(double x0, double y0, double x1, double y1) {
        double minx = Math.min(x0, x1), maxx = Math.max(x0, x1);
        double miny = Math.min(y0, y1), maxy = Math.max(y0, y1);
        selection.clear();
        for (Unit u : state.getUnits()) {
            Vec2 p = u.getPos();
            if (p.x >= minx && p.x <= maxx && p.y >= miny && p.y <= maxy) selection.add(u.getId());
        }
    }

    public boolean isSelected(int id) { return selection.contains(Integer.valueOf(id)); }
    public void clearSelection() { selection.clear(); }
    public void selectOnly(int id) { selection.clear(); if (id >= 0) selection.add(id); }

    // ================= mode transitions =================

    /** Enter Battle Mode and start playback from the current tick. */
    public void play() {
        if (mode != Mode.BATTLE) {
            mode = Mode.BATTLE;
            engine.loadScenario(state);
            latest = engine.snapshotAt(0);
            if (frame != null) frame.showBattleMode();
        }
        engine.play();
        if (bar != null) bar.syncFromEngine();
    }

    public void pause() {
        engine.pause();
        if (bar != null) bar.syncFromEngine();
    }

    public void seek(int tick) {
        latest = engine.snapshotAt(tick);
        engine.seek(tick);
        if (field != null) field.repaint();
        if (bar != null) bar.syncFromEngine();
    }

    /** Cycle playback speed through the standard multipliers. */
    public double cycleSpeed() {
        double[] speeds = {0.25, 0.5, 1.0, 2.0, 4.0};
        double cur = engine.getSpeed();
        int idx = 0;
        for (int i = 0; i < speeds.length; i++) if (Math.abs(speeds[i] - cur) < 1e-6) idx = i;
        double next = speeds[(idx + 1) % speeds.length];
        engine.setSpeed(next);
        if (bar != null) bar.syncFromEngine();
        return next;
    }

    /** Return to Simulation (editor) mode and reset the engine to tick 0. */
    public void backToEditor() {
        engine.pause();
        engine.reset();
        mode = Mode.SIMULATION;
        latest = null;
        if (frame != null) frame.showSimulationMode();
        if (field != null) field.repaint();
        if (bar != null) bar.syncFromEngine();
    }

    public void shutdown() { engine.shutdown(); }
}

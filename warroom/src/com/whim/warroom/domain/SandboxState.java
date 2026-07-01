package com.whim.warroom.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The whole editable scenario: the terrain map, the placed units and markers,
 * the single shared RNG (seeded for determinism), and id bookkeeping.
 */
public class SandboxState {
    private final MapState map;
    private final long seed;

    private final List<Unit> units = new ArrayList<Unit>();
    private final List<MapMarker> markers = new ArrayList<MapMarker>();

    /** The one shared RNG. All simulation randomness must draw from this. */
    public final Random rng;

    private int nextId = 1;

    public SandboxState(MapState map, long seed) {
        this.map = map;
        this.seed = seed;
        this.rng = new Random(seed);
    }

    public MapState getMap() {
        return map;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public List<MapMarker> getMarkers() {
        return markers;
    }

    public long getSeed() {
        return seed;
    }

    /** Monotonically increasing id source for freshly created units. */
    public int nextUnitId() {
        return nextId++;
    }

    /** The unit with this id, or {@code null} if none. */
    public Unit unit(int id) {
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    public void addUnit(Unit u) {
        units.add(u);
    }

    public void removeUnit(int id) {
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).getId() == id) {
                units.remove(i);
                return;
            }
        }
    }
}

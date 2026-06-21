package com.midnight.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * The world of Midnight: a rectangular grid of {@link Terrain} tiles plus the
 * named {@link Stronghold}s standing on it. Tiles with no recorded terrain read
 * back as {@link Terrain#PLAINS}.
 *
 * <p>{@link #standard()} builds the canonical map — at least 60x40 — with the
 * northern Mountains of Ithorn walling off the top, Doomdark's wastes and the
 * citadel of Ushgarak in the north, and the free lands and Xajorkith in the
 * south.
 */
public class Map {

    private final int width;
    private final int height;
    private final Terrain[][] grid;
    private final List<Stronghold> strongholds;
    private final java.util.Map<Location, Stronghold> strongholdIndex;

    public Map(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Terrain[height][width];
        this.strongholds = new ArrayList<Stronghold>();
        this.strongholdIndex = new HashMap<Location, Stronghold>();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean inBounds(Location loc) {
        return loc.x() >= 0 && loc.x() < width && loc.y() >= 0 && loc.y() < height;
    }

    /** The terrain at {@code loc}; {@link Terrain#PLAINS} for tiles with no data or out of bounds. */
    public Terrain terrainAt(Location loc) {
        if (!inBounds(loc)) {
            return Terrain.PLAINS;
        }
        Terrain t = grid[loc.y()][loc.x()];
        return t == null ? Terrain.PLAINS : t;
    }

    public void setTerrain(Location loc, Terrain t) {
        if (inBounds(loc)) {
            grid[loc.y()][loc.x()] = t;
        }
    }

    /** True when {@code loc} is in bounds and its terrain may be entered. */
    public boolean isPassable(Location loc) {
        return inBounds(loc) && terrainAt(loc).isPassable();
    }

    /** The stronghold standing on {@code loc}, or {@code null} if none. */
    public Stronghold strongholdAt(Location loc) {
        return strongholdIndex.get(loc);
    }

    public List<Stronghold> strongholds() {
        return Collections.unmodifiableList(strongholds);
    }

    /**
     * Register {@code s} on the map and stamp its tile with the stronghold's
     * structure terrain so the landscape reflects it.
     */
    public void addStronghold(Stronghold s) {
        strongholds.add(s);
        strongholdIndex.put(s.location(), s);
        setTerrain(s.location(), s.type());
    }

    // ------------------------------------------------------------------
    // The canonical map
    // ------------------------------------------------------------------

    /** The standard 60x40 map of Midnight, terrain and strongholds laid out. */
    public static Map standard() {
        Map m = new Map(60, 40);

        // The northern Mountains of Ithorn: an impassable wall along the top edge.
        fillRow(m, 0, Terrain.MOUNTAINS);
        // Two broken mountain ranges flanking the gate to Doomdark's lands.
        fillRect(m, 18, 2, 26, 4, Terrain.MOUNTAINS);
        fillRect(m, 34, 2, 41, 4, Terrain.MOUNTAINS);

        // Snowfields and wastes of the far north under the Witchking's chill.
        fillRect(m, 1, 1, 58, 2, Terrain.SNOW);
        fillRect(m, 24, 5, 36, 9, Terrain.WASTELAND);

        // Mid-world forests, downs, and a cold lake.
        fillRect(m, 8, 14, 19, 20, Terrain.FOREST);
        fillRect(m, 40, 16, 51, 23, Terrain.FOREST);
        fillRect(m, 20, 24, 42, 30, Terrain.DOWNS);
        fillRect(m, 45, 9, 49, 12, Terrain.LAKE);

        // A scattering of standing stones and old ruins.
        m.setTerrain(Location.of(12, 26), Terrain.HENGE);
        m.setTerrain(Location.of(50, 31), Terrain.RUINS);
        m.setTerrain(Location.of(33, 20), Terrain.VILLAGE);

        // Strongholds. North (Doomdark): Ushgarak, the Tower of Doom, Kor.
        m.addStronghold(new Stronghold("Ushgarak", Location.of(30, 3), Terrain.CITADEL, Side.DOOMDARK, 1000));
        m.addStronghold(new Stronghold("Tower of Doom", Location.of(40, 1), Terrain.TOWER, Side.DOOMDARK, 500));
        m.addStronghold(new Stronghold("Kor", Location.of(15, 5), Terrain.KEEP, Side.DOOMDARK, 400));

        // South (Free): Xajorkith the Citadel of the Moon, and friendly keeps.
        m.addStronghold(new Stronghold("Xajorkith", Location.of(28, 37), Terrain.CITADEL, Side.FREE, 800));
        m.addStronghold(new Stronghold("Marakith", Location.of(46, 34), Terrain.KEEP, Side.FREE, 400));
        m.addStronghold(new Stronghold("Kumar", Location.of(14, 35), Terrain.CITADEL, Side.FREE, 500));
        m.addStronghold(new Stronghold("Gard", Location.of(33, 31), Terrain.KEEP, Side.FREE, 300));

        return m;
    }

    private static void fillRow(Map m, int y, Terrain t) {
        for (int x = 0; x < m.width; x++) {
            m.setTerrain(Location.of(x, y), t);
        }
    }

    private static void fillRect(Map m, int x0, int y0, int x1, int y1, Terrain t) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                m.setTerrain(Location.of(x, y), t);
            }
        }
    }
}

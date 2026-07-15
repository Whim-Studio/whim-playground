package com.whim.settlers.engine;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingManager;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.economy.Economy;
import com.whim.settlers.economy.Good;
import com.whim.settlers.io.MapLoader;
import com.whim.settlers.map.MapGenerator;
import com.whim.settlers.map.TileMap;
import com.whim.settlers.map.TerrainType;

import java.io.BufferedReader;
import java.io.StringReader;
import java.awt.geom.Point2D;

/**
 * Headless sanity checks for the Phase 0 engine — exercised when {@link
 * com.whim.settlers.app.Main} runs without a display. Verifies the map model,
 * camera coordinate transforms, and the fixed-timestep world update without
 * needing a screen. Not a substitute for a real test framework; it is a smoke
 * test that keeps the container/CI path honest.
 */
public final class SelfTest {

    public static void run() {
        int failures = 0;
        failures += check("map dimensions", mapDimensions());
        failures += check("map default grass", mapDefaultGrass());
        failures += check("camera round-trip", cameraRoundTrip());
        failures += check("zoom-to-cursor keeps anchor", zoomKeepsAnchor());
        failures += check("world clock advances", worldClock());
        failures += check("generator is deterministic", generatorDeterministic());
        failures += check("generator produces varied terrain", generatorVaried());
        failures += check("map loader parses codes", loaderParses());
        failures += check("placement rules", placementRules());
        failures += check("footprint overlap rejected", overlapRejected());
        failures += check("construction completes", constructionCompletes());
        failures += check("found settlement places castle", foundSettlement());
        failures += check("woodcutter->sawmill produces planks", woodToPlanks());
        failures += check("forester replants felled trees", foresterReplants());
        failures += check("unstaffed without tool", staffingNeedsTool());

        if (failures == 0) {
            System.out.println("[settlers] Self-test passed.");
        } else {
            System.out.println("[settlers] Self-test FAILED: " + failures + " check(s).");
            System.exit(1);
        }
    }

    private static boolean mapDimensions() {
        TileMap m = new TileMap(80, 60);
        return m.width() == 80 && m.height() == 60 && m.inBounds(79, 59) && !m.inBounds(80, 0);
    }

    private static boolean mapDefaultGrass() {
        TileMap m = new TileMap(4, 4);
        return m.get(0, 0) == TerrainType.GRASS && m.get(3, 3) == TerrainType.GRASS;
    }

    private static boolean cameraRoundTrip() {
        Camera c = new Camera(40, 40);
        c.setViewport(1024, 720);
        Point2D.Double screen = c.worldToScreen(12.5, 7.25);
        Point2D.Double back = c.screenToWorld(screen.x, screen.y);
        return near(back.x, 12.5) && near(back.y, 7.25);
    }

    private static boolean zoomKeepsAnchor() {
        Camera c = new Camera(40, 40);
        c.setViewport(1024, 720);
        int ax = 300, ay = 500;
        Point2D.Double before = c.screenToWorld(ax, ay);
        c.zoomAt(3, ax, ay);
        Point2D.Double after = c.screenToWorld(ax, ay);
        return near(before.x, after.x) && near(before.y, after.y);
    }

    private static boolean worldClock() {
        World w = new World(new TileMap(20, 20));
        for (int i = 0; i < 60; i++) w.update(1.0 / 60.0);
        return near(w.clock(), 1.0);
    }

    private static boolean generatorDeterministic() {
        TileMap a = MapGenerator.generate(48, 48, 42L);
        TileMap b = MapGenerator.generate(48, 48, 42L);
        for (int y = 0; y < 48; y++) {
            for (int x = 0; x < 48; x++) {
                if (a.get(x, y) != b.get(x, y)) return false;
            }
        }
        // A different seed should differ somewhere.
        TileMap c = MapGenerator.generate(48, 48, 7L);
        for (int y = 0; y < 48; y++) {
            for (int x = 0; x < 48; x++) {
                if (a.get(x, y) != c.get(x, y)) return true;
            }
        }
        return false;
    }

    private static boolean generatorVaried() {
        TileMap m = MapGenerator.generate(64, 64, 1993L);
        boolean water = false, land = false, mountain = false;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                TerrainType t = m.get(x, y);
                if (t.isWater()) water = true;
                else if (t.isMountain()) mountain = true;
                else land = true;
            }
        }
        return water && land && mountain;
    }

    private static boolean loaderParses() {
        String src = "# comment\n.f.\nwci\n";
        try {
            TileMap m = MapLoader.parse(new BufferedReader(new StringReader(src)));
            return m.width() == 3 && m.height() == 2
                && m.get(1, 0) == TerrainType.FOREST
                && m.get(0, 1) == TerrainType.WATER
                && m.get(1, 1) == TerrainType.MOUNTAIN_COAL
                && m.get(2, 1) == TerrainType.MOUNTAIN_IRON;
        } catch (Exception e) {
            return false;
        }
    }

    /** A small all-grass map with one coal mountain and one water tile for rules tests. */
    private static TileMap testMap() {
        TileMap m = new TileMap(10, 10);
        m.set(5, 5, TerrainType.MOUNTAIN_COAL);
        m.set(0, 0, TerrainType.WATER);
        return m;
    }

    private static boolean placementRules() {
        BuildingManager mgr = new BuildingManager(testMap());
        // Woodcutter on grass: ok. On water: no. On mountain: no.
        boolean grassOk   = mgr.canPlace(BuildingType.WOODCUTTER, 2, 2);
        boolean waterBad  = !mgr.canPlace(BuildingType.WOODCUTTER, 0, 0);
        boolean mtnBad    = !mgr.canPlace(BuildingType.WOODCUTTER, 5, 5);
        // Coal mine needs a coal mountain: ok on (5,5), not on grass.
        boolean mineOk    = mgr.canPlace(BuildingType.COAL_MINE, 5, 5);
        boolean mineBad   = !mgr.canPlace(BuildingType.COAL_MINE, 2, 2);
        // Iron mine on a coal mountain must be rejected (resource mismatch).
        boolean mismatch  = !mgr.canPlace(BuildingType.IRON_MINE, 5, 5);
        return grassOk && waterBad && mtnBad && mineOk && mineBad && mismatch;
    }

    private static boolean overlapRejected() {
        BuildingManager mgr = new BuildingManager(testMap());
        Building a = mgr.place(BuildingType.FARM, 2, 2, 0); // 2x2 footprint
        boolean placed = a != null;
        boolean overlap = !mgr.canPlace(BuildingType.WOODCUTTER, 3, 3); // inside the farm
        boolean adjacentOk = mgr.canPlace(BuildingType.WOODCUTTER, 4, 4); // just outside
        return placed && overlap && adjacentOk;
    }

    private static boolean constructionCompletes() {
        BuildingManager mgr = new BuildingManager(testMap());
        Building b = mgr.place(BuildingType.WOODCUTTER, 2, 2, 0);
        if (b == null || b.isFinished()) return false; // starts under construction
        for (int i = 0; i < 600; i++) mgr.update(1f / 60f); // 10s, > 4s build time
        return b.isFinished() && near(b.progress(), 1.0);
    }

    /** Tick construction + economy together for {@code seconds} at 60 Hz. */
    private static void tick(BuildingManager mgr, Economy eco, double seconds) {
        int steps = (int) Math.round(seconds * 60);
        for (int i = 0; i < steps; i++) {
            mgr.update(1f / 60f);
            eco.update(1f / 60f);
        }
    }

    private static boolean woodToPlanks() {
        TileMap m = new TileMap(24, 24);
        for (int y = 0; y < 7; y++) for (int x = 0; x < 7; x++) m.set(x, y, TerrainType.FOREST);
        BuildingManager mgr = new BuildingManager(m);
        Economy eco = new Economy(m, mgr);
        mgr.place(BuildingType.WOODCUTTER, 3, 8, 0); // forest within radius 5
        mgr.place(BuildingType.SAWMILL, 10, 10, 0);
        int planks0 = eco.stock().get(Good.PLANK);
        tick(mgr, eco, 70);
        // The sawmill should have turned harvested wood into new planks.
        return eco.stock().get(Good.PLANK) > planks0
            && eco.stock().get(Good.WOOD) >= 0;
    }

    private static boolean foresterReplants() {
        TileMap m = new TileMap(20, 20); // all grass
        BuildingManager mgr = new BuildingManager(m);
        Economy eco = new Economy(m, mgr);
        mgr.place(BuildingType.FORESTER, 10, 10, 0);
        int forest0 = countTerrain(m, TerrainType.FOREST);
        tick(mgr, eco, 40);
        return countTerrain(m, TerrainType.FOREST) > forest0;
    }

    private static boolean staffingNeedsTool() {
        TileMap m = new TileMap(20, 20);
        for (int y = 0; y < 6; y++) for (int x = 0; x < 6; x++) m.set(x, y, TerrainType.FOREST);
        BuildingManager mgr = new BuildingManager(m);
        Economy eco = new Economy(m, mgr); // seeds exactly one AXE
        Building a = mgr.place(BuildingType.WOODCUTTER, 2, 8, 0);
        Building b = mgr.place(BuildingType.WOODCUTTER, 8, 8, 0);
        tick(mgr, eco, 20);
        int staffed = (eco.isStaffed(a) ? 1 : 0) + (eco.isStaffed(b) ? 1 : 0);
        boolean oneWaiting = eco.statusOf(a).contains("axe") || eco.statusOf(b).contains("axe");
        return staffed == 1 && oneWaiting;
    }

    private static int countTerrain(TileMap m, TerrainType t) {
        int n = 0;
        for (int y = 0; y < m.height(); y++)
            for (int x = 0; x < m.width(); x++)
                if (m.get(x, y) == t) n++;
        return n;
    }

    private static boolean foundSettlement() {
        World w = new World(MapGenerator.generate(40, 40, 5L));
        boolean ok = w.foundSettlement();
        return ok && w.buildings().count() == 1
            && w.buildings().all().get(0).type() == BuildingType.CASTLE
            && w.buildings().all().get(0).isFinished();
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    private static int check(String name, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + name);
        return ok ? 0 : 1;
    }

    private SelfTest() { }
}

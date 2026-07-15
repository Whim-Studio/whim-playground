package com.whim.settlers.engine;

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

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    private static int check(String name, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + name);
        return ok ? 0 : 1;
    }

    private SelfTest() { }
}

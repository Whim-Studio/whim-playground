package com.whim.settlers.engine;

import com.whim.settlers.map.TileMap;
import com.whim.settlers.map.TerrainType;

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

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    private static int check(String name, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + name);
        return ok ? 0 : 1;
    }

    private SelfTest() { }
}

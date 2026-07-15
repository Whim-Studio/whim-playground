package com.whim.settlers.engine;

import com.whim.settlers.map.TileMap;
import com.whim.settlers.map.TerrainType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

/**
 * Draws the world with Java2D. Culls to the visible tile range so cost scales
 * with the viewport, not the map — important once maps reach 100×100.
 */
public final class Renderer {

    private static final Color GRID = new Color(0, 0, 0, 40);
    private static final Color BG   = new Color(0x101418);

    public void render(Graphics2D g, World world, InputHandler input, double fps) {
        Camera cam = world.camera();
        TileMap map = world.map();
        int vw = cam.viewportW();
        int vh = cam.viewportH();

        g.setColor(BG);
        g.fillRect(0, 0, vw, vh);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double s = cam.scale();

        // Visible tile window (with a one-tile margin so edges aren't clipped).
        Point2D.Double topLeft = cam.screenToWorld(0, 0);
        Point2D.Double botRight = cam.screenToWorld(vw, vh);
        int minX = Math.max(0, (int) Math.floor(topLeft.x) - 1);
        int minY = Math.max(0, (int) Math.floor(topLeft.y) - 1);
        int maxX = Math.min(map.width(),  (int) Math.ceil(botRight.x) + 1);
        int maxY = Math.min(map.height(), (int) Math.ceil(botRight.y) + 1);

        int tilePx = (int) Math.ceil(s) + 1;
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                TerrainType t = map.get(x, y);
                Point2D.Double p = cam.worldToScreen(x, y);
                g.setColor(t.color());
                g.fillRect((int) Math.floor(p.x), (int) Math.floor(p.y), tilePx, tilePx);
            }
        }

        // Grid lines, only when zoomed in enough to be legible.
        if (s >= 12) {
            g.setColor(GRID);
            g.setStroke(new BasicStroke(1f));
            for (int x = minX; x <= maxX; x++) {
                Point2D.Double a = cam.worldToScreen(x, minY);
                Point2D.Double b = cam.worldToScreen(x, maxY);
                g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
            }
            for (int y = minY; y <= maxY; y++) {
                Point2D.Double a = cam.worldToScreen(minX, y);
                Point2D.Double b = cam.worldToScreen(maxX, y);
                g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
            }
        }

        drawHud(g, world, fps, minX, minY, maxX, maxY);
    }

    private void drawHud(Graphics2D g, World world, double fps,
                         int minX, int minY, int maxX, int maxY) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(8, 8, 250, 74);
        g.setColor(Color.WHITE);
        g.drawString("The Settlers — Phase 0 scaffold", 18, 26);
        g.drawString(String.format("FPS %.0f   zoom %.2f", fps, world.camera().zoom()), 18, 44);
        g.drawString(String.format("visible tiles %d..%d x %d..%d", minX, maxX, minY, maxY), 18, 62);
        g.setColor(new Color(200, 200, 200));
        g.drawString("WASD/arrows pan · wheel zoom · right-drag pan", 18, 78);
    }
}

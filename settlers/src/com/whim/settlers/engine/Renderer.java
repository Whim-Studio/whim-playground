package com.whim.settlers.engine;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingState;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.map.TileMap;
import com.whim.settlers.map.TerrainType;
import com.whim.settlers.ui.BuildMenu;
import com.whim.settlers.ui.Minimap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Draws the world with Java2D. Culls to the visible tile range so cost scales
 * with the viewport, not the map — important once maps reach 100×100.
 */
public final class Renderer {

    private static final Color GRID = new Color(0, 0, 0, 40);
    private static final Color BG   = new Color(0x101418);

    private final Minimap minimap;
    private final BuildMenu buildMenu;

    public Renderer(Minimap minimap, BuildMenu buildMenu) {
        this.minimap = minimap;
        this.buildMenu = buildMenu;
    }

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

        drawBuildings(g, world, s);
        drawGhost(g, world, input, s);
        minimap.render(g, world);
        buildMenu.render(g, input.selectedType(), vh);
        drawHud(g, world, fps, minX, minY, maxX, maxY);
    }

    private void drawBuildings(Graphics2D g, World world, double s) {
        List<Building> list = world.buildings().all();
        for (int i = 0; i < list.size(); i++) {
            Building b = list.get(i);
            drawBuilding(g, world, b.type(), b.x(), b.y(), s,
                         b.state() == BuildingState.FINISHED ? 255 : 150);
            if (b.state() == BuildingState.UNDER_CONSTRUCTION) {
                drawProgress(g, world, b, s);
            }
        }
    }

    /** Draw one building's footprint block plus its glyph. */
    private void drawBuilding(Graphics2D g, World world, BuildingType type,
                              int tx, int ty, double s, int alpha) {
        Point2D.Double p = world.camera().worldToScreen(tx, ty);
        int w = (int) Math.ceil(type.footprintW() * s);
        int h = (int) Math.ceil(type.footprintH() * s);
        int x = (int) Math.floor(p.x), y = (int) Math.floor(p.y);
        Color c = type.color();
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
        g.fillRoundRect(x + 2, y + 2, Math.max(4, w - 4), Math.max(4, h - 4), 6, 6);
        g.setColor(new Color(0, 0, 0, Math.min(alpha, 180)));
        g.drawRoundRect(x + 2, y + 2, Math.max(4, w - 4), Math.max(4, h - 4), 6, 6);
        if (s >= 16) {
            g.setColor(new Color(20, 20, 20, alpha));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, (int) (s * 0.5))));
            g.drawString(String.valueOf(type.glyph()),
                         x + w / 2 - 4, y + h / 2 + 5);
        }
    }

    private void drawProgress(Graphics2D g, World world, Building b, double s) {
        Point2D.Double p = world.camera().worldToScreen(b.x(), b.y());
        int w = (int) Math.ceil(b.type().footprintW() * s);
        int barW = Math.max(6, w - 8);
        int x = (int) p.x + 4, y = (int) p.y + 3;
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(x, y, barW, 4);
        g.setColor(new Color(0x6FD07A));
        g.fillRect(x, y, (int) (barW * b.progress()), 4);
    }

    /** Placement ghost: green when the armed building can be placed here, red if not. */
    private void drawGhost(Graphics2D g, World world, InputHandler input, double s) {
        BuildingType type = input.selectedType();
        if (type == null) return;
        Point tile = input.hoveredTile();
        if (buildMenu.contains(input.mouseX(), input.mouseY(), world.camera().viewportH())) {
            return; // don't ghost while the cursor is over the menu
        }
        boolean ok = world.buildings().canPlace(type, tile.x, tile.y);
        Point2D.Double p = world.camera().worldToScreen(tile.x, tile.y);
        int w = (int) Math.ceil(type.footprintW() * s);
        int h = (int) Math.ceil(type.footprintH() * s);
        Color tint = ok ? new Color(80, 220, 100, 110) : new Color(220, 70, 70, 110);
        g.setColor(tint);
        g.fillRect((int) p.x, (int) p.y, w, h);
        g.setColor(ok ? new Color(120, 255, 140) : new Color(255, 120, 120));
        g.drawRect((int) p.x, (int) p.y, w, h);
    }

    private void drawHud(Graphics2D g, World world, double fps,
                         int minX, int minY, int maxX, int maxY) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(BuildMenu.WIDTH + 8, 8, 300, 60);
        int hx = BuildMenu.WIDTH + 18;
        g.setColor(Color.WHITE);
        g.drawString("The Settlers — Phase 2 (buildings)", hx, 26);
        g.drawString(String.format("FPS %.0f   zoom %.2f   buildings %d",
                fps, world.camera().zoom(), world.buildings().count()), hx, 44);
        g.setColor(new Color(200, 200, 200));
        g.drawString("Pick a building at left · left-click to place · right-click cancels", hx, 62);
    }
}

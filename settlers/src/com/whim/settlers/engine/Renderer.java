package com.whim.settlers.engine;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingState;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.map.TileMap;
import com.whim.settlers.map.TerrainType;
import com.whim.settlers.transport.Flag;
import com.whim.settlers.transport.Road;
import com.whim.settlers.ui.BuildMenu;
import com.whim.settlers.ui.EconomyPanel;
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
    private final EconomyPanel economyPanel;

    public Renderer(Minimap minimap, BuildMenu buildMenu, EconomyPanel economyPanel) {
        this.minimap = minimap;
        this.buildMenu = buildMenu;
        this.economyPanel = economyPanel;
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

        drawRoadsAndFlags(g, world, input, s);
        drawBuildings(g, world, s);
        drawCarriers(g, world, s);
        drawGhost(g, world, input, s);
        minimap.render(g, world);
        buildMenu.render(g, input.selectedType(), vh);
        economyPanel.render(g, world.economy(), vw, vh);
        drawHud(g, world, input, fps);
    }

    /** Roads (paths), flags (posts), and the road-tool start highlight. */
    private void drawRoadsAndFlags(Graphics2D g, World world, InputHandler input, double s) {
        Camera cam = world.camera();
        g.setStroke(new BasicStroke((float) Math.max(2.0, s * 0.18),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x6B4E2E));
        for (Road r : world.transport().network().roads()) {
            List<int[]> path = r.path();
            for (int i = 0; i + 1 < path.size(); i++) {
                Point2D.Double a = cam.worldToScreen(path.get(i)[0] + 0.5, path.get(i)[1] + 0.5);
                Point2D.Double b = cam.worldToScreen(path.get(i + 1)[0] + 0.5, path.get(i + 1)[1] + 0.5);
                g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
            }
        }
        int post = (int) Math.max(3, s * 0.28);
        for (Flag f : world.transport().network().flags()) {
            Point2D.Double p = cam.worldToScreen(f.x() + 0.5, f.y() + 0.5);
            boolean start = f.id() == input.roadStartFlag();
            g.setColor(start ? new Color(0xFFD84D) : new Color(0xE8E0C0));
            g.fillOval((int) p.x - post / 2, (int) p.y - post, post, post);
            g.setColor(new Color(0x5A4A2A));
            g.drawOval((int) p.x - post / 2, (int) p.y - post, post, post);
            if (f.hasWaiting()) { // congestion indicator
                g.setColor(new Color(0xE05050));
                g.drawString(String.valueOf(f.waiting()), (int) p.x + post, (int) p.y);
            }
        }
    }

    /** Carriers relaying goods, drawn as dots moving along their road. */
    private void drawCarriers(Graphics2D g, World world, double s) {
        Camera cam = world.camera();
        int r = (int) Math.max(3, s * 0.22);
        for (Road road : world.transport().network().roads()) {
            double[] pos = road.carrierPos();
            if (pos == null) continue;
            Point2D.Double p = cam.worldToScreen(pos[0] + 0.5, pos[1] + 0.5);
            g.setColor(road.loaded() ? new Color(0xFFF2B0) : new Color(0x9A8C6A));
            g.fillOval((int) p.x - r / 2, (int) p.y - r / 2, r, r);
            g.setColor(new Color(0, 0, 0, 150));
            g.drawOval((int) p.x - r / 2, (int) p.y - r / 2, r, r);
        }
    }

    private void drawBuildings(Graphics2D g, World world, double s) {
        List<Building> list = world.buildings().all();
        for (int i = 0; i < list.size(); i++) {
            Building b = list.get(i);
            drawBuilding(g, world, b.type(), b.x(), b.y(), s,
                         b.state() == BuildingState.FINISHED ? 255 : 150);
            if (b.state() == BuildingState.UNDER_CONSTRUCTION) {
                drawProgress(g, world, b, s);
            } else if (s >= 18) {
                drawStatus(g, world, b, s);
            }
        }
    }

    /** Small status caption under a finished building (staffed / stall reason). */
    private void drawStatus(Graphics2D g, World world, Building b, double s) {
        String status = world.economy().statusOf(b);
        if (status == null || status.isEmpty()) return;
        Point2D.Double p = world.camera().worldToScreen(b.x(), b.y());
        int h = (int) Math.ceil(b.type().footprintH() * s);
        boolean ok = world.economy().isStaffed(b) && "working".equals(status);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        int tx = (int) p.x + 2, ty = (int) p.y + h + 11;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(tx - 1, ty - 9, g.getFontMetrics().stringWidth(status) + 4, 12);
        g.setColor(ok ? new Color(150, 230, 150) : new Color(240, 200, 120));
        g.drawString(status, tx + 1, ty);
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
        int vw = world.camera().viewportW(), vh = world.camera().viewportH();
        if (buildMenu.contains(input.mouseX(), input.mouseY(), vh)
                || economyPanel.contains(input.mouseX(), input.mouseY(), vw, vh)) {
            return; // don't ghost while the cursor is over a panel
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

    private void drawHud(Graphics2D g, World world, InputHandler input, double fps) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(BuildMenu.WIDTH + 8, 8, 340, 60);
        int hx = BuildMenu.WIDTH + 18;
        g.setColor(Color.WHITE);
        String mode;
        if (input.tool() == InputHandler.Tool.FLAG) mode = "FLAG tool — click land to place a flag";
        else if (input.tool() == InputHandler.Tool.ROAD) mode = input.roadStartFlag() < 0
                ? "ROAD tool — click the first flag" : "ROAD tool — click the second flag";
        else if (input.selectedType() != null) mode = "Placing: " + input.selectedType().displayName();
        else mode = "F flag · R road · E economy · build menu at left";
        g.drawString("The Settlers — Phase 4 (transport)", hx, 26);
        g.drawString(String.format("FPS %.0f   zoom %.2f   buildings %d   settlers %d",
                fps, world.camera().zoom(), world.buildings().count(),
                world.economy().totalPopulation()), hx, 44);
        g.setColor(new Color(200, 220, 255));
        g.drawString(mode, hx, 62);
    }
}

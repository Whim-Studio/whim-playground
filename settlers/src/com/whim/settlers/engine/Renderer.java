package com.whim.settlers.engine;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingState;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.map.TileMap;
import com.whim.settlers.map.TerrainType;
import com.whim.settlers.military.MilitarySystem;
import com.whim.settlers.military.Players;
import com.whim.settlers.transport.Flag;
import com.whim.settlers.transport.Road;
import com.whim.settlers.economy.Economy;
import com.whim.settlers.economy.ProductionChains;
import com.whim.settlers.economy.Recipe;
import com.whim.settlers.ui.BuildMenu;
import com.whim.settlers.ui.EconomyPanel;
import com.whim.settlers.ui.MetaScreen;
import com.whim.settlers.ui.MilitaryPanel;
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
    private final MilitaryPanel militaryPanel;
    private final MetaScreen meta;

    public Renderer(MetaScreen meta, Minimap minimap, BuildMenu buildMenu,
                    EconomyPanel economyPanel, MilitaryPanel militaryPanel) {
        this.meta = meta;
        this.minimap = minimap;
        this.buildMenu = buildMenu;
        this.economyPanel = economyPanel;
        this.militaryPanel = militaryPanel;
    }

    /** Top-level render, dispatched by {@link Game.State}. */
    public void render(Graphics2D g, Game game, InputHandler input, double fps, int vw, int vh) {
        Game.State st = game.state();
        if (st == Game.State.MENU || st == Game.State.SETUP) {
            meta.render(g, game, vw, vh);
            return;
        }
        // All remaining states have a live world board.
        renderWorld(g, game, input, fps, st);
        if (st == Game.State.VICTORY || st == Game.State.DEFEAT) {
            meta.render(g, game, vw, vh);
        }
    }

    /** Draw the play board plus the in-game HUD/overlays for the given state. */
    private void renderWorld(Graphics2D g, Game game, InputHandler input, double fps, Game.State st) {
        World world = game.world();
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

        drawTerritory(g, world, minX, minY, maxX, maxY);
        drawRoadsAndFlags(g, world, input, s);
        drawBuildings(g, world, s);
        drawCarriers(g, world, s);

        boolean playing = st == Game.State.PLAYING;
        if (playing) drawGhost(g, world, input, s);
        if (st == Game.State.PLACING_CASTLE) drawFoundingGhost(g, world, input, s);

        // HUD panels only while actively playing or paused.
        if (playing || st == Game.State.PAUSED) {
            minimap.render(g, world);
            buildMenu.render(g, input.selectedType(), vh);
            economyPanel.render(g, world.economy(), vw, vh);
            militaryPanel.render(g, world, vw, vh);
            drawHud(g, world, input, fps);
        }

        if (playing) drawTooltip(g, world, input, s);

        // Founding banner.
        if (st == Game.State.PLACING_CASTLE) {
            banner(g, vw, "Left-click a green tile to found your Castle   ·   Esc to menu");
        }

        game.notifications().render(g, vw / 2, 12);
        if (game.helpVisible()) drawHelp(g, vw, vh);
        if (st == Game.State.PAUSED) drawPauseOverlay(g, vw, vh);
    }

    /** Founding ghost: the Castle footprint tinted by validity at the cursor. */
    private void drawFoundingGhost(Graphics2D g, World world, InputHandler input, double s) {
        Point tile = input.hoveredTile();
        boolean ok = world.canFoundAt(tile.x, tile.y);
        Point2D.Double p = world.camera().worldToScreen(tile.x, tile.y);
        int w = (int) Math.ceil(BuildingType.CASTLE.footprintW() * s);
        int h = (int) Math.ceil(BuildingType.CASTLE.footprintH() * s);
        g.setColor(ok ? new Color(80, 220, 100, 120) : new Color(220, 70, 70, 120));
        g.fillRect((int) p.x, (int) p.y, w, h);
        g.setColor(ok ? new Color(120, 255, 140) : new Color(255, 120, 120));
        g.setStroke(new BasicStroke(2f));
        g.drawRect((int) p.x, (int) p.y, w, h);
    }

    private void banner(Graphics2D g, int vw, String text) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        int w = g.getFontMetrics().stringWidth(text) + 28;
        int x = vw / 2 - w / 2, y = 44;
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(x, y, w, 28, 10, 10);
        g.setColor(new Color(120, 230, 140));
        g.drawRoundRect(x, y, w, 28, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(text, x + 14, y + 19);
    }

    /** Territory tint per owner plus border outlines between differing owners. */
    private void drawTerritory(Graphics2D g, World world, int minX, int minY, int maxX, int maxY) {
        MilitarySystem mil = world.military();
        Camera cam = world.camera();
        double s = cam.scale();
        int tile = (int) Math.ceil(s) + 1;
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                int owner = mil.ownerAt(x, y);
                if (owner < 0) continue;
                Point2D.Double p = cam.worldToScreen(x, y);
                Color c = Players.color(owner);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 46));
                g.fillRect((int) Math.floor(p.x), (int) Math.floor(p.y), tile, tile);
                // Border where the owner differs from the right/bottom neighbour.
                boolean edge = mil.ownerAt(x + 1, y) != owner || mil.ownerAt(x, y + 1) != owner
                            || mil.ownerAt(x - 1, y) != owner || mil.ownerAt(x, y - 1) != owner;
                if (edge) {
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
                    g.drawRect((int) Math.floor(p.x), (int) Math.floor(p.y),
                               (int) Math.ceil(s), (int) Math.ceil(s));
                }
            }
        }
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
            for (double[] pos : road.carrierPositions()) {
                Point2D.Double p = cam.worldToScreen(pos[0] + 0.5, pos[1] + 0.5);
                boolean loaded = pos[2] > 0.5;
                g.setColor(loaded ? new Color(0xFFF2B0) : new Color(0x9A8C6A));
                g.fillOval((int) p.x - r / 2, (int) p.y - r / 2, r, r);
                g.setColor(new Color(0, 0, 0, 150));
                g.drawOval((int) p.x - r / 2, (int) p.y - r / 2, r, r);
            }
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
            } else if (MilitarySystem.isFort(b.type())) {
                drawGarrison(g, world, b, s);
            } else if (s >= 18) {
                drawStatus(g, world, b, s);
            }
        }
    }

    /** Owner-tinted border + "knights/cap" label on forts. */
    private void drawGarrison(Graphics2D g, World world, Building b, double s) {
        Point2D.Double p = world.camera().worldToScreen(b.x(), b.y());
        int w = (int) Math.ceil(b.type().footprintW() * s);
        int h = (int) Math.ceil(b.type().footprintH() * s);
        Color oc = b.ownerId() < 0 ? new Color(150, 150, 150) : Players.color(b.ownerId());
        g.setColor(oc);
        g.setStroke(new java.awt.BasicStroke(2f));
        g.drawRect((int) p.x + 1, (int) p.y + 1, w - 2, h - 2);
        if (s >= 14) {
            String label = world.military().garrisonSize(b) + "/"
                    + MilitarySystem.capacity(b.type());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            int tx = (int) p.x + 2, ty = (int) p.y + h + 11;
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(tx - 1, ty - 9, g.getFontMetrics().stringWidth(label) + 4, 12);
            g.setColor(Color.WHITE);
            g.drawString(label, tx + 1, ty);
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
        boolean ok = world.canPlayerPlace(type, tile.x, tile.y, World.PLAYER_ID);
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
        else mode = "F flag · R road · E economy · H help · P pause";
        g.drawString("The Settlers — Phase 8", hx, 26);
        g.drawString(String.format("FPS %.0f   zoom %.2f   buildings %d   settlers %d   knights %d",
                fps, world.camera().zoom(), world.buildings().count(),
                world.economy().totalPopulation(), world.military().knightCount(World.PLAYER_ID)), hx, 44);
        g.setColor(new Color(200, 220, 255));
        g.drawString(mode, hx, 62);
    }

    /** Building info tooltip near the cursor: name, owner, status, staffing, buffers, yield. */
    private void drawTooltip(Graphics2D g, World world, InputHandler input, double s) {
        int mx = input.mouseX(), my = input.mouseY();
        int vw = world.camera().viewportW(), vh = world.camera().viewportH();
        if (buildMenu.contains(mx, my, vh) || economyPanel.contains(mx, my, vw, vh)
                || militaryPanel.contains(mx, my, vw, vh)) return;
        if (input.selectedType() != null || input.tool() != InputHandler.Tool.NONE) return;
        Point tile = input.hoveredTile();
        Building b = world.buildings().at(tile.x, tile.y);
        if (b == null) return;

        java.util.List<String> lines = new java.util.ArrayList<String>();
        lines.add(b.type().displayName());
        lines.add("Owner: " + com.whim.settlers.military.Players.name(b.ownerId()));
        if (!b.isFinished()) {
            lines.add(String.format("Under construction  %.0f%%", b.progress() * 100));
        } else if (MilitarySystem.isFort(b.type())) {
            lines.add("Garrison: " + world.military().garrisonSize(b) + "/"
                    + MilitarySystem.capacity(b.type()));
            lines.add(String.format("Morale: %.2f", world.military().morale(b)));
        }
        Economy eco = world.economyOf(b.ownerId());
        Recipe r = ProductionChains.of(b.type());
        if (eco != null && r != null && b.isFinished()) {
            lines.add("Status: " + eco.statusOf(b));
            lines.add(eco.isStaffed(b) ? "Staffed" : "Unstaffed");
            int yield = eco.remainingYield(b);
            if (yield >= 0) lines.add("Deposit left: " + yield);
        }

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        int w = 0;
        for (int i = 0; i < lines.size(); i++) w = Math.max(w, g.getFontMetrics().stringWidth(lines.get(i)));
        w += 16;
        int h = 6 + lines.size() * 16 + 4;
        int tx = Math.min(mx + 16, vw - w - 6);
        int ty = Math.min(my + 12, vh - h - 6);
        g.setColor(new Color(0, 0, 0, 205));
        g.fillRoundRect(tx, ty, w, h, 8, 8);
        g.setColor(new Color(255, 255, 255, 50));
        g.drawRoundRect(tx, ty, w, h, 8, 8);
        int yy = ty + 18;
        for (int i = 0; i < lines.size(); i++) {
            g.setColor(i == 0 ? new Color(255, 230, 160) : new Color(220, 225, 230));
            g.drawString(lines.get(i), tx + 8, yy);
            yy += 16;
        }
    }

    private void drawPauseOverlay(Graphics2D g, int vw, int vh) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, 0, vw, vh);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        String t = "PAUSED";
        g.drawString(t, vw / 2 - g.getFontMetrics().stringWidth(t) / 2, vh / 2 - 20);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        String h = "Esc / P resume   ·   M main menu   ·   H help";
        g.setColor(new Color(210, 220, 230));
        g.drawString(h, vw / 2 - g.getFontMetrics().stringWidth(h) / 2, vh / 2 + 14);
    }

    private static final String[] HELP_LINES = {
        "CONTROLS",
        "WASD / arrows / right-drag — pan     Mouse wheel — zoom",
        "Left-click build menu, then map — place a building (green ghost = valid)",
        "F — flag tool     R — road tool (click two flags)     Right-click — cancel",
        "E — economy panel     Hover a building — info tooltip",
        "Click an enemy fort — attack panel (choose knights, Attack)",
        "P / Esc — pause     M — main menu     H or ? — toggle this help",
        "",
        "Goal: eliminate every rival to control the whole map.",
    };

    private void drawHelp(Graphics2D g, int vw, int vh) {
        int w = 560, h = 40 + HELP_LINES.length * 22;
        int x = vw / 2 - w / 2, y = vh / 2 - h / 2;
        g.setColor(new Color(0x14, 0x1c, 0x24, 245));
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(new Color(0x6F, 0xB0, 0xE0));
        g.drawRoundRect(x, y, w, h, 14, 14);
        int yy = y + 30;
        for (int i = 0; i < HELP_LINES.length; i++) {
            if (i == 0) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                g.setColor(Color.WHITE);
            } else {
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                g.setColor(new Color(210, 218, 226));
            }
            g.drawString(HELP_LINES[i], x + 20, yy);
            yy += i == 0 ? 28 : 22;
        }
    }
}

package com.midnight.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import com.midnight.core.Character;
import com.midnight.core.Direction;
import com.midnight.core.GameState;
import com.midnight.core.Location;
import com.midnight.core.Map;
import com.midnight.core.Side;
import com.midnight.core.Stronghold;
import com.midnight.core.Terrain;

/**
 * The "Landscaping" first-person viewpoint, painted entirely with
 * {@link Graphics2D}. It looks outward from {@code state.selected().location()}
 * along the lord's {@code facing()} and stacks three depth layers:
 *
 * <ul>
 *   <li><b>Distant</b> &mdash; a mountain/snow horizon silhouette.</li>
 *   <li><b>Mid-ground</b> &mdash; forests, henges, citadels a few tiles out.</li>
 *   <li><b>Foreground</b> &mdash; armies and lords on the adjacent tile ahead.</li>
 * </ul>
 *
 * <p>Pure view: it reads the core map and characters and never mutates state.
 * The frame calls {@link #setState(GameState)} after every look/move to
 * re-render.
 */
final class LandscapePanel extends JPanel {

    /** How many tiles ahead we sample, nearest (1) to farthest. */
    private static final int DEPTH = 5;
    /** Lateral spread sampled either side of the centre line. */
    private static final int SPREAD = 2;

    private GameState state;

    LandscapePanel(GameState state) {
        this.state = state;
        setPreferredSize(new Dimension(640, 460));
        setBackground(new Color(0x10131A));
    }

    void setState(GameState state) {
        this.state = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        boolean night = !state.isDay();

        paintSkyAndGround(g2, w, h, night);

        Character me = state.selected();
        if (me == null || !me.isAlive()) {
            drawCentredText(g2, w, h, "No lord selected.");
            g2.dispose();
            return;
        }

        Map map = state.map();
        Direction facing = me.facing();
        Location base = me.location();
        // "right" is a 90-degree clockwise turn from the facing.
        Direction right = facing.clockwise().clockwise();

        int horizon = (int) (h * 0.34);

        // --- Distant layer: horizon silhouette of far high ground. ---------
        paintHorizon(g2, w, horizon, map, base, facing, right, night);

        // --- Mid + foreground: paint far depths first so near ones overlap. -
        for (int d = DEPTH; d >= 1; d--) {
            for (int col = -SPREAD; col <= SPREAD; col++) {
                Location loc = offset(base, facing, right, d, col);
                paintTile(g2, w, h, horizon, map, loc, d, col, night);
            }
        }

        // --- Foreground occupants on the tile directly ahead. --------------
        Location ahead = offset(base, facing, right, 1, 0);
        paintForegroundOccupants(g2, w, h, ahead, night);

        paintCompassRose(g2, w, facing);
        if (night) {
            paintNightVeil(g2, w, h);
        }
        g2.dispose();
    }

    // ----- layers ------------------------------------------------------------

    private void paintSkyAndGround(Graphics2D g2, int w, int h, boolean night) {
        int horizon = (int) (h * 0.34);
        Color skyTop = night ? new Color(0x0B1026) : new Color(0x7FB3E0);
        Color skyBottom = night ? new Color(0x222a45) : new Color(0xCFE6F5);
        g2.setPaint(new GradientPaint(0, 0, skyTop, 0, horizon, skyBottom));
        g2.fillRect(0, 0, w, horizon);

        Color gNear = night ? new Color(0x1B2418) : new Color(0x4F7038);
        Color gFar = night ? new Color(0x10160E) : new Color(0x6E9650);
        g2.setPaint(new GradientPaint(0, horizon, gFar, 0, h, gNear));
        g2.fillRect(0, horizon, w, h - horizon);
    }

    private void paintHorizon(Graphics2D g2, int w, int horizon, Map map, Location base,
                              Direction facing, Direction right, boolean night) {
        // Sample one row far ahead; raise the silhouette where high ground sits.
        Color hi = night ? new Color(0x39406A) : new Color(0xA9B6C6);
        g2.setColor(hi);
        int far = DEPTH + 2;
        int slices = (SPREAD * 2) + 1;
        int sliceW = w / slices;
        for (int i = 0; i < slices; i++) {
            int col = i - SPREAD;
            Location loc = offset(base, facing, right, far, col);
            Terrain t = map.inBounds(loc) ? map.terrainAt(loc) : Terrain.SNOW;
            int peak;
            if (t == Terrain.MOUNTAINS) {
                peak = (int) (horizon * 0.55);
            } else if (t == Terrain.SNOW) {
                peak = (int) (horizon * 0.72);
            } else {
                peak = (int) (horizon * 0.92);
            }
            int x = i * sliceW;
            Polygon p = new Polygon();
            p.addPoint(x, horizon);
            p.addPoint(x + sliceW / 2, peak);
            p.addPoint(x + sliceW, horizon);
            g2.fillPolygon(p);
            if (t == Terrain.SNOW) {
                g2.setColor(night ? new Color(0x5b6390) : Color.WHITE);
                Polygon cap = new Polygon();
                cap.addPoint(x + sliceW / 2 - 6, peak + 14);
                cap.addPoint(x + sliceW / 2, peak);
                cap.addPoint(x + sliceW / 2 + 6, peak + 14);
                g2.fillPolygon(cap);
                g2.setColor(hi);
            }
        }
    }

    private void paintTile(Graphics2D g2, int w, int h, int horizon, Map map,
                           Location loc, int depth, int col, boolean night) {
        if (!map.inBounds(loc)) {
            return;
        }
        Terrain t = map.terrainAt(loc);

        // Perspective: nearer depths are lower on screen and wider.
        double nearF = (DEPTH - depth + 1) / (double) DEPTH; // 1.0 near .. ~0.2 far
        int bandTop = horizon + (int) ((h - horizon) * (1.0 - nearF) * 0.95);
        int bandH = Math.max(6, (int) ((h - horizon) * 0.30 * nearF) + 4);
        int laneW = (int) ((w * 0.42) * nearF) + 26;
        int cx = w / 2 + (int) (col * laneW * 0.9);

        Color ground = TerrainArt.groundColor(t);
        if (night) {
            ground = ground.darker();
        }
        g2.setColor(ground);
        int tileW = laneW;
        g2.fillRect(cx - tileW / 2, bandTop, tileW, bandH);

        // Object on the tile, scaled by nearness.
        int objH = (int) (bandH * 1.8 * nearF) + 8;
        int baseY = bandTop + bandH;
        Stronghold sh = map.strongholdAt(loc);
        if (sh != null || TerrainArt.hasStructure(t)) {
            paintStructure(g2, cx, baseY, objH, t, sh, night);
        } else if (TerrainArt.isTrees(t)) {
            paintTrees(g2, cx, baseY, objH, tileW, night);
        } else if (TerrainArt.isHighGround(t)) {
            paintHill(g2, cx, baseY, objH, tileW, t, night);
        }
    }

    private void paintStructure(Graphics2D g2, int cx, int baseY, int objH, Terrain t,
                                Stronghold sh, boolean night) {
        int wWall = Math.max(14, objH);
        Color stone = night ? new Color(0x6c6658) : new Color(0xBFB39A);
        g2.setColor(stone);
        g2.fillRect(cx - wWall / 2, baseY - objH, wWall, objH);
        g2.setColor(stone.darker());
        // crenellations / roof
        g2.fillRect(cx - wWall / 2 - 3, baseY - objH, wWall + 6, Math.max(4, objH / 6));
        boolean tall = t == Terrain.TOWER || (sh != null && sh.type() == Terrain.TOWER)
                || t == Terrain.CITADEL || (sh != null && sh.type() == Terrain.CITADEL);
        if (tall) {
            int turret = wWall / 3;
            g2.setColor(stone);
            g2.fillRect(cx - turret / 2, baseY - (int) (objH * 1.4), turret, (int) (objH * 0.4));
        }
        if (sh != null) {
            Color flag = sh.owner() == Side.FREE ? new Color(0x2E86DE) : new Color(0xC0392B);
            g2.setColor(flag);
            g2.fillRect(cx, baseY - (int) (objH * (tall ? 1.4 : 1.0)) - 8, 12, 8);
            if (objH > 24) {
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
                g2.drawString(sh.name(), cx - wWall / 2, baseY - objH - 6);
            }
        }
    }

    private void paintTrees(Graphics2D g2, int cx, int baseY, int objH, int laneW, boolean night) {
        Color trunk = night ? new Color(0x352617) : new Color(0x5A3A1A);
        Color leaf = night ? new Color(0x143018) : new Color(0x2E5E33);
        int n = Math.max(2, laneW / 26);
        for (int i = 0; i < n; i++) {
            int tx = cx - laneW / 2 + (i + 1) * (laneW / (n + 1));
            int th = (int) (objH * (0.7 + 0.3 * ((i % 3) / 2.0)));
            g2.setColor(trunk);
            g2.fillRect(tx - 2, baseY - th / 3, 4, th / 3);
            g2.setColor(leaf);
            Polygon tri = new Polygon();
            tri.addPoint(tx, baseY - th);
            tri.addPoint(tx - th / 3, baseY - th / 3);
            tri.addPoint(tx + th / 3, baseY - th / 3);
            g2.fillPolygon(tri);
        }
    }

    private void paintHill(Graphics2D g2, int cx, int baseY, int objH, int laneW, Terrain t, boolean night) {
        Color rock = TerrainArt.groundColor(t);
        if (night) {
            rock = rock.darker();
        }
        g2.setColor(rock.darker());
        Polygon p = new Polygon();
        p.addPoint(cx - laneW / 2, baseY);
        p.addPoint(cx, baseY - objH);
        p.addPoint(cx + laneW / 2, baseY);
        g2.fillPolygon(p);
        if (t == Terrain.SNOW) {
            g2.setColor(night ? new Color(0x9aa3c0) : Color.WHITE);
            Polygon cap = new Polygon();
            cap.addPoint(cx - laneW / 8, baseY - (int) (objH * 0.7));
            cap.addPoint(cx, baseY - objH);
            cap.addPoint(cx + laneW / 8, baseY - (int) (objH * 0.7));
            g2.fillPolygon(cap);
        }
    }

    private void paintForegroundOccupants(Graphics2D g2, int w, int h, Location ahead, boolean night) {
        if (!state.map().inBounds(ahead)) {
            return;
        }
        List<Character> here = state.charactersAt(ahead);
        if (here.isEmpty()) {
            return;
        }
        int freeCount = 0;
        int doomCount = 0;
        int armies = 0;
        for (int i = 0; i < here.size(); i++) {
            Character c = here.get(i);
            if (!c.isAlive()) {
                continue;
            }
            if (c.side() == Side.FREE) {
                freeCount++;
            } else {
                doomCount++;
            }
            armies += c.warriors() + c.riders();
        }
        int baseY = (int) (h * 0.92);
        int slot = 0;
        int total = freeCount + doomCount;
        for (int i = 0; i < here.size(); i++) {
            Character c = here.get(i);
            if (!c.isAlive()) {
                continue;
            }
            int span = Math.max(1, total);
            int x = (int) (w * (0.5 + (slot - (span - 1) / 2.0) * 0.18));
            paintFigure(g2, x, baseY, c, night);
            slot++;
        }
        if (armies > 0) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(w / 2 - 90, 12, 180, 24, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            String who = doomCount > 0 ? "Foul army ahead!" : "Allies ahead";
            drawCentred(g2, who + "  (" + armies + " strong)", w / 2, 29);
        }
    }

    private void paintFigure(Graphics2D g2, int x, int baseY, Character c, boolean night) {
        Color body = c.side() == Side.FREE ? new Color(0x2E86DE) : new Color(0xC0392B);
        if (night) {
            body = body.darker();
        }
        int hgt = 70;
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(x - 18, baseY - 6, 36, 12);
        g2.setColor(body);
        g2.fillRoundRect(x - 12, baseY - hgt + 16, 24, hgt - 16, 10, 10); // cloak
        g2.setColor(night ? new Color(0xC9B79A) : new Color(0xEAD8B8));
        g2.fillOval(x - 8, baseY - hgt, 16, 16); // head
        if (c.isMounted()) {
            g2.setColor(body.darker());
            g2.fillRoundRect(x - 20, baseY - 24, 40, 16, 8, 8); // mount
        }
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
        drawCentred(g2, c.name(), x, baseY - hgt - 6);
        if (c.carriesIceCrown()) {
            g2.setColor(new Color(0x9AD8FF));
            int[] xs = {x - 9, x - 5, x, x + 5, x + 9, x + 6, x - 6};
            int[] ys = {baseY - hgt - 10, baseY - hgt - 18, baseY - hgt - 12, baseY - hgt - 18, baseY - hgt - 10, baseY - hgt - 8, baseY - hgt - 8};
            g2.fillPolygon(xs, ys, xs.length);
        }
    }

    // ----- overlays ----------------------------------------------------------

    private void paintCompassRose(Graphics2D g2, int w, Direction facing) {
        int cx = w - 46;
        int cy = 46;
        int r = 30;
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
        drawCentred(g2, "N", cx, cy - r + 12);
        // needle points the way the lord faces
        double ang = angleOf(facing);
        int nx = cx + (int) (Math.sin(ang) * (r - 8));
        int ny = cy - (int) (Math.cos(ang) * (r - 8));
        g2.setColor(new Color(0xE67E22));
        g2.drawLine(cx, cy, nx, ny);
        g2.fillOval(nx - 3, ny - 3, 6, 6);
        g2.setColor(Color.WHITE);
        drawCentred(g2, label(facing), cx, cy + r + 12);
    }

    private void paintNightVeil(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0x00, 0x00, 0x20, 90));
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(0xCBD3FF));
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
        drawCentred(g2, "NIGHT — Doomdark stirs. The lords cannot move.", w / 2, h - 14);
    }

    // ----- geometry helpers --------------------------------------------------

    /** base + facing*depth + right*col, walked one step at a time. */
    private static Location offset(Location base, Direction facing, Direction right, int depth, int col) {
        Location loc = base;
        for (int i = 0; i < depth; i++) {
            loc = loc.neighbor(facing);
        }
        if (col > 0) {
            for (int i = 0; i < col; i++) {
                loc = loc.neighbor(right);
            }
        } else if (col < 0) {
            Direction left = right.opposite();
            for (int i = 0; i < -col; i++) {
                loc = loc.neighbor(left);
            }
        }
        return loc;
    }

    private static double angleOf(Direction d) {
        // radians clockwise from north
        return Math.toRadians(45.0 * d.ordinal());
    }

    private static String label(Direction d) {
        switch (d) {
            case NORTH: return "N";
            case NORTHEAST: return "NE";
            case EAST: return "E";
            case SOUTHEAST: return "SE";
            case SOUTH: return "S";
            case SOUTHWEST: return "SW";
            case WEST: return "W";
            case NORTHWEST: return "NW";
            default: return "?";
        }
    }

    private void drawCentred(Graphics2D g2, String s, int cx, int y) {
        int sw = g2.getFontMetrics().stringWidth(s);
        g2.drawString(s, cx - sw / 2, y);
    }

    private void drawCentredText(Graphics2D g2, int w, int h, String s) {
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        drawCentred(g2, s, w / 2, h / 2);
    }
}

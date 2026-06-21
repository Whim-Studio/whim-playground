package com.midnight.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import com.midnight.core.Character;
import com.midnight.core.GameState;
import com.midnight.core.Location;
import com.midnight.core.Map;
import com.midnight.core.Side;
import com.midnight.core.Stronghold;
import com.midnight.core.Terrain;

/**
 * Strategic top-down overlay of the whole {@link Map}: terrain colours,
 * strongholds (coloured by owner), lord markers for both sides, the Ice Crown,
 * and a highlight on {@code state.selected()}. Toggled in/out by the frame.
 *
 * <p>Pure view &mdash; reads the core map and characters only.
 */
final class MapPanel extends JPanel {

    private GameState state;

    MapPanel(GameState state) {
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

        Map map = state.map();
        int cols = map.width();
        int rows = map.height();
        int cell = Math.max(4, Math.min((getWidth() - 8) / cols, (getHeight() - 8) / rows));
        int gridW = cell * cols;
        int gridH = cell * rows;
        int ox = (getWidth() - gridW) / 2;
        int oy = (getHeight() - gridH) / 2;

        // Terrain (y=0 is north / top, matching the world model).
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Location loc = Location.of(x, y);
                g2.setColor(TerrainArt.mapColor(map.terrainAt(loc)));
                g2.fillRect(ox + x * cell, oy + y * cell, cell, cell);
            }
        }

        // Strongholds.
        List<Stronghold> holds = map.strongholds();
        for (int i = 0; i < holds.size(); i++) {
            Stronghold sh = holds.get(i);
            Location loc = sh.location();
            int sx = ox + loc.x() * cell;
            int sy = oy + loc.y() * cell;
            g2.setColor(sh.owner() == Side.FREE ? new Color(0x2E86DE) : new Color(0xC0392B));
            g2.fillRect(sx, sy, cell, cell);
            g2.setColor(Color.WHITE);
            g2.drawRect(sx, sy, cell - 1, cell - 1);
        }

        // Lord markers for both sides.
        List<Character> all = state.characters();
        for (int i = 0; i < all.size(); i++) {
            Character c = all.get(i);
            if (!c.isAlive()) {
                continue;
            }
            Location loc = c.location();
            int sx = ox + loc.x() * cell;
            int sy = oy + loc.y() * cell;
            int d = Math.max(3, cell - 2);
            int px = sx + (cell - d) / 2;
            int py = sy + (cell - d) / 2;
            g2.setColor(c.side() == Side.FREE ? new Color(0x9AD8FF) : new Color(0x3A1010));
            g2.fillOval(px, py, d, d);
            g2.setColor(c.side() == Side.FREE ? new Color(0x174A8C) : new Color(0xE74C3C));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(px, py, d, d);
        }

        // Ice Crown marker.
        Location crown = state.iceCrownLocation();
        if (crown != null && map.inBounds(crown)) {
            int sx = ox + crown.x() * cell;
            int sy = oy + crown.y() * cell;
            g2.setColor(new Color(0xBEE7FF));
            int[] xs = {sx + cell / 2, sx + cell, sx + cell / 2, sx};
            int[] ys = {sy, sy + cell / 2, sy + cell, sy + cell / 2};
            g2.fillPolygon(xs, ys, 4);
        }

        // Highlight the selected lord.
        Character me = state.selected();
        if (me != null && me.isAlive()) {
            Location loc = me.location();
            int sx = ox + loc.x() * cell;
            int sy = oy + loc.y() * cell;
            g2.setColor(new Color(0xF1C40F));
            g2.setStroke(new BasicStroke(2.5f));
            int pad = 2;
            g2.drawRect(sx - pad, sy - pad, cell + pad * 2, cell + pad * 2);
        }

        // Compass + legend caption.
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(ox + 6, oy + 6, 132, 20, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
        g2.drawString("N ↑   Day " + state.day() + "   " + (state.isDay() ? "DAY" : "NIGHT"),
                ox + 12, oy + 20);

        g2.dispose();
    }
}

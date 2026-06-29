package com.whim.nextrun.ui;

import com.whim.nextrun.domain.EntityType;
import com.whim.nextrun.domain.GridMap;
import com.whim.nextrun.domain.Position;
import com.whim.nextrun.domain.Tile;
import com.whim.nextrun.engine.GameState;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Center canvas: draws the world grid with pure Graphics2D primitives — colored
 * cells plus a Unicode glyph per entity. No images, no external assets.
 */
public final class MapPanel extends JPanel {

    private static final int CELL = 30;

    private final GameState game;

    public MapPanel(GameState game) {
        this.game = game;
        GridMap m = game.map;
        setPreferredSize(new Dimension(m.width * CELL, m.height * CELL));
        setBackground(new Color(12, 12, 18));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GridMap m = game.map;
        Font glyph = new Font("Monospaced", Font.BOLD, 18);
        g.setFont(glyph);

        for (int x = 0; x < m.width; x++) {
            for (int y = 0; y < m.height; y++) {
                int px = x * CELL;
                int py = y * CELL;
                Tile t = m.at(x, y);

                if (!t.discovered) {
                    g.setColor(new Color(20, 20, 28));
                    g.fillRect(px, py, CELL, CELL);
                    g.setColor(new Color(28, 28, 38));
                    g.drawRect(px, py, CELL, CELL);
                    continue;
                }

                g.setColor(new Color(30, 32, 44));
                g.fillRect(px, py, CELL, CELL);
                g.setColor(new Color(45, 48, 64));
                g.drawRect(px, py, CELL, CELL);

                drawEntity(g, t, px, py);
            }
        }

        // the hero on top
        Position p = game.player.pos;
        int hx = p.x * CELL;
        int hy = p.y * CELL;
        g.setColor(new Color(90, 200, 255));
        g.fillOval(hx + 5, hy + 5, CELL - 10, CELL - 10);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(hx + 5, hy + 5, CELL - 10, CELL - 10);
        g.setColor(new Color(10, 20, 30));
        centerGlyph(g, "@", hx, hy);
    }

    private void drawEntity(Graphics2D g, Tile t, int px, int py) {
        Color c;
        String glyph;
        switch (t.type) {
            case RESOURCE:  c = new Color(110, 190, 90);  glyph = "♣"; break; // club = tree/herb
            case GOLD_PILE: c = new Color(230, 200, 70);  glyph = "$";       break;
            case RUIN:      c = new Color(150, 130, 110);  glyph = "⌂"; break; // house
            case ENEMY:     c = new Color(220, 70, 70);    glyph = enemyGlyph(t); break;
            case STRUCTURE: c = new Color(120, 150, 230);  glyph = "⚑"; break; // flag
            case PORTAL:    c = new Color(190, 110, 230);  glyph = "◉"; break; // fisheye
            default:        return;
        }
        g.setColor(c);
        centerGlyph(g, glyph, px, py);
    }

    private String enemyGlyph(Tile t) {
        if (t.enemy != null && t.enemy.tier >= 4) return "♛"; // queen = archdemon
        if (t.enemy != null && t.enemy.tier >= 2) return "♞"; // knight
        return "☠"; // skull
    }

    private void centerGlyph(Graphics2D g, String s, int px, int py) {
        int w = g.getFontMetrics().stringWidth(s);
        int h = g.getFontMetrics().getAscent();
        g.drawString(s, px + (CELL - w) / 2, py + (CELL + h) / 2 - 2);
    }
}

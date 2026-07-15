package com.whim.ruinlander.ui;

import com.whim.ruinlander.domain.Enemy;
import com.whim.ruinlander.domain.Entity;
import com.whim.ruinlander.domain.GameMode;
import com.whim.ruinlander.domain.GridMap;
import com.whim.ruinlander.domain.Player;
import com.whim.ruinlander.domain.Position;
import com.whim.ruinlander.domain.Tile;
import com.whim.ruinlander.engine.CombatState;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Center canvas. Renders the exploration overworld in EXPLORATION mode and the
 * tactical arena in COMBAT mode, using only Graphics2D primitives, colors, and
 * Unicode glyphs (no external assets). Clicking a cell forwards to the
 * controller (used to pick combat targets).
 */
public class MapPanel extends JPanel {

    private static final Color BG = new Color(18, 16, 14);
    private static final Color GRID = new Color(40, 38, 34);
    private static final Color FOG = new Color(10, 9, 8);
    private static final Color PLAYER = new Color(255, 224, 90);

    private final GameController controller;

    // Geometry captured on each paint so mouse clicks map back to grid cells.
    private int cell = 24;
    private int originX = 0;
    private int originY = 0;
    private int gridW = 1;
    private int gridH = 1;

    public MapPanel(GameController controller) {
        this.controller = controller;
        setBackground(BG);
        setPreferredSize(new Dimension(640, 480));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (cell <= 0) {
                    return;
                }
                int gx = (e.getX() - originX) / cell;
                int gy = (e.getY() - originY) / cell;
                if (gx >= 0 && gy >= 0 && gx < gridW && gy < gridH) {
                    controller.handleGridClick(gx, gy);
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (controller.getStateManager().getMode() == GameMode.COMBAT
                && controller.getCombatState() != null) {
            paintCombat(g2);
        } else {
            paintExploration(g2);
        }
    }

    private void computeGeometry(int cols, int rows) {
        gridW = cols;
        gridH = rows;
        int w = getWidth();
        int h = getHeight();
        cell = Math.max(8, Math.min(w / cols, h / rows));
        originX = (w - cell * cols) / 2;
        originY = (h - cell * rows) / 2;
    }

    private void paintExploration(Graphics2D g2) {
        GridMap map = controller.getStateManager().getMap();
        Player player = controller.getStateManager().getPlayer();
        computeGeometry(map.getWidth(), map.getHeight());

        Font glyphFont = new Font("Monospaced", Font.BOLD, Math.max(10, cell - 8));
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int px = originX + x * cell;
                int py = originY + y * cell;
                Tile t = map.getTile(x, y);
                if (!t.isDiscovered()) {
                    g2.setColor(FOG);
                    g2.fillRect(px, py, cell, cell);
                    continue;
                }
                g2.setColor(terrainColor(t));
                g2.fillRect(px, py, cell, cell);
                g2.setColor(GRID);
                g2.drawRect(px, py, cell, cell);

                Entity ent = t.getEntity();
                if (ent != null) {
                    drawGlyph(g2, glyphFont, ent.glyph(), ent.color(), px, py);
                }
            }
        }
        // Player on top.
        Position pp = player.getPosition();
        int px = originX + pp.x * cell;
        int py = originY + pp.y * cell;
        g2.setColor(PLAYER);
        g2.fillOval(px + cell / 5, py + cell / 5, cell - 2 * cell / 5, cell - 2 * cell / 5);
        drawGlyph(g2, glyphFont, "@", new Color(30, 20, 0), px, py);

        drawBanner(g2, "WASTELAND  ·  turn " + controller.getStateManager().getTurnCount());
    }

    private void paintCombat(Graphics2D g2) {
        CombatState cs = controller.getCombatState();
        Player player = controller.getStateManager().getPlayer();
        computeGeometry(CombatState.WIDTH, CombatState.HEIGHT);

        Font glyphFont = new Font("Monospaced", Font.BOLD, Math.max(12, cell - 6));
        for (int y = 0; y < CombatState.HEIGHT; y++) {
            for (int x = 0; x < CombatState.WIDTH; x++) {
                int px = originX + x * cell;
                int py = originY + y * cell;
                g2.setColor(((x + y) % 2 == 0) ? new Color(46, 30, 28) : new Color(38, 26, 24));
                g2.fillRect(px, py, cell, cell);
                g2.setColor(GRID);
                g2.drawRect(px, py, cell, cell);
            }
        }

        // Enemies + selection ring.
        List<Enemy> alive = cs.aliveEnemies();
        int selected = controller.getSelectedEnemy();
        for (int i = 0; i < alive.size(); i++) {
            Enemy e = alive.get(i);
            Position ep = e.getPosition();
            if (ep == null) {
                continue;
            }
            int px = originX + ep.x * cell;
            int py = originY + ep.y * cell;
            if (i == selected) {
                g2.setColor(new Color(255, 90, 80));
                g2.drawRect(px + 1, py + 1, cell - 2, cell - 2);
                g2.drawRect(px + 2, py + 2, cell - 4, cell - 4);
            }
            drawGlyph(g2, glyphFont, e.glyph(), e.color(), px, py);
            // HP pip bar under the glyph.
            int barW = cell - 6;
            int filled = (int) Math.round(barW * (e.getHp() / (double) e.getMaxHp()));
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(px + 3, py + cell - 5, barW, 3);
            g2.setColor(new Color(220, 70, 60));
            g2.fillRect(px + 3, py + cell - 5, Math.max(0, filled), 3);
            // index label
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString(String.valueOf(i + 1), px + 2, py + 11);
        }

        // Player.
        Position pp = cs.getPlayerPos();
        int px = originX + pp.x * cell;
        int py = originY + pp.y * cell;
        g2.setColor(PLAYER);
        g2.fillOval(px + cell / 5, py + cell / 5, cell - 2 * cell / 5, cell - 2 * cell / 5);
        drawGlyph(g2, glyphFont, "@", new Color(30, 20, 0), px, py);

        drawBanner(g2, "COMBAT  ·  round " + cs.getRound() + "  ·  AP " + player.getActionPoints()
                + "  [1-9 target · SPACE attack · move keys · E end turn]");
    }

    private void drawGlyph(Graphics2D g2, Font font, String glyph, Color color, int px, int py) {
        g2.setFont(font);
        g2.setColor(color != null ? color : Color.WHITE);
        int w = g2.getFontMetrics().stringWidth(glyph);
        int ascent = g2.getFontMetrics().getAscent();
        g2.drawString(glyph, px + (cell - w) / 2, py + (cell + ascent) / 2 - 2);
    }

    private void drawBanner(Graphics2D g2, String text) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, getWidth(), 20);
        g2.setColor(new Color(180, 200, 140));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(text, 8, 15);
    }

    private Color terrainColor(Tile t) {
        switch (t.getTerrain()) {
            case WASTELAND:
                return new Color(96, 84, 60);
            case CITY:
                return new Color(70, 72, 80);
            case TOXIC:
                return new Color(60, 96, 40);
            case SETTLEMENT:
                return new Color(120, 96, 56);
            case FOREST:
                return new Color(46, 78, 50);
            case WATER:
                return new Color(40, 70, 110);
            case ROAD:
                return new Color(60, 56, 52);
            default:
                return new Color(50, 50, 50);
        }
    }
}

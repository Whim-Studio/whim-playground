package com.heroquest.ui;

import com.heroquest.model.Entity;
import com.heroquest.model.Furniture;
import com.heroquest.model.GameState;
import com.heroquest.model.Hero;
import com.heroquest.model.Monster;
import com.heroquest.model.Point;
import com.heroquest.model.Tile;
import com.heroquest.model.TileType;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

/**
 * Procedurally renders the top-down dungeon with Graphics2D: fog of war, walls,
 * floors, doors, furniture and entities. Reads only from {@link GameState}.
 */
public final class BoardPanel extends JPanel {
    static final int TILE = 34;

    private static final Color FOG = new Color(0x10, 0x10, 0x14);
    private static final Color WALL = new Color(0x3A, 0x33, 0x2C);
    private static final Color WALL_EDGE = new Color(0x24, 0x20, 0x1B);
    private static final Color CORRIDOR = new Color(0x5B, 0x54, 0x4A);
    private static final Color ROOM = new Color(0x74, 0x6A, 0x5C);
    private static final Color GRID = new Color(0, 0, 0, 40);
    private static final Color DOOR = new Color(0x8A, 0x5A, 0x28);
    private static final Color REACH = new Color(0x4F, 0xC3, 0xF7, 70);
    private static final Color ATTACK = new Color(0xFF, 0x52, 0x52, 90);
    private static final Color ACTIVE_RING = new Color(0xFF, 0xD5, 0x4F);

    private final GameController controller;

    public BoardPanel(GameController controller) {
        this.controller = controller;
        GameState s = controller.getState();
        int w = s.getMap().getWidth() * TILE;
        int h = s.getMap().getHeight() * TILE;
        setPreferredSize(new Dimension(w, h));
        setBackground(FOG);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int gx = e.getX() / TILE;
                int gy = e.getY() / TILE;
                if (s.getMap().inBounds(gx, gy)) {
                    controller.onTileClicked(new Point(gx, gy));
                }
            }
        });
    }

    /** Resizes the drawing surface to match a (possibly new) map. */
    public void setPreferredSizeToMap(GameState s) {
        int w = s.getMap().getWidth() * TILE;
        int h = s.getMap().getHeight() * TILE;
        setPreferredSize(new Dimension(w, h));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GameState s = controller.getState();

        drawTiles(g2, s);
        drawHighlights(g2, s);
        drawEntities(g2, s);
    }

    private void drawTiles(Graphics2D g2, GameState s) {
        for (int y = 0; y < s.getMap().getHeight(); y++) {
            for (int x = 0; x < s.getMap().getWidth(); x++) {
                Tile t = s.getMap().tileAt(x, y);
                int px = x * TILE;
                int py = y * TILE;
                if (!t.isRevealed()) {
                    g2.setColor(FOG);
                    g2.fillRect(px, py, TILE, TILE);
                    continue;
                }
                Color base;
                TileType type = t.getType();
                if (type == TileType.WALL) {
                    base = WALL;
                } else if (t.isRoom()) {
                    base = ROOM;
                } else {
                    base = CORRIDOR;
                }
                g2.setColor(base);
                g2.fillRect(px, py, TILE, TILE);

                if (type == TileType.WALL) {
                    g2.setColor(WALL_EDGE);
                    g2.drawRect(px, py, TILE - 1, TILE - 1);
                } else {
                    g2.setColor(GRID);
                    g2.drawRect(px, py, TILE, TILE);
                }

                if (type.isDoor()) {
                    drawDoor(g2, px, py, type == TileType.DOOR_OPEN);
                }
                if (t.getFurniture() != null) {
                    drawFurniture(g2, px, py, t.getFurniture());
                }
            }
        }
    }

    private void drawDoor(Graphics2D g2, int px, int py, boolean open) {
        g2.setColor(open ? new Color(0xB6, 0x8A, 0x4C) : DOOR);
        int m = 5;
        g2.fillRect(px + m, py + m, TILE - 2 * m, TILE - 2 * m);
        g2.setColor(WALL_EDGE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(px + m, py + m, TILE - 2 * m, TILE - 2 * m);
        if (open) {
            g2.setColor(CORRIDOR);
            g2.fillRect(px + TILE / 2 - 3, py + m, 6, TILE - 2 * m);
        }
    }

    private void drawFurniture(Graphics2D g2, int px, int py, Furniture f) {
        g2.setColor(new Color(0x2E, 0x24, 0x1A));
        g2.fillRoundRect(px + 4, py + 6, TILE - 8, TILE - 12, 6, 6);
        g2.setColor(new Color(0xC8, 0xA9, 0x6A));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String label = f.getLabel().substring(0, 1);
        g2.drawString(label, px + TILE / 2 - 4, py + TILE / 2 + 4);
    }

    private void drawHighlights(Graphics2D g2, GameState s) {
        if (controller.isAttackMode()) {
            g2.setColor(ATTACK);
            for (Monster m : s.getLivingMonsters()) {
                if (controller.canActiveHeroAttack(m)) {
                    fillTile(g2, m.getPosition());
                }
            }
            return;
        }
        Set<Point> reach = controller.getReachable();
        g2.setColor(REACH);
        for (Point p : reach) {
            fillTile(g2, p);
        }
    }

    private void fillTile(Graphics2D g2, Point p) {
        g2.fillRect(p.x * TILE + 2, p.y * TILE + 2, TILE - 4, TILE - 4);
    }

    private void drawEntities(Graphics2D g2, GameState s) {
        for (Monster m : s.getLivingMonsters()) {
            if (s.getMap().tileAt(m.getPosition()).isRevealed()) {
                drawMonster(g2, m);
            }
        }
        Hero active = s.getActiveHero();
        for (Hero h : s.getLivingHeroes()) {
            drawHero(g2, h, h == active);
        }
    }

    private void drawHero(Graphics2D g2, Hero h, boolean active) {
        int px = h.getPosition().x * TILE;
        int py = h.getPosition().y * TILE;
        if (active) {
            g2.setColor(ACTIVE_RING);
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(px + 3, py + 3, TILE - 6, TILE - 6);
        }
        g2.setColor(h.getColor());
        g2.fillOval(px + 6, py + 6, TILE - 12, TILE - 12);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(h.getType().getLabel().substring(0, 1), px + TILE / 2 - 4, py + TILE / 2 + 5);
        drawHealthBar(g2, px, py, h);
    }

    private void drawMonster(Graphics2D g2, Monster m) {
        int px = m.getPosition().x * TILE;
        int py = m.getPosition().y * TILE;
        int cx = px + TILE / 2;
        int cy = py + TILE / 2;
        int r = TILE / 2 - 5;
        int[] xs = {cx, cx + r, cx, cx - r};
        int[] ys = {cy - r, cy, cy + r, cy};
        g2.setColor(m.getColor());
        g2.fillPolygon(xs, ys, 4);
        g2.setColor(new Color(0x1A, 0x1A, 0x1A));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(xs, ys, 4);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(String.valueOf(m.getType().getGlyph()), cx - 4, cy + 4);
        drawHealthBar(g2, px, py, m);
    }

    private void drawHealthBar(Graphics2D g2, int px, int py, Entity e) {
        int w = TILE - 10;
        int x = px + 5;
        int y = py + TILE - 6;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(x, y, w, 4);
        double frac = e.getMaxBodyPoints() == 0 ? 0 : (double) e.getBodyPoints() / e.getMaxBodyPoints();
        g2.setColor(frac > 0.5 ? new Color(0x4C, 0xAF, 0x50)
                : frac > 0.25 ? new Color(0xFF, 0xC1, 0x07) : new Color(0xF4, 0x43, 0x36));
        g2.fillRect(x, y, (int) (w * frac), 4);
    }
}

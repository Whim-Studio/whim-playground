package com.whim.tacticalnexus.ui;

import com.whim.tacticalnexus.domain.Door;
import com.whim.tacticalnexus.domain.Enemy;
import com.whim.tacticalnexus.domain.Entity;
import com.whim.tacticalnexus.domain.GemType;
import com.whim.tacticalnexus.domain.GridMap;
import com.whim.tacticalnexus.domain.KeyColor;
import com.whim.tacticalnexus.domain.KeyItem;
import com.whim.tacticalnexus.domain.Player;
import com.whim.tacticalnexus.domain.Position;
import com.whim.tacticalnexus.domain.StairDirection;
import com.whim.tacticalnexus.domain.Staircase;
import com.whim.tacticalnexus.domain.StatGem;
import com.whim.tacticalnexus.state.StateManager;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * CENTER component: renders the current floor with {@link Graphics2D} primitives.
 *
 * <p>The panel is a pure view. Each {@link #paintComponent} reads
 * {@code stateManager.current()} fresh and draws whatever it finds — it never
 * mutates state or computes a rule. Tiles are {@value #TILE}px squares.
 */
public final class GamePanel extends JPanel {

    /** Tile edge in pixels. */
    static final int TILE = 40;

    private static final Color BACKGROUND = new Color(24, 24, 28);
    private static final Color FLOOR = new Color(46, 46, 54);
    private static final Color GRID_LINE = new Color(60, 60, 70);
    private static final Color WALL = new Color(70, 70, 78);
    private static final Color PLAYER = new Color(38, 198, 188);

    private final StateManager stateManager;

    public GamePanel(StateManager stateManager) {
        this.stateManager = stateManager;
        GridMap floor = stateManager.current().currentFloor();
        setPreferredSize(new Dimension(floor.cols() * TILE, floor.rows() * TILE));
        setBackground(BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GridMap floor = stateManager.current().currentFloor();
        Player player = stateManager.current().player();

        for (int r = 0; r < floor.rows(); r++) {
            for (int c = 0; c < floor.cols(); c++) {
                int x = c * TILE;
                int y = r * TILE;
                g2.setColor(FLOOR);
                g2.fillRect(x, y, TILE, TILE);
                g2.setColor(GRID_LINE);
                g2.drawRect(x, y, TILE, TILE);

                Entity e = floor.at(new Position(r, c));
                if (e != null) {
                    drawEntity(g2, e, x, y);
                }
            }
        }

        drawPlayer(g2, player.position());
        g2.dispose();
    }

    private void drawEntity(Graphics2D g2, Entity e, int x, int y) {
        switch (e.type()) {
            case WALL:
                drawWall(g2, x, y);
                break;
            case DOOR:
                drawDoor(g2, (Door) e, x, y);
                break;
            case KEY:
                drawKey(g2, (KeyItem) e, x, y);
                break;
            case GEM:
                drawGem(g2, (StatGem) e, x, y);
                break;
            case ENEMY:
                drawEnemy(g2, (Enemy) e, x, y);
                break;
            case STAIR:
                drawStair(g2, (Staircase) e, x, y);
                break;
            default:
                // EMPTY / PLAYER markers are not drawn from the map layer.
                break;
        }
    }

    private void drawWall(Graphics2D g2, int x, int y) {
        g2.setColor(WALL);
        g2.fillRect(x + 1, y + 1, TILE - 1, TILE - 1);
        g2.setColor(WALL.brighter());
        g2.drawLine(x + 1, y + 1, x + TILE - 1, y + 1);
    }

    private void drawDoor(Graphics2D g2, Door door, int x, int y) {
        Color base = keyColor(door.color());
        g2.setColor(base.darker());
        g2.fillRoundRect(x + 4, y + 4, TILE - 8, TILE - 8, 6, 6);
        g2.setColor(base);
        g2.fillRoundRect(x + 6, y + 6, TILE - 12, TILE - 12, 6, 6);
        // Keyhole.
        g2.setColor(new Color(20, 20, 24));
        int cx = x + TILE / 2;
        g2.fillOval(cx - 4, y + TILE / 2 - 6, 8, 8);
        g2.fillRect(cx - 2, y + TILE / 2, 4, 8);
    }

    private void drawKey(Graphics2D g2, KeyItem key, int x, int y) {
        Color base = keyColor(key.color());
        int cx = x + TILE / 2;
        int cy = y + TILE / 2;
        Polygon diamond = new Polygon();
        diamond.addPoint(cx, cy - 11);
        diamond.addPoint(cx + 11, cy);
        diamond.addPoint(cx, cy + 11);
        diamond.addPoint(cx - 11, cy);
        g2.setColor(base);
        g2.fillPolygon(diamond);
        g2.setColor(base.darker().darker());
        g2.drawPolygon(diamond);
    }

    private void drawGem(Graphics2D g2, StatGem gem, int x, int y) {
        Color base = gemColor(gem.gem());
        int d = TILE - 12;
        g2.setColor(base);
        g2.fillOval(x + 6, y + 6, d, d);
        g2.setColor(base.darker());
        g2.drawOval(x + 6, y + 6, d, d);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        String label = "+" + gemLetter(gem.gem());
        drawCentered(g2, label, x + TILE / 2, y + TILE / 2 + 5);
    }

    private void drawEnemy(Graphics2D g2, Enemy enemy, int x, int y) {
        Color base = enemy.color() != null ? enemy.color() : new Color(170, 60, 60);
        g2.setColor(base);
        g2.fillRect(x + 3, y + 3, TILE - 6, TILE - 6);
        g2.setColor(base.darker());
        g2.drawRect(x + 3, y + 3, TILE - 6, TILE - 6);

        // Name initial.
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        String initial = enemy.name() != null && enemy.name().length() > 0
                ? enemy.name().substring(0, 1).toUpperCase()
                : "E";
        drawCentered(g2, initial, x + TILE / 2, y + TILE / 2 + 2);

        // Tiny HP/ATK/DEF tag along the bottom edge.
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(x + 3, y + TILE - 11, TILE - 6, 9);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 8));
        String tag = enemy.hp() + "/" + enemy.atk() + "/" + enemy.def();
        drawCentered(g2, tag, x + TILE / 2, y + TILE - 3);
    }

    private void drawStair(Graphics2D g2, Staircase stair, int x, int y) {
        boolean up = stair.direction() == StairDirection.UP;
        g2.setColor(up ? new Color(120, 200, 120) : new Color(200, 170, 110));
        g2.fillRoundRect(x + 5, y + 5, TILE - 10, TILE - 10, 6, 6);
        g2.setColor(new Color(20, 20, 24));
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        drawCentered(g2, up ? "<" : ">", x + TILE / 2, y + TILE / 2 + 7);
    }

    private void drawPlayer(Graphics2D g2, Position p) {
        int x = p.col() * TILE;
        int y = p.row() * TILE;
        g2.setColor(PLAYER);
        g2.fillRoundRect(x + 5, y + 5, TILE - 10, TILE - 10, 12, 12);
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(PLAYER.darker());
        g2.drawRoundRect(x + 5, y + 5, TILE - 10, TILE - 10, 12, 12);
        g2.setStroke(old);
    }

    private void drawCentered(Graphics2D g2, String text, int cx, int baselineY) {
        int w = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, cx - w / 2, baselineY);
    }

    private static Color keyColor(KeyColor c) {
        switch (c) {
            case YELLOW:
                return new Color(228, 196, 48);
            case BLUE:
                return new Color(70, 120, 220);
            case RED:
                return new Color(206, 64, 64);
            default:
                return Color.LIGHT_GRAY;
        }
    }

    private static Color gemColor(GemType g) {
        switch (g) {
            case HP:
                return new Color(80, 200, 110);
            case ATK:
                return new Color(220, 110, 70);
            case DEF:
                return new Color(110, 150, 230);
            default:
                return Color.LIGHT_GRAY;
        }
    }

    private static String gemLetter(GemType g) {
        switch (g) {
            case HP:
                return "H";
            case ATK:
                return "A";
            case DEF:
                return "D";
            default:
                return "?";
        }
    }
}

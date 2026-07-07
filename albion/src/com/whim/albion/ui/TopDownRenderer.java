package com.whim.albion.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import com.whim.albion.api.GameController;
import com.whim.albion.api.Views.NpcView;
import com.whim.albion.api.Views.PlayerView;
import com.whim.albion.api.Views.TileView;
import com.whim.albion.api.Views.WorldView;
import com.whim.albion.api.Enums.TileType;

/**
 * Bird's-eye tile renderer for {@link com.whim.albion.api.Enums.MapType#OUTDOOR_2D} maps.
 * Draws tiles by {@link TileType} + {@code decorKey}, NPCs by {@code spriteKey}, and the
 * player with a facing indicator. A mouse click issues {@code moveTo(x,y)}; arrow/WASD keys
 * are handled by {@link GameFrame} and translated to {@code move(dir)}.
 */
final class TopDownRenderer extends JPanel {

    private final GameController controller;

    TopDownRenderer(GameController controller) {
        this.controller = controller;
        setBackground(new Color(24, 28, 24));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onClick(e); }
        });
    }

    private void onClick(MouseEvent e) {
        WorldView w = controller.state().world();
        if (w == null) return;
        int cell = cellSize(w);
        int ox = originX(w, cell), oy = originY(w, cell);
        int tx = (e.getX() - ox) / cell;
        int ty = (e.getY() - oy) / cell;
        if (tx >= 0 && ty >= 0 && tx < w.width() && ty < w.height()) {
            controller.moveTo(tx, ty);
        }
    }

    private int cellSize(WorldView w) {
        int cw = Math.max(1, getWidth() / Math.max(1, w.width()));
        int ch = Math.max(1, getHeight() / Math.max(1, w.height()));
        return Math.max(8, Math.min(cw, ch));
    }

    private int originX(WorldView w, int cell) { return (getWidth() - cell * w.width()) / 2; }
    private int originY(WorldView w, int cell) { return (getHeight() - cell * w.height()) / 2; }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        WorldView w = controller.state().world();
        if (w == null) return;
        int cell = cellSize(w);
        int ox = originX(w, cell), oy = originY(w, cell);

        for (int y = 0; y < w.height(); y++) {
            for (int x = 0; x < w.width(); x++) {
                TileView t = w.tileAt(x, y);
                int px = ox + x * cell, py = oy + y * cell;
                g.setColor(tileColor(t.type()));
                g.fillRect(px, py, cell, cell);
                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(px, py, cell, cell);
                if (!t.decorKey().isEmpty()) {
                    SpriteFactory.drawDecor(g, t.decorKey(), px, py, cell, cell);
                }
            }
        }

        for (NpcView n : w.npcs()) {
            int px = ox + n.x() * cell, py = oy + n.y() * cell;
            SpriteFactory.drawActor(g, n.spriteKey(), px, py, cell, cell, n.hostile());
        }

        PlayerView p = w.player();
        SpriteFactory.drawPlayer(g, ox + p.x() * cell, oy + p.y() * cell, cell, cell, p.facing());

        // map name banner
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, getWidth(), 22);
        g.setColor(new Color(230, 220, 180));
        g.drawString(w.mapName() + "   (click to move · WASD/arrows · E to interact)", 8, 16);
    }

    private static Color tileColor(TileType t) {
        switch (t) {
            case GRASS:    return new Color(58, 120, 60);
            case PATH:     return new Color(150, 130, 90);
            case FLOOR:    return new Color(110, 100, 90);
            case WALL:     return new Color(70, 66, 62);
            case WATER:    return new Color(48, 90, 160);
            case DOOR:     return new Color(120, 82, 44);
            case OBSTACLE: return new Color(46, 60, 44);
            case STAIRS:   return new Color(40, 40, 50);
            case VOID:     return new Color(12, 12, 12);
            default:       return Color.MAGENTA;
        }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(640, 520); }
}

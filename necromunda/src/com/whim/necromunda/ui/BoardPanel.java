package com.whim.necromunda.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import com.whim.necromunda.engine.GameState;
import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.House;
import com.whim.necromunda.model.board.Board;
import com.whim.necromunda.model.board.Position;
import com.whim.necromunda.ui.render.FighterRenderer;
import com.whim.necromunda.ui.render.TerrainRenderer;

/**
 * The battlefield view. Custom-paints the grid, terrain, and fighter tokens from
 * {@link GameState} — it reads state only and forwards clicks as selection
 * intents. No rules logic lives here.
 */
public final class BoardPanel extends JPanel {

    private final GameState state;
    private Fighter selected;

    public BoardPanel(GameState state) {
        this.state = state;
        setBackground(new Color(0x1A, 0x1A, 0x1E));
        setPreferredSize(new Dimension(640, 640));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onClick(e.getX(), e.getY());
            }
        });
    }

    public Fighter selected() {
        return selected;
    }

    public void setSelected(Fighter f) {
        this.selected = f;
        repaint();
    }

    private int cellSize() {
        Board b = state.board();
        int cw = getWidth() / b.width();
        int ch = getHeight() / b.height();
        return Math.max(6, Math.min(cw, ch));
    }

    private void onClick(int mx, int my) {
        Board b = state.board();
        int size = cellSize();
        int gx = mx / size;
        int gy = my / size;
        if (!b.inBounds(gx, gy)) {
            return;
        }
        // Match a fighter on this (x,y) regardless of level.
        for (Gang g : state.gangs()) {
            for (Fighter f : g.roster()) {
                Position p = b.positionOf(f);
                if (p != null && p.x() == gx && p.y() == gy && f.status().inPlay()) {
                    setSelected(f);
                    return;
                }
            }
        }
        setSelected(null);
    }

    private House houseOf(Fighter f) {
        for (Gang g : state.gangs()) {
            if (g.roster().contains(f)) {
                return g.house();
            }
        }
        return House.ORLOCK;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Board board = state.board();
        int size = cellSize();

        // Terrain + grid.
        for (int x = 0; x < board.width(); x++) {
            for (int y = 0; y < board.height(); y++) {
                TerrainRenderer.paint(g, board.tile(x, y), x * size, y * size, size);
            }
        }

        // Fighters.
        for (Gang gang : state.gangs()) {
            for (Fighter f : gang.roster()) {
                Position p = board.positionOf(f);
                if (p == null) {
                    continue;
                }
                int px = p.x() * size;
                int py = p.y() * size;
                FighterRenderer.paint(g, f, gang.house(), px, py, size, f == selected);
                FighterRenderer.paintElevationBadge(g, px, py, size, p.z());
            }
        }
    }
}

package com.rampart.ui;

import com.rampart.engine.GameApi;
import com.rampart.model.GameStateView;
import com.rampart.model.Phase;
import com.rampart.model.Rules;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * The in-game canvas: a custom-painted {@link JPanel} that renders the 30&times;22
 * playfield and a HUD strip beneath it from the read-only {@link GameStateView}
 * snapshot polled off the {@link GameApi}. It owns no game state and never mutates
 * the model — the {@link InputHandler} forwards input to the engine and the game
 * loop (in {@link GameFrame}) drives ticks; this panel only paints and tracks the
 * current hover cell for the REPAIR ghost preview.
 */
public final class GamePanel extends JPanel {

    /** Pixel size of one grid cell. The single UI pixel&harr;cell mapping constant. */
    public static final int CELL = 24;
    /** Playfield width in pixels ({@code GRID_COLS * CELL}). */
    public static final int GRID_W = Rules.GRID_COLS * CELL; // 720
    /** Playfield height in pixels ({@code GRID_ROWS * CELL}). */
    public static final int GRID_H = Rules.GRID_ROWS * CELL; // 528
    /** Total panel width. */
    public static final int PANEL_W = GRID_W;
    /** Total panel height (playfield + HUD strip). */
    public static final int PANEL_H = GRID_H + Hud.HEIGHT;

    private final GameApi api;
    private final Renderer renderer = new Renderer();
    private final Hud hud = new Hud();

    /** Current hover cell (REPAIR ghost anchor); {@code -1} when off-grid. */
    private int hoverCol = -1;
    private int hoverRow = -1;

    public GamePanel(GameApi api) {
        this.api = api;
        setPreferredSize(new Dimension(PANEL_W, PANEL_H));
        setBackground(Palette.WATER_DEEP);
        setFocusable(true);
    }

    /** @return the pixel&harr;cell size used to map mouse coordinates to grid cells */
    public int cellSize() { return CELL; }

    /**
     * Records the hover cell for the ghost preview.
     *
     * @param col column, or {@code -1} to clear
     * @param row row, or {@code -1} to clear
     */
    public void setHover(int col, int row) {
        this.hoverCol = col;
        this.hoverRow = row;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        renderer.applyHints(g);

        GameStateView s = api.state();

        // Playfield.
        renderer.drawGrid(g, s.grid(), CELL);
        renderer.drawCastles(g, s.castles(), CELL);
        renderer.drawCannons(g, s.cannons(), CELL);
        renderer.drawShips(g, s.ships(), CELL);

        // REPAIR ghost preview at the hover cell.
        if (s.phase() == Phase.REPAIR && s.currentPiece() != null
                && hoverCol >= 0 && hoverRow >= 0) {
            renderer.drawGhost(g, s.currentPiece(), s.grid(), hoverCol, hoverRow, CELL);
        }

        // HUD strip below the grid.
        hud.draw(g, s, GRID_H, PANEL_W);

        // Thin separator frame around the playfield.
        g.setColor(new Color(0, 0, 0, 90));
        g.drawRect(0, 0, GRID_W - 1, GRID_H - 1);
    }
}

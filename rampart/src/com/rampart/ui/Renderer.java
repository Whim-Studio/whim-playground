package com.rampart.ui;

import com.rampart.model.CannonView;
import com.rampart.model.CastleView;
import com.rampart.model.Coord;
import com.rampart.model.GameStateView;
import com.rampart.model.GridView;
import com.rampart.model.ShipView;
import com.rampart.model.TileType;
import com.rampart.model.TileView;
import com.rampart.model.WallPieceView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Stateless drawing helpers the {@link GamePanel} delegates to. Every method
 * renders a slice of a read-only {@link GameStateView} snapshot with
 * {@code Graphics2D} primitives at a caller-supplied cell size. This class holds
 * NO game state and performs NO game logic — it only reads views and paints. The
 * ghost legality it computes is a purely cosmetic preview hint; the engine remains
 * the sole authority on whether a piece may actually be placed.
 */
public final class Renderer {

    /** Enable antialiasing for smoother ovals/polygons. */
    public void applyHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    // ---- Grid ----------------------------------------------------------------

    /**
     * Paints the whole terrain grid: one color-coded cell per tile, an enclosed
     * tint over sealed territory, and thin grid lines.
     */
    public void drawGrid(Graphics2D g, GridView grid, int cell) {
        int cols = grid.cols();
        int rows = grid.rows();
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                TileView t = grid.tile(c, r);
                drawTile(g, t, cell);
            }
        }
        // Grid lines on top for readability.
        g.setColor(Palette.GRID_LINE);
        g.setStroke(new BasicStroke(1f));
        for (int c = 0; c <= cols; c++) {
            g.drawLine(c * cell, 0, c * cell, rows * cell);
        }
        for (int r = 0; r <= rows; r++) {
            g.drawLine(0, r * cell, cols * cell, r * cell);
        }
    }

    private void drawTile(Graphics2D g, TileView t, int cell) {
        int x = t.col() * cell;
        int y = t.row() * cell;
        TileType type = t.type();
        g.setColor(Palette.forTile(type));
        g.fillRect(x, y, cell, cell);

        if (type == TileType.WATER) {
            // A couple of subtle wave dashes.
            g.setColor(Palette.WATER_DEEP);
            g.drawLine(x + 4, y + cell - 6, x + cell - 6, y + cell - 6);
        } else if (type == TileType.WALL) {
            // Brick-like border block.
            g.setColor(Palette.WALL_EDGE);
            g.drawRect(x + 1, y + 1, cell - 3, cell - 3);
        } else if (type == TileType.RUBBLE) {
            g.setColor(Palette.edgeFor(type));
            g.fillRect(x + 3, y + 3, cell / 3, cell / 3);
            g.fillRect(x + cell - cell / 3 - 3, y + cell / 2, cell / 3, cell / 3);
        }

        if (t.enclosed()) {
            g.setColor(Palette.ENCLOSED_TINT);
            g.fillRect(x, y, cell, cell);
        }
    }

    // ---- Castles -------------------------------------------------------------

    public void drawCastles(Graphics2D g, List<? extends CastleView> castles, int cell) {
        for (int i = 0; i < castles.size(); i++) {
            CastleView cv = castles.get(i);
            Coord p = cv.position();
            int x = p.col() * cell;
            int y = p.row() * cell;
            int pad = Math.max(2, cell / 8);
            Color body = cv.alive() ? Palette.CASTLE_BODY : Palette.CASTLE_DEAD;
            g.setColor(body);
            g.fillRect(x + pad, y + pad, cell - 2 * pad, cell - 2 * pad);
            // Crenellations along the top.
            g.setColor(Palette.CASTLE_TRIM);
            int merlon = Math.max(2, cell / 6);
            for (int mx = x + pad; mx < x + cell - pad; mx += merlon * 2) {
                g.fillRect(mx, y + pad - merlon / 2, merlon, merlon);
            }
            if (cv.alive()) {
                // Flag pole + pennant.
                g.setColor(Palette.CASTLE_TRIM);
                g.drawLine(x + cell / 2, y + pad, x + cell / 2, y + pad - cell / 3);
                g.setColor(Palette.CASTLE_FLAG);
                g.fillRect(x + cell / 2, y + pad - cell / 3, cell / 4, cell / 6);
            }
        }
    }

    // ---- Cannons -------------------------------------------------------------

    public void drawCannons(Graphics2D g, List<? extends CannonView> cannons, int cell) {
        for (int i = 0; i < cannons.size(); i++) {
            CannonView cn = cannons.get(i);
            if (!cn.alive()) continue;
            Coord p = cn.position();
            int cx = p.col() * cell + cell / 2;
            int cy = p.row() * cell + cell / 2;
            int rad = Math.max(3, cell / 3);
            // Base wheel.
            g.setColor(Palette.CANNON_TRIM);
            g.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);
            // Barrel pointing up-right.
            g.setColor(Palette.CANNON_BODY);
            g.setStroke(new BasicStroke(Math.max(2f, cell / 8f)));
            g.drawLine(cx, cy, cx + rad, cy - rad);
            g.fillOval(cx - rad / 2, cy - rad / 2, rad, rad);
            // Readiness pip.
            g.setColor(cn.ready() ? Palette.CANNON_READY : Palette.CANNON_RELOAD);
            g.fillOval(cx - 2, cy - 2, 4, 4);
        }
    }

    // ---- Ships ---------------------------------------------------------------

    public void drawShips(Graphics2D g, List<? extends ShipView> ships, int cell) {
        for (int i = 0; i < ships.size(); i++) {
            ShipView s = ships.get(i);
            if (!s.alive()) continue;
            // Sub-cell position → pixels.
            int cx = (int) Math.round(s.x() * cell);
            int cy = (int) Math.round(s.y() * cell);
            int w = cell * 2;
            int h = Math.max(6, cell - cell / 4);
            // Hull (a trapezoid).
            int[] xs = { cx - w / 2, cx + w / 2, cx + w / 2 - h / 2, cx - w / 2 + h / 2 };
            int[] ys = { cy, cy, cy + h, cy + h };
            g.setColor(Palette.SHIP_HULL);
            g.fillPolygon(xs, ys, 4);
            g.setColor(Palette.SHIP_HULL_HI);
            g.drawPolygon(xs, ys, 4);
            // Mast + sail.
            g.setColor(Palette.SHIP_HULL_HI);
            g.drawLine(cx, cy, cx, cy - cell);
            g.setColor(Palette.SHIP_SAIL);
            int sail = Math.max(6, cell - 4);
            g.fillPolygon(new int[] { cx + 1, cx + 1, cx + sail / 2 },
                          new int[] { cy - cell, cy - 2, cy - cell / 2 }, 3);
            // Damage bar for larger ships / hurt ships.
            int maxHp = s.type().baseHealth();
            if (maxHp > 0 && s.health() < maxHp) {
                double frac = Math.max(0.0, Math.min(1.0, s.health() / (double) maxHp));
                g.setColor(Palette.SHIP_DAMAGE);
                g.fillRect(cx - w / 2, cy - 3, (int) (w * frac), 2);
            }
        }
    }

    // ---- Ghost / preview piece ----------------------------------------------

    /**
     * Draws the REPAIR-phase ghost preview of {@code piece} anchored at the hover
     * cell, tinted legal/illegal. The legality here is a cosmetic hint only (bounds
     * + obvious terrain overlap read from the grid view); the engine decides the
     * real outcome on {@code placePieceAt}.
     */
    public void drawGhost(Graphics2D g, WallPieceView piece, GridView grid,
                          int hoverCol, int hoverRow, int cell) {
        if (piece == null) return;
        List<Coord> cells = piece.absoluteCells();
        boolean legal = ghostLooksLegal(cells, grid);
        Color fill = legal ? Palette.GHOST_LEGAL : Palette.GHOST_ILLEGAL;
        for (int i = 0; i < cells.size(); i++) {
            Coord c = cells.get(i);
            int x = c.col() * cell;
            int y = c.row() * cell;
            g.setColor(fill);
            g.fillRect(x, y, cell, cell);
            g.setColor(Palette.GHOST_BORDER);
            g.drawRect(x, y, cell - 1, cell - 1);
        }
    }

    /** Cosmetic-only hint: in-bounds and not over water/wall/cannon/castle. */
    private boolean ghostLooksLegal(List<Coord> cells, GridView grid) {
        for (int i = 0; i < cells.size(); i++) {
            Coord c = cells.get(i);
            if (!grid.inBounds(c.col(), c.row())) return false;
            TileType t = grid.typeAt(c.col(), c.row());
            if (t != TileType.LAND && t != TileType.RUBBLE) return false;
        }
        return true;
    }
}

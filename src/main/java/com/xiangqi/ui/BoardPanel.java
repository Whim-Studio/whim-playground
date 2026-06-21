package com.xiangqi.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.JPanel;

import com.xiangqi.core.Board;
import com.xiangqi.core.GameState;
import com.xiangqi.core.Piece;
import com.xiangqi.core.Position;
import com.xiangqi.core.Side;

/**
 * Renders the Xiangqi board (9 columns x 10 rows of intersections) and the
 * pieces sitting on those intersections, and translates mouse clicks into board
 * {@link Position}s. Pure view: it never mutates {@link GameState}; it reports
 * clicks to a listener and is told what to draw via setters.
 */
final class BoardPanel extends JPanel {

    private static final int COLS = 9; // intersections 0..8
    private static final int ROWS = 10; // intersections 0..9
    private static final int MARGIN = 40; // px around the playing area

    // Board "wood" and ink palette.
    private static final Color BOARD_BG = new Color(0xF3D9A0);
    private static final Color LINE = new Color(0x5A3A1A);
    private static final Color RIVER_TEXT = new Color(0x3A6EA5);
    private static final Color RED_PIECE = new Color(0xC0392B);
    private static final Color BLACK_PIECE = new Color(0x1C1C1C);
    private static final Color DISC = new Color(0xFBEFD2);
    private static final Color SELECT = new Color(0x2E86DE);
    private static final Color LEGAL = new Color(0x27AE60);
    private static final Color COACH = new Color(0x8E44AD);
    private static final Color LAST_MOVE = new Color(0xE67E22);

    private GameState state;
    private Position selected;
    private Set<Position> legalTargets = Collections.emptySet();
    private Set<Position> coachTargets = Collections.emptySet();
    private Position lastFrom;
    private Position lastTo;

    private final Consumer<Position> clickListener;

    BoardPanel(GameState initial, Consumer<Position> clickListener) {
        this.state = initial;
        this.clickListener = clickListener;
        setBackground(BOARD_BG);
        setPreferredSize(new Dimension(540, 600));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Position p = intersectionAt(e.getX(), e.getY());
                if (p != null) {
                    BoardPanel.this.clickListener.accept(p);
                }
            }
        });
    }

    // ----- state pushed in by the frame -------------------------------------

    void setState(GameState state) {
        this.state = state;
        repaint();
    }

    void setSelection(Position selected, Set<Position> legalTargets) {
        this.selected = selected;
        this.legalTargets = legalTargets == null ? Collections.<Position>emptySet() : legalTargets;
        repaint();
    }

    void setCoachTargets(Set<Position> coachTargets) {
        this.coachTargets = coachTargets == null ? Collections.<Position>emptySet() : coachTargets;
        repaint();
    }

    void setLastMove(Position from, Position to) {
        this.lastFrom = from;
        this.lastTo = to;
        repaint();
    }

    // ----- geometry ----------------------------------------------------------

    private int cellSize() {
        int usableW = (getWidth() - 2 * MARGIN) / (COLS - 1);
        int usableH = (getHeight() - 2 * MARGIN) / (ROWS - 1);
        return Math.max(20, Math.min(usableW, usableH));
    }

    private int originX(int cell) {
        int boardW = cell * (COLS - 1);
        return (getWidth() - boardW) / 2;
    }

    private int originY(int cell) {
        int boardH = cell * (ROWS - 1);
        return (getHeight() - boardH) / 2;
    }

    private int xOf(int col, int cell) {
        return originX(cell) + col * cell;
    }

    private int yOf(int row, int cell) {
        return originY(cell) + row * cell;
    }

    /** Nearest intersection to a pixel, or null if the click is too far off-grid. */
    private Position intersectionAt(int px, int py) {
        int cell = cellSize();
        int col = Math.round((px - originX(cell)) / (float) cell);
        int row = Math.round((py - originY(cell)) / (float) cell);
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return null;
        }
        int dx = px - xOf(col, cell);
        int dy = py - yOf(row, cell);
        if (Math.sqrt(dx * dx + dy * dy) > cell * 0.5) {
            return null; // clicked in dead space far from any node
        }
        return Position.of(row, col);
    }

    // ----- painting ----------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cell = cellSize();
        drawGrid(g2, cell);
        drawPalaces(g2, cell);
        drawRiver(g2, cell);
        drawHighlights(g2, cell);
        drawPieces(g2, cell);
        g2.dispose();
    }

    private void drawGrid(Graphics2D g2, int cell) {
        g2.setColor(LINE);
        g2.setStroke(new BasicStroke(1.4f));
        int left = xOf(0, cell);
        int right = xOf(COLS - 1, cell);
        int top = yOf(0, cell);
        int bottom = yOf(ROWS - 1, cell);

        // Horizontal lines span the full width at every rank.
        for (int r = 0; r < ROWS; r++) {
            int y = yOf(r, cell);
            g2.drawLine(left, y, right, y);
        }
        // Vertical lines: full-height at the two edges, broken at the river in between.
        int riverTop = yOf(4, cell);
        int riverBottom = yOf(5, cell);
        for (int c = 0; c < COLS; c++) {
            int x = xOf(c, cell);
            if (c == 0 || c == COLS - 1) {
                g2.drawLine(x, top, x, bottom);
            } else {
                g2.drawLine(x, top, x, riverTop);
                g2.drawLine(x, riverBottom, x, bottom);
            }
        }
    }

    private void drawPalaces(Graphics2D g2, int cell) {
        g2.setColor(LINE);
        g2.setStroke(new BasicStroke(1.4f));
        // BLACK palace: rows 0..2, cols 3..5. RED palace: rows 7..9, cols 3..5.
        drawPalaceCross(g2, cell, 0, 2);
        drawPalaceCross(g2, cell, 7, 9);
    }

    private void drawPalaceCross(Graphics2D g2, int cell, int topRow, int bottomRow) {
        int x3 = xOf(3, cell);
        int x5 = xOf(5, cell);
        int yTop = yOf(topRow, cell);
        int yBottom = yOf(bottomRow, cell);
        g2.drawLine(x3, yTop, x5, yBottom);
        g2.drawLine(x5, yTop, x3, yBottom);
    }

    private void drawRiver(Graphics2D g2, int cell) {
        int left = xOf(0, cell);
        int right = xOf(COLS - 1, cell);
        int riverTop = yOf(4, cell);
        int riverBottom = yOf(5, cell);

        g2.setColor(new Color(0xBFE0F2));
        g2.fillRect(left, riverTop + 1, right - left, riverBottom - riverTop - 1);

        g2.setColor(RIVER_TEXT);
        g2.setFont(deriveCjkFont(g2, Math.max(16, (int) (cell * 0.55f)), Font.BOLD));
        int midY = (riverTop + riverBottom) / 2;
        drawCenteredString(g2, "楚 河", left + (right - left) / 4, midY);
        drawCenteredString(g2, "漢 界", left + 3 * (right - left) / 4, midY);
    }

    private void drawHighlights(Graphics2D g2, int cell) {
        // Last move trail.
        if (lastFrom != null) {
            markNode(g2, lastFrom, cell, LAST_MOVE, 0.42f, false);
        }
        if (lastTo != null) {
            markNode(g2, lastTo, cell, LAST_MOVE, 0.42f, false);
        }
        // Coach recommendations (drawn under the selection ring so selection wins).
        for (Position p : coachTargets) {
            markNode(g2, p, cell, COACH, 0.46f, true);
        }
        // Legal targets for the selected piece.
        for (Position p : legalTargets) {
            markNode(g2, p, cell, LEGAL, 0.34f, false);
        }
        // The selected piece itself.
        if (selected != null) {
            markNode(g2, selected, cell, SELECT, 0.46f, true);
        }
    }

    private void markNode(Graphics2D g2, Position p, int cell, Color color, float frac, boolean ring) {
        int cx = xOf(p.col(), cell);
        int cy = yOf(p.row(), cell);
        int r = (int) (cell * frac);
        if (ring) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);
        } else {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 110));
            g2.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        }
    }

    private void drawPieces(Graphics2D g2, int cell) {
        if (state == null) {
            return;
        }
        Board board = state.board();
        int discR = (int) (cell * 0.44f);
        Stroke discStroke = new BasicStroke(2.2f);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Piece piece = board.pieceAt(row, col);
                if (piece == null) {
                    continue;
                }
                int cx = xOf(col, cell);
                int cy = yOf(row, cell);
                Color ink = piece.side() == Side.RED ? RED_PIECE : BLACK_PIECE;

                g2.setColor(DISC);
                g2.fillOval(cx - discR, cy - discR, 2 * discR, 2 * discR);
                g2.setColor(ink);
                g2.setStroke(discStroke);
                g2.drawOval(cx - discR, cy - discR, 2 * discR, 2 * discR);

                String text = glyphFor(g2, piece, (int) (cell * 0.55f));
                drawCenteredString(g2, text, cx, cy);
            }
        }
    }

    /** Returns the CJK glyph if the font can render it, else the Latin letter. */
    private String glyphFor(Graphics2D g2, Piece piece, int size) {
        String glyph = PieceGlyphs.glyph(piece);
        Font cjk = deriveCjkFont(g2, size, Font.BOLD);
        if (cjk.canDisplayUpTo(glyph) == -1) {
            g2.setFont(cjk);
            return glyph;
        }
        g2.setFont(getFont().deriveFont(Font.BOLD, size));
        return PieceGlyphs.fallbackLetter(piece.type());
    }

    private Font deriveCjkFont(Graphics2D g2, int size, int style) {
        // Prefer a font that actually has the glyphs; fall back to the panel font.
        String[] candidates = {"Noto Sans CJK SC", "SansSerif", "Serif", "Dialog"};
        for (String name : candidates) {
            Font f = new Font(name, style, size);
            if (f.canDisplay('車')) {
                return f;
            }
        }
        return new Font("SansSerif", style, size);
    }

    private void drawCenteredString(Graphics2D g2, String text, int cx, int cy) {
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tx = cx - fm.stringWidth(text) / 2;
        int ty = cy - fm.getHeight() / 2 + fm.getAscent();
        g2.drawString(text, tx, ty);
    }
}

package com.janggi.ui;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import com.janggi.core.Board;
import com.janggi.core.Piece;
import com.janggi.core.Position;
import com.janggi.core.Side;

/**
 * Renders the 9x10 Janggi board with pieces drawn ON the line intersections,
 * including the palace diagonals, and reports intersection clicks back to its
 * listener. Pure drawing + hit-testing; it holds no game rules.
 */
public class BoardPanel extends JComponent {

    /** Notified when the user clicks (near) a board intersection. */
    public interface IntersectionListener {
        void onIntersectionClicked(Position p);
    }

    private static final int COLS = 9;   // 0..8
    private static final int ROWS = 10;  // 0..9
    private static final int MARGIN = 44;

    private static final Color BOARD_BG = new Color(0xE7C27D);
    private static final Color LINE = new Color(0x4A3210);
    private static final Color CHO_COLOR = new Color(0x0B6E3B);   // green
    private static final Color HAN_COLOR = new Color(0xB22222);   // red
    private static final Color SELECT = new Color(0x2D7BE5);
    private static final Color MOVE_DOT = new Color(0x2D7BE5);
    private static final Color LAST_MOVE = new Color(0xF2A900);
    private static final Color CHECK = new Color(0xE53935);

    private Board board;
    private Position selected;
    private final Set<Position> destinations = new HashSet<Position>();
    private Position lastFrom;
    private Position lastTo;
    private Position checkedGeneral;
    private boolean interactive = true;

    private IntersectionListener listener;

    public BoardPanel() {
        setPreferredSize(new Dimension(
                MARGIN * 2 + cellW() * (COLS - 1),
                MARGIN * 2 + cellH() * (ROWS - 1)));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!interactive || listener == null) {
                    return;
                }
                Position p = intersectionAt(e.getX(), e.getY());
                if (p != null) {
                    listener.onIntersectionClicked(p);
                }
            }
        });
    }

    public void setListener(IntersectionListener listener) {
        this.listener = listener;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public void setBoard(Board board) {
        this.board = board;
        repaint();
    }

    public void setSelection(Position selected, List<Position> dests) {
        this.selected = selected;
        this.destinations.clear();
        if (dests != null) {
            this.destinations.addAll(dests);
        }
        repaint();
    }

    public void clearSelection() {
        setSelection(null, null);
    }

    public void setLastMove(Position from, Position to) {
        this.lastFrom = from;
        this.lastTo = to;
        repaint();
    }

    public void setCheckedGeneral(Position p) {
        this.checkedGeneral = p;
        repaint();
    }

    // --- geometry ---------------------------------------------------------

    private int cellW() {
        int avail = getWidth() - 2 * MARGIN;
        if (avail <= 0) {
            return 56; // sensible default before first layout
        }
        return avail / (COLS - 1);
    }

    private int cellH() {
        int avail = getHeight() - 2 * MARGIN;
        if (avail <= 0) {
            return 56;
        }
        return avail / (ROWS - 1);
    }

    private int xOf(int col) {
        return MARGIN + col * cellW();
    }

    private int yOf(int row) {
        return MARGIN + row * cellH();
    }

    private Position intersectionAt(int px, int py) {
        int cw = cellW();
        int ch = cellH();
        int col = Math.round((px - MARGIN) / (float) cw);
        int row = Math.round((py - MARGIN) / (float) ch);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
            return null;
        }
        int dx = px - xOf(col);
        int dy = py - yOf(row);
        int tol = Math.min(cw, ch) / 2;
        if (dx * dx + dy * dy > tol * tol) {
            return null;
        }
        return Position.of(row, col);
    }

    // --- painting ---------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(BOARD_BG);
        g2.fillRect(0, 0, getWidth(), getHeight());

        drawGrid(g2);
        drawPalaces(g2);
        drawLastMove(g2);
        drawPieces(g2);
        drawHighlights(g2);

        g2.dispose();
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(LINE);
        g2.setStroke(new BasicStroke(1.4f));
        for (int r = 0; r < ROWS; r++) {
            g2.drawLine(xOf(0), yOf(r), xOf(COLS - 1), yOf(r));
        }
        for (int c = 0; c < COLS; c++) {
            g2.drawLine(xOf(c), yOf(0), xOf(c), yOf(ROWS - 1));
        }
    }

    private void drawPalaces(Graphics2D g2) {
        g2.setColor(LINE);
        g2.setStroke(new BasicStroke(1.4f));
        // HAN palace rows 0..2, CHO palace rows 7..9; columns 3..5.
        drawPalaceDiagonals(g2, 0, 2);
        drawPalaceDiagonals(g2, 7, 9);
    }

    private void drawPalaceDiagonals(Graphics2D g2, int topRow, int bottomRow) {
        g2.drawLine(xOf(3), yOf(topRow), xOf(5), yOf(bottomRow));
        g2.drawLine(xOf(5), yOf(topRow), xOf(3), yOf(bottomRow));
    }

    private void drawLastMove(Graphics2D g2) {
        if (lastFrom == null || lastTo == null) {
            return;
        }
        g2.setColor(LAST_MOVE);
        g2.setStroke(new BasicStroke(3f));
        int r = pieceRadius();
        markRing(g2, lastFrom, r);
        markRing(g2, lastTo, r);
    }

    private void markRing(Graphics2D g2, Position p, int r) {
        g2.drawOval(xOf(p.col()) - r, yOf(p.row()) - r, r * 2, r * 2);
    }

    private int pieceRadius() {
        return (int) (Math.min(cellW(), cellH()) * 0.42);
    }

    private void drawPieces(Graphics2D g2) {
        if (board == null) {
            return;
        }
        int r = pieceRadius();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Piece piece = board.pieceAt(row, col);
                if (piece == null) {
                    continue;
                }
                drawPiece(g2, Position.of(row, col), piece, r);
            }
        }
    }

    private void drawPiece(Graphics2D g2, Position p, Piece piece, int r) {
        int cx = xOf(p.col());
        int cy = yOf(p.row());
        Color side = piece.side() == Side.CHO ? CHO_COLOR : HAN_COLOR;

        g2.setColor(new Color(0xF8EFD8));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(side);
        g2.setStroke(new BasicStroke(2.4f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);

        String label = PieceGlyphs.label(piece);
        Font font = getFont().deriveFont(Font.BOLD, r * 1.15f);
        g2.setFont(font);
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(label);
        int th = fm.getAscent();
        g2.drawString(label, cx - tw / 2, cy + th / 2 - 2);
    }

    private void drawHighlights(Graphics2D g2) {
        int r = pieceRadius();
        if (checkedGeneral != null) {
            g2.setColor(CHECK);
            g2.setStroke(new BasicStroke(3.5f));
            markRing(g2, checkedGeneral, r + 3);
        }
        if (selected != null) {
            g2.setColor(SELECT);
            g2.setStroke(new BasicStroke(3.5f));
            markRing(g2, selected, r + 1);
        }
        g2.setColor(MOVE_DOT);
        List<Position> dots = new ArrayList<Position>(destinations);
        for (int i = 0; i < dots.size(); i++) {
            Position d = dots.get(i);
            boolean capture = board != null && board.pieceAt(d) != null;
            if (capture) {
                g2.setStroke(new BasicStroke(3f));
                markRing(g2, d, r + 1);
            } else {
                int dr = Math.max(5, r / 3);
                g2.fillOval(xOf(d.col()) - dr, yOf(d.row()) - dr, dr * 2, dr * 2);
            }
        }
    }
}

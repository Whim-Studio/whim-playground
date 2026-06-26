package com.whim.monopoly.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.whim.monopoly.domain.Board;
import com.whim.monopoly.domain.ColorGroup;
import com.whim.monopoly.domain.OwnableSpace;
import com.whim.monopoly.domain.Player;
import com.whim.monopoly.domain.Space;
import com.whim.monopoly.domain.SpaceType;
import com.whim.monopoly.domain.StreetSpace;
import com.whim.monopoly.engine.GameEngine;
import com.whim.monopoly.engine.GameState;
import com.whim.monopoly.engine.Holding;

/**
 * Classic square 40-space Monopoly board rendered with Java2D. Reads everything
 * live from {@link GameState}; mutates nothing.
 */
public class BoardPanel extends JPanel {

    private final GameEngine engine;

    public BoardPanel(GameEngine engine) {
        this.engine = engine;
        setBackground(BoardColors.BOARD_BG);
        setPreferredSize(new Dimension(760, 760));
    }

    /** Side length of the square board in pixels for the current panel size. */
    private int boardLen() {
        return Math.max(330, Math.min(getWidth(), getHeight()));
    }

    /**
     * Geometry for one of the 40 spaces. The board is an 11x11 grid; the 40
     * border cells are uniform squares of side {@code cell = boardLen / 11}.
     */
    Rectangle cellRect(int index) {
        int cell = boardLen() / 11;
        int x;
        int y;
        if (index >= 0 && index <= 10) {
            // bottom edge, right -> left; index 0 bottom-right corner
            x = (10 - index) * cell;
            y = 10 * cell;
        } else if (index <= 20) {
            // left edge, bottom -> top
            x = 0;
            y = (20 - index) * cell;
        } else if (index <= 30) {
            // top edge, left -> right
            x = (index - 20) * cell;
            y = 0;
        } else {
            // right edge, top -> bottom
            x = 10 * cell;
            y = (index - 30) * cell;
        }
        return new Rectangle(x, y, cell, cell);
    }

    /** Which edge a space sits on: 0 bottom, 1 left, 2 top, 3 right. */
    private int edgeOf(int index) {
        if (index <= 10) {
            return 0;
        }
        if (index <= 20) {
            return 1;
        }
        if (index <= 30) {
            return 2;
        }
        return 3;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        GameState state = engine.getState();
        if (state == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int len = boardLen();
        int cell = len / 11;

        // Interior.
        g2.setColor(new Color(208, 230, 211));
        g2.fillRect(cell, cell, len - 2 * cell, len - 2 * cell);
        drawCenter(g2, cell, len);

        Board board = state.getBoard();
        for (int i = 0; i < Board.SIZE; i++) {
            drawSpace(g2, state, board.spaceAt(i), i);
        }

        drawTokens(g2, state, cell);
        g2.dispose();
    }

    private void drawCenter(Graphics2D g2, int cell, int len) {
        g2.setColor(new Color(176, 40, 52));
        Font f = getFont().deriveFont(Font.BOLD, Math.max(20f, cell * 0.7f));
        g2.setFont(f);
        String title = "MONOPOLY";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (len - fm.stringWidth(title)) / 2;
        int ty = len / 2 + fm.getAscent() / 2;
        g2.drawString(title, tx, ty);
    }

    private void drawSpace(Graphics2D g2, GameState state, Space space, int index) {
        Rectangle r = cellRect(index);
        int edge = edgeOf(index);

        g2.setColor(BoardColors.CELL_BG);
        g2.fill(r);

        Rectangle inner = new Rectangle(r);
        int band = Math.round(r.height * 0.22f);

        if (space.getType() == SpaceType.STREET) {
            StreetSpace street = (StreetSpace) space;
            g2.setColor(BoardColors.of(street.getColorGroup()));
            Rectangle bandRect = bandRect(r, edge, band);
            g2.fill(bandRect);
            g2.setColor(BoardColors.BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(bandRect);
            shrinkForBand(inner, edge, band);
        }

        // Mortgage tint for any mortgaged ownable.
        if (space instanceof OwnableSpace) {
            Holding h = state.holdingAt(index);
            if (h != null && h.isMortgaged()) {
                g2.setColor(BoardColors.MORTGAGE_TINT);
                g2.fill(r);
            }
        }

        drawLabel(g2, inner, space);
        drawPips(g2, state, index, r, edge);

        g2.setColor(BoardColors.BORDER);
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(r);
    }

    private Rectangle bandRect(Rectangle r, int edge, int band) {
        switch (edge) {
            case 0: // bottom row -> band on top (toward center)
                return new Rectangle(r.x, r.y, r.width, band);
            case 1: // left column -> band on right
                return new Rectangle(r.x + r.width - band, r.y, band, r.height);
            case 2: // top row -> band on bottom
                return new Rectangle(r.x, r.y + r.height - band, r.width, band);
            default: // right column -> band on left
                return new Rectangle(r.x, r.y, band, r.height);
        }
    }

    private void shrinkForBand(Rectangle inner, int edge, int band) {
        switch (edge) {
            case 0:
                inner.y += band;
                inner.height -= band;
                break;
            case 1:
                inner.width -= band;
                break;
            case 2:
                inner.height -= band;
                break;
            default:
                inner.x += band;
                inner.width -= band;
                break;
        }
    }

    private void drawLabel(Graphics2D g2, Rectangle inner, Space space) {
        g2.setColor(BoardColors.BORDER);
        float fontSize = Math.max(7f, inner.width * 0.16f);
        g2.setFont(getFont().deriveFont(fontSize));
        FontMetrics fm = g2.getFontMetrics();

        List<String> lines = wrap(space.getName(), fm, inner.width - 4);
        if (space instanceof OwnableSpace) {
            lines.add("$" + ((OwnableSpace) space).getPrice());
        }
        int lineH = fm.getHeight();
        int totalH = lineH * lines.size();
        int y = inner.y + (inner.height - totalH) / 2 + fm.getAscent();
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            int x = inner.x + (inner.width - fm.stringWidth(s)) / 2;
            g2.drawString(s, x, y + i * lineH);
        }
    }

    private List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        List<String> out = new ArrayList<String>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (fm.stringWidth(test) > maxWidth && cur.length() > 0) {
                out.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    /** Houses (green squares) / hotel (red rect) along the inner edge of the band. */
    private void drawPips(Graphics2D g2, GameState state, int index, Rectangle r, int edge) {
        Space space = state.getBoard().spaceAt(index);
        if (space.getType() != SpaceType.STREET) {
            return;
        }
        Holding h = state.holdingAt(index);
        if (h == null) {
            return;
        }
        int pip = Math.max(5, r.width / 9);
        int gap = Math.max(2, pip / 3);

        if (h.hasHotel()) {
            g2.setColor(BoardColors.HOTEL);
            int hw = pip * 2;
            int hh = pip;
            int cx = r.x + (r.width - hw) / 2;
            int cy = r.y + r.height - hh - 2;
            if (edge == 2) {
                cy = r.y + 2;
            }
            g2.fillRect(cx, cy, hw, hh);
            g2.setColor(BoardColors.BORDER);
            g2.drawRect(cx, cy, hw, hh);
            return;
        }

        int houses = h.getHouseCount();
        if (houses <= 0) {
            return;
        }
        int total = houses * pip + (houses - 1) * gap;
        int startX = r.x + (r.width - total) / 2;
        int py = (edge == 2) ? r.y + 3 : r.y + r.height - pip - 3;
        for (int i = 0; i < houses; i++) {
            int px = startX + i * (pip + gap);
            g2.setColor(BoardColors.HOUSE);
            g2.fillRect(px, py, pip, pip);
            g2.setColor(BoardColors.BORDER);
            g2.drawRect(px, py, pip, pip);
        }
    }

    private void drawTokens(Graphics2D g2, GameState state, int cell) {
        List<Player> players = state.getPlayers();

        // Group players by the space they render on. Jailed players render in
        // the jail corner (index 10) rather than wherever their position is.
        List<List<Player>> bySpace = new ArrayList<List<Player>>();
        for (int i = 0; i < Board.SIZE; i++) {
            bySpace.add(new ArrayList<Player>());
        }
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (p.isBankrupt()) {
                continue;
            }
            int spaceIdx = p.isInJail() ? 10 : p.getPosition();
            bySpace.get(spaceIdx).add(p);
        }

        int dia = Math.max(10, cell / 4);
        for (int idx = 0; idx < Board.SIZE; idx++) {
            List<Player> here = bySpace.get(idx);
            if (here.isEmpty()) {
                continue;
            }
            Rectangle r = cellRect(idx);
            for (int j = 0; j < here.size(); j++) {
                Player p = here.get(j);
                // Pack up to 6 tokens in a 3-column grid inside the cell.
                int col = j % 3;
                int row = j / 3;
                int ox = r.x + 4 + col * (dia + 2);
                int oy = r.y + r.height - dia - 4 - row * (dia + 2);
                boolean jailedHere = p.isInJail() && idx == 10;
                if (jailedHere) {
                    // Pull jailed tokens into the inner corner (the "Jail" box).
                    ox = r.x + r.width - dia - 4 - col * (dia + 2);
                    oy = r.y + 4 + row * (dia + 2);
                }
                g2.setColor(p.getToken() != null ? p.getToken() : Color.BLACK);
                g2.fillOval(ox, oy, dia, dia);
                g2.setColor(jailedHere ? BoardColors.HOTEL : Color.WHITE);
                g2.setStroke(new BasicStroke(jailedHere ? 2f : 1.5f));
                g2.drawOval(ox, oy, dia, dia);
            }
        }
    }
}

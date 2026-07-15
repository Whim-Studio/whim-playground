package com.whim.settlers.ui;

import com.whim.settlers.buildings.BuildingCategory;
import com.whim.settlers.buildings.BuildingType;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Left-edge build palette: every {@link BuildingType} grouped under its
 * {@link BuildingCategory}, one clickable row each. Selecting a row arms
 * placement mode (handled by the input layer); the armed type is highlighted.
 *
 * <p>Layout is computed deterministically from the enum order so rendering and
 * hit-testing ({@link #typeAt}) always agree.
 */
public final class BuildMenu {

    public static final int WIDTH = 150;
    private static final int HEADER_H = 18;
    private static final int ROW_H = 16;
    private static final int TOP = 92; // clears the debug HUD

    private final Font headerFont = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private final Font rowFont = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    /** Screen rectangle occupied by the panel; world clicks here are ignored. */
    public Rectangle bounds(int viewportH) {
        return new Rectangle(0, 0, WIDTH, viewportH);
    }

    public boolean contains(int x, int y, int viewportH) {
        return bounds(viewportH).contains(x, y);
    }

    public void render(Graphics2D g, BuildingType selected, int viewportH) {
        g.setColor(new Color(0, 0, 0, 165));
        g.fillRect(0, 0, WIDTH, viewportH);
        g.setColor(new Color(255, 255, 255, 40));
        g.drawLine(WIDTH, 0, WIDTH, viewportH);

        g.setFont(headerFont);
        g.setColor(Color.WHITE);
        g.drawString("BUILD", 10, 24);
        g.setFont(rowFont);
        g.setColor(new Color(190, 190, 190));
        g.drawString(selected == null ? "(pick a building)" : selected.displayName(), 10, 40);

        int y = TOP;
        BuildingCategory current = null;
        for (BuildingType t : BuildingType.values()) {
            if (t.category() != current) {
                current = t.category();
                g.setFont(headerFont);
                g.setColor(current.color());
                g.drawString(current.label().toUpperCase(), 8, y + 13);
                y += HEADER_H;
            }
            Rectangle row = new Rectangle(0, y, WIDTH, ROW_H);
            boolean isSel = t == selected;
            if (isSel) {
                g.setColor(new Color(80, 120, 200, 180));
                g.fillRect(row.x, row.y, row.width, row.height);
            }
            // Colour swatch + label.
            g.setColor(t.color());
            g.fillRect(8, y + 3, 10, 10);
            g.setColor(new Color(0, 0, 0, 120));
            g.drawRect(8, y + 3, 10, 10);
            g.setFont(rowFont);
            g.setColor(isSel ? Color.WHITE : new Color(220, 220, 220));
            g.drawString(t.displayName(), 24, y + 12);
            y += ROW_H;
        }
    }

    /** The building type whose row contains {@code (x,y)}, or {@code null}. */
    public BuildingType typeAt(int x, int y) {
        if (x < 0 || x >= WIDTH) return null;
        int cy = TOP;
        BuildingCategory current = null;
        for (BuildingType t : BuildingType.values()) {
            if (t.category() != current) {
                current = t.category();
                cy += HEADER_H;
            }
            if (y >= cy && y < cy + ROW_H) return t;
            cy += ROW_H;
        }
        return null;
    }
}

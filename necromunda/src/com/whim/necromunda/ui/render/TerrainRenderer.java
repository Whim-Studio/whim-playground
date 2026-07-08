package com.whim.necromunda.ui.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import com.whim.necromunda.model.board.Tile;
import com.whim.necromunda.model.board.TerrainType;

/**
 * Draws a single board {@link Tile} as a Java2D geometric placeholder — no art
 * assets. Isolated here so richer CC0 sprites could be swapped in later without
 * touching the board panel or the engine.
 */
public final class TerrainRenderer {

    private static final Color OPEN_FILL   = new Color(0x2B, 0x2B, 0x30);
    private static final Color GRID_LINE   = new Color(0x3C, 0x3C, 0x44);
    private static final Color WALL_FILL   = new Color(0x70, 0x70, 0x78);
    private static final Color PIT_FILL    = new Color(0x0A, 0x0A, 0x0C);
    private static final Color PIT_BORDER  = new Color(0xE0, 0xA0, 0x20);
    private static final Color RUBBLE_FILL = new Color(0x5A, 0x46, 0x32);
    private static final Color BARRICADE   = new Color(0xE0, 0xA0, 0x20);
    private static final Color GANTRY_FILL = new Color(0x55, 0x60, 0x55);
    private static final Color LADDER_FILL = new Color(0x8A, 0x6A, 0x30);
    private static final Color PLATFORM    = new Color(0x40, 0x55, 0x78);

    private TerrainRenderer() {
    }

    /** Paint one tile within the pixel rectangle (px, py, size, size). */
    public static void paint(Graphics2D g, Tile tile, int px, int py, int size) {
        TerrainType t = tile.terrain();

        // Base floor + grid.
        g.setColor(OPEN_FILL);
        g.fillRect(px, py, size, size);

        switch (t) {
            case WALL:
                g.setColor(WALL_FILL);
                g.fillRect(px, py, size, size);
                g.setColor(WALL_FILL.darker());
                g.drawRect(px, py, size - 1, size - 1);
                break;
            case PIT:
                g.setColor(PIT_FILL);
                g.fillRect(px, py, size, size);
                g.setColor(PIT_BORDER);
                g.setStroke(new BasicStroke(2f));
                g.drawRect(px + 1, py + 1, size - 3, size - 3);
                g.setStroke(new BasicStroke(1f));
                break;
            case RUBBLE:
                g.setColor(RUBBLE_FILL);
                g.fillRect(px, py, size, size);
                g.setColor(RUBBLE_FILL.brighter());
                for (int i = 0; i < 5; i++) {
                    int rx = px + (i * 7 + 3) % (size - 3);
                    int ry = py + (i * 11 + 5) % (size - 3);
                    g.fillRect(rx, ry, 2, 2);
                }
                break;
            case BARRICADE:
                g.setColor(BARRICADE);
                g.fillRect(px + 1, py + size / 3, size - 2, size / 3);
                break;
            case GANTRY:
                g.setColor(GANTRY_FILL);
                g.fillRect(px, py, size, size);
                g.setColor(GANTRY_FILL.darker());
                for (int i = 2; i < size; i += 4) {
                    g.drawLine(px + i, py, px + i, py + size);
                }
                break;
            case LADDER:
                g.setColor(LADDER_FILL);
                g.fillRect(px + size / 3, py, size / 3, size);
                g.setColor(LADDER_FILL.darker());
                for (int i = 2; i < size; i += 4) {
                    g.drawLine(px + size / 3, py + i, px + 2 * size / 3, py + i);
                }
                break;
            case PLATFORM:
                g.setColor(PLATFORM);
                g.fillRect(px, py, size, size);
                g.setColor(PLATFORM.brighter());
                g.drawRect(px, py, size - 1, size - 1);
                break;
            case OPEN:
            default:
                break;
        }

        // Elevation shading: brighten higher tiles slightly and label the level.
        if (tile.height() > 0 && t != TerrainType.WALL) {
            g.setColor(new Color(255, 255, 255, 22 * Math.min(3, tile.height())));
            g.fillRect(px, py, size, size);
        }

        // Grid overlay.
        g.setColor(GRID_LINE);
        g.drawRect(px, py, size, size);
    }
}

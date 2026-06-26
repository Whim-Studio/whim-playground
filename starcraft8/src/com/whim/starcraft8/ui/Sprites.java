package com.whim.starcraft8.ui;

import java.awt.Color;
import java.awt.Graphics2D;

import com.whim.starcraft8.domain.AttackKind;
import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.UnitType;

/**
 * Algorithmic 8-bit sprite bank. Every sprite is a small {@code int[][]} bitmap
 * (0 = transparent, 1 = base tint, 2 = shadow, 3 = highlight, 4 = outline) that is
 * tinted at draw time with the {@code baseColor()} of the unit/building type and
 * scaled with nearest-neighbour (hard pixel edges, no anti-aliasing).
 *
 * No external image assets are used — all art is drawn from these arrays.
 */
final class Sprites {

    private Sprites() {}

    // ---- Unit bitmaps (8x8) ------------------------------------------------

    private static final int[][] WORKER = {
        {0,0,4,4,4,4,0,0},
        {0,4,1,3,3,1,4,0},
        {0,4,1,1,1,1,4,0},
        {4,1,1,2,2,1,1,4},
        {4,1,1,1,1,1,1,4},
        {0,4,1,1,1,1,4,0},
        {0,4,2,0,0,2,4,0},
        {0,4,4,0,0,4,4,0},
    };

    private static final int[][] RANGED = {
        {0,0,4,4,4,4,0,0},
        {0,4,1,1,1,1,4,0},
        {0,4,3,1,1,3,4,0},
        {4,1,1,1,1,1,1,4},
        {4,2,1,1,1,1,2,4},
        {0,4,1,1,1,1,4,3},
        {0,4,1,0,0,1,4,3},
        {0,4,4,0,0,4,4,0},
    };

    private static final int[][] MELEE = {
        {0,4,4,0,0,4,4,0},
        {0,4,1,4,4,1,4,0},
        {0,0,4,1,1,4,0,0},
        {0,4,1,3,3,1,4,0},
        {4,1,1,1,1,1,1,4},
        {4,2,1,1,1,1,2,4},
        {0,4,1,0,0,1,4,0},
        {0,4,4,0,0,4,4,0},
    };

    private static final int[][] BIG = {
        {0,4,4,4,4,4,4,0},
        {4,1,3,1,1,3,1,4},
        {4,1,1,1,1,1,1,4},
        {4,2,1,1,1,1,2,4},
        {4,1,1,2,2,1,1,4},
        {4,1,1,1,1,1,1,4},
        {4,2,4,1,1,4,2,4},
        {0,4,0,4,4,0,4,0},
    };

    private static final int[][] FLYER = {
        {0,0,0,4,4,0,0,0},
        {0,4,4,1,1,4,4,0},
        {4,1,3,1,1,3,1,4},
        {4,1,1,1,1,1,1,4},
        {0,4,1,1,1,1,4,0},
        {0,0,4,2,2,4,0,0},
        {0,4,3,0,0,3,4,0},
        {4,0,0,0,0,0,0,4},
    };

    // ---- Building bitmaps (8x8, scaled to footprint) -----------------------

    private static final int[][] TOWNHALL = {
        {4,4,4,4,4,4,4,4},
        {4,3,1,1,1,1,3,4},
        {4,1,2,2,2,2,1,4},
        {4,1,2,3,3,2,1,4},
        {4,1,2,3,3,2,1,4},
        {4,1,2,2,2,2,1,4},
        {4,1,1,1,1,1,1,4},
        {4,4,4,2,2,4,4,4},
    };

    private static final int[][] SUPPLY = {
        {0,4,4,4,4,4,4,0},
        {4,1,1,3,3,1,1,4},
        {4,1,2,1,1,2,1,4},
        {4,3,1,1,1,1,3,4},
        {4,1,1,2,2,1,1,4},
        {4,1,3,1,1,3,1,4},
        {4,1,1,1,1,1,1,4},
        {0,4,4,4,4,4,4,0},
    };

    private static final int[][] GAS = {
        {0,0,4,4,4,4,0,0},
        {0,4,3,1,1,3,4,0},
        {4,1,1,2,2,1,1,4},
        {4,1,2,3,3,2,1,4},
        {4,1,2,3,3,2,1,4},
        {4,1,1,2,2,1,1,4},
        {4,3,1,1,1,1,3,4},
        {0,4,4,4,4,4,4,0},
    };

    private static final int[][] PRODUCTION = {
        {4,4,4,4,4,4,4,4},
        {4,1,1,1,1,1,1,4},
        {4,1,3,2,2,3,1,4},
        {4,1,2,1,1,2,1,4},
        {4,1,2,1,1,2,1,4},
        {4,3,1,1,1,1,3,4},
        {4,1,1,2,2,1,1,4},
        {4,4,2,4,4,2,4,4},
    };

    static int[][] forUnit(UnitType t) {
        if (t.isWorker()) return WORKER;
        if (t.isFlyer()) return FLYER;
        if (t.supplyCost() >= 2) return BIG;
        if (t.attackKind() == AttackKind.RANGED) return RANGED;
        return MELEE;
    }

    static int[][] forBuilding(BuildingType b) {
        if (b.isTownHall()) return TOWNHALL;
        if (b.isSupply()) return SUPPLY;
        if (b.isGas()) return GAS;
        return PRODUCTION;
    }

    // ---- Drawing -----------------------------------------------------------

    /** Draw a tinted bitmap as chunky pixels in [px,py, size x size], nearest-neighbour. */
    static void draw(Graphics2D g, int[][] bm, Color base, int px, int py, int size) {
        int rows = bm.length;
        int cols = bm[0].length;
        // integer cell size, at least 1px, so edges stay hard
        int cell = Math.max(1, size / Math.max(rows, cols));
        Color shadow = darker(base);
        Color light = lighter(base);
        Color outline = darker(shadow);
        int ox = px + (size - cell * cols) / 2;
        int oy = py + (size - cell * rows) / 2;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int v = bm[r][c];
                if (v == 0) continue;
                Color col;
                if (v == 1) col = base;
                else if (v == 2) col = shadow;
                else if (v == 3) col = light;
                else col = outline;
                g.setColor(col);
                g.fillRect(ox + c * cell, oy + r * cell, cell, cell);
            }
        }
    }

    static Color darker(Color c) {
        return new Color(
            (int) (c.getRed() * 0.55),
            (int) (c.getGreen() * 0.55),
            (int) (c.getBlue() * 0.55));
    }

    static Color lighter(Color c) {
        return new Color(
            Math.min(255, (int) (c.getRed() * 0.45 + 150)),
            Math.min(255, (int) (c.getGreen() * 0.45 + 150)),
            Math.min(255, (int) (c.getBlue() * 0.45 + 150)));
    }
}

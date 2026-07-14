package com.whim.stars.model.race;

import java.io.Serializable;

/**
 * A race's habitable range for one environment axis (Gravity, Temperature or
 * Radiation). Each planet reports that axis on a normalized 0..100 scale; a
 * race is comfortable within {@code [center - halfWidth, center + halfWidth]}.
 *
 * <p>A race may be {@code immune} to an axis, in which case that axis never
 * hurts habitability (it always reads as ideal).
 */
public final class HabBand implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int center;     // 0..100, midpoint of the comfortable range
    private final int halfWidth;  // 0..50, tolerance either side of center
    private final boolean immune;

    private HabBand(int center, int halfWidth, boolean immune) {
        this.center = center;
        this.halfWidth = halfWidth;
        this.immune = immune;
    }

    public static HabBand of(int center, int halfWidth) {
        return new HabBand(clamp(center, 0, 100), clamp(halfWidth, 1, 50), false);
    }

    public static HabBand immune() {
        return new HabBand(50, 50, true);
    }

    public int center() { return center; }
    public int halfWidth() { return halfWidth; }
    public boolean isImmune() { return immune; }

    public boolean contains(int value) {
        return immune || Math.abs(value - center) <= halfWidth;
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}

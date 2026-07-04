package com.whim.shinobi.domain;

import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/**
 * A solid terrain segment on a single plane (ground strip or raised ledge).
 * Implements {@link Views.PlatformView}. Static level geometry — never mutated
 * after {@link LevelBuilder} produces it.
 */
public class Platform implements Views.PlatformView {
    private final Aabb box;
    private final Enums.Plane plane;

    public Platform(Aabb box, Enums.Plane plane) {
        this.box = box;
        this.plane = plane;
    }

    // ---- Views.BoxView / PlatformView ----
    @Override public double x() { return box.x(); }
    @Override public double y() { return box.y(); }
    @Override public double w() { return box.w(); }
    @Override public double h() { return box.h(); }
    @Override public Enums.Plane plane() { return plane; }

    public Aabb box() { return box; }

    /** Top surface Y (feet-Y for an entity standing on this platform). */
    public double topY() { return box.y(); }

    /** True if world X lies within this platform's horizontal span. */
    public boolean spansX(double px) {
        return px >= box.x() && px <= box.x() + box.w();
    }
}

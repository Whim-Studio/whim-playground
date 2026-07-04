// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import com.whim.shinobi.api.Views;

/** Axis-aligned bounding box in world space; implements {@link Views.BoxView}. */
public class Aabb implements Views.BoxView {
    public double x, y, w, h;

    public Aabb() {}
    public Aabb(double x, double y, double w, double h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public double w() { return w; }
    @Override public double h() { return h; }

    public double cx() { return x + w / 2.0; }
    public double cy() { return y + h / 2.0; }
    public double right() { return x + w; }
    public double bottom() { return y + h; }

    /** True if this box overlaps another (AABB intersection test). */
    public boolean overlaps(Aabb o) {
        return x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y;
    }
}

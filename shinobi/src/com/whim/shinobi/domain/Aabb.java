package com.whim.shinobi.domain;

import com.whim.shinobi.api.Views;

/**
 * Mutable axis-aligned bounding box in world pixels. (x,y) is the top-left
 * corner of the box; w/h are its size. Implements {@link Views.BoxView} so the
 * engine can hand it straight to the UI as a read-only snapshot.
 *
 * Physics/collision <em>resolution</em> lives in the engine (Task 2); this class
 * only stores the box and offers pure geometry helpers.
 */
public class Aabb implements Views.BoxView {
    private double x;
    private double y;
    private double w;
    private double h;

    public Aabb(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    // ---- Views.BoxView ----
    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public double w() { return w; }
    @Override public double h() { return h; }

    // ---- Mutators ----
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setW(double w) { this.w = w; }
    public void setH(double h) { this.h = h; }

    /** Move the top-left corner to an absolute position. */
    public void moveTo(double nx, double ny) {
        this.x = nx;
        this.y = ny;
    }

    /** Shift the box by a delta. */
    public void translate(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    // ---- Derived geometry ----
    public double centerX() { return x + w * 0.5; }
    public double centerY() { return y + h * 0.5; }
    public double right() { return x + w; }
    public double bottom() { return y + h; }

    /** True if this box overlaps {@code o} (touching edges do not count). */
    public boolean intersects(Aabb o) {
        return x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y;
    }

    /** True if the world point (px,py) lies inside this box. */
    public boolean contains(double px, double py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    @Override
    public String toString() {
        return "Aabb[" + x + "," + y + " " + w + "x" + h + "]";
    }
}

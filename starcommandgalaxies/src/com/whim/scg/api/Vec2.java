package com.whim.scg.api;

/** Immutable 2D double vector for continuous-space combat (ships, projectiles). */
public final class Vec2 {
    public final double x, y;

    public Vec2(double x, double y) { this.x = x; this.y = y; }

    public Vec2 add(Vec2 o) { return new Vec2(x + o.x, y + o.y); }
    public Vec2 sub(Vec2 o) { return new Vec2(x - o.x, y - o.y); }
    public Vec2 scale(double s) { return new Vec2(x * s, y * s); }
    public double len() { return Math.sqrt(x * x + y * y); }
    public double dist(Vec2 o) { return sub(o).len(); }

    public Vec2 norm() {
        double l = len();
        return l < 1e-9 ? new Vec2(0, 0) : new Vec2(x / l, y / l);
    }

    @Override public String toString() { return "[" + x + "," + y + "]"; }
}

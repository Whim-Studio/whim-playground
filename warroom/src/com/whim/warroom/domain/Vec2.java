package com.whim.warroom.domain;

/** Immutable 2D vector in world space (double units). */
public final class Vec2 {
    public final double x;
    public final double y;

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 add(Vec2 o) {
        return new Vec2(x + o.x, y + o.y);
    }

    public Vec2 sub(Vec2 o) {
        return new Vec2(x - o.x, y - o.y);
    }

    public Vec2 scale(double s) {
        return new Vec2(x * s, y * s);
    }

    public double len() {
        return Math.sqrt(x * x + y * y);
    }

    public double dist(Vec2 o) {
        double dx = x - o.x;
        double dy = y - o.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Unit vector in the same direction; zero-safe (returns (0,0)). */
    public Vec2 normalized() {
        double l = len();
        if (l == 0.0) {
            return new Vec2(0.0, 0.0);
        }
        return new Vec2(x / l, y / l);
    }

    /** Linear interpolation from a to b by t (t clamped is caller's concern). */
    public static Vec2 lerp(Vec2 a, Vec2 b, double t) {
        return new Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Vec2)) {
            return false;
        }
        Vec2 o = (Vec2) obj;
        return Double.compare(x, o.x) == 0 && Double.compare(y, o.y) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(x) * 31 + Double.hashCode(y);
    }

    @Override
    public String toString() {
        return "Vec2(" + x + ", " + y + ")";
    }
}

package com.whimkit.layout;

/** An axis-aligned rectangle in device-independent CSS pixels. Mutable and cheap. */
public class Rect {
    public float x, y, width, height;

    public Rect() {}

    public Rect(float x, float y, float width, float height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    /** @return a rectangle expanded on all four sides by the given edge sizes. */
    public Rect expandedBy(EdgeSizes e) {
        return new Rect(x - e.left, y - e.top,
                width + e.left + e.right, height + e.top + e.bottom);
    }

    public float right() { return x + width; }
    public float bottom() { return y + height; }

    @Override
    public String toString() {
        return String.format("Rect[%.1f,%.1f %.1fx%.1f]", x, y, width, height);
    }
}

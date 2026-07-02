package com.whimkit.layout;

/** The four edge widths used by margin, border, and padding rings of the box model. */
public class EdgeSizes {
    public float top, right, bottom, left;

    public EdgeSizes() {}

    public EdgeSizes(float top, float right, float bottom, float left) {
        this.top = top; this.right = right; this.bottom = bottom; this.left = left;
    }
}

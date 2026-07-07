package com.whim.b5wars.model;

/**
 * The 6 hexsides, clockwise from the front:
 * F (front), FR (front-right), BR (back-right), B (back), BL (back-left), FL (front-left).
 */
public enum Facing {
    F, FR, BR, B, BL, FL;

    /** Ordinal position 0..5, clockwise from front. */
    public int index() {
        return ordinal();
    }

    /** Rotate clockwise by {@code steps} hexsides (+cw / -ccw), wrapping mod 6. */
    public Facing rotate(int steps) {
        Facing[] vals = values();
        int i = ((ordinal() + steps) % 6 + 6) % 6;
        return vals[i];
    }

    /** The facing 180 degrees opposite. */
    public Facing opposite() {
        return rotate(3);
    }
}

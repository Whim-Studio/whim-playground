package com.whim.b5wars.model;

/** Immutable axial hex coordinate (q, r) with hex-grid geometry helpers. */
public final class Hex {
    private final int q;
    private final int r;

    public Hex(int q, int r) {
        this.q = q;
        this.r = r;
    }

    public int getQ() {
        return q;
    }

    public int getR() {
        return r;
    }

    /** Axial hex distance between this hex and {@code other}. */
    public int distance(Hex other) {
        int dq = q - other.q;
        int dr = r - other.r;
        return (Math.abs(dq) + Math.abs(dq + dr) + Math.abs(dr)) / 2;
    }

    /** The adjacent hex in the given facing direction. */
    public Hex neighbor(Facing dir) {
        switch (dir) {
            case F:  return new Hex(q,     r - 1);
            case FR: return new Hex(q + 1, r - 1);
            case BR: return new Hex(q + 1, r);
            case B:  return new Hex(q,     r + 1);
            case BL: return new Hex(q - 1, r + 1);
            case FL: return new Hex(q - 1, r);
            default: throw new IllegalArgumentException("Unknown facing: " + dir);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hex)) {
            return false;
        }
        Hex other = (Hex) o;
        return q == other.q && r == other.r;
    }

    @Override
    public int hashCode() {
        return 31 * q + r;
    }

    @Override
    public String toString() {
        return "(" + q + "," + r + ")";
    }
}

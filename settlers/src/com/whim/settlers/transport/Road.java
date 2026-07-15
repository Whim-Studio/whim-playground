package com.whim.settlers.transport;

import java.util.List;

/**
 * A road segment joining two flags along a fixed tile path, worked by a single
 * carrier (the relay courier). The carrier picks up one shipment at a flag,
 * walks the length to the far flag, sets it down, then walks back empty before it
 * can carry again — so each segment has a finite throughput. Splitting a long
 * route with more flags gives it more carriers in parallel and thus more
 * throughput, exactly as the design describes.
 */
public final class Road {

    /** Tiles the carrier walks per second. */
    private static final float SPEED = 3.0f;

    private final int id;
    private final int flagA, flagB;
    private final List<int[]> path;   // tile path, flagA .. flagB
    private final float legTime;      // seconds to traverse the road one way

    // Carrier state.
    private Shipment carrying;
    private boolean towardB;          // travel direction of the loaded leg
    private float elapsed;            // progress along the current leg
    private boolean returning;        // walking back empty

    public Road(int id, int flagA, int flagB, List<int[]> path) {
        this.id = id;
        this.flagA = flagA;
        this.flagB = flagB;
        this.path = path;
        this.legTime = Math.max(0.2f, (path.size() - 1) / SPEED);
    }

    public int id()      { return id; }
    public int flagA()   { return flagA; }
    public int flagB()   { return flagB; }
    public List<int[]> path() { return path; }

    public int otherEnd(int flagId) { return flagId == flagA ? flagB : flagA; }
    public boolean connects(int flagId) { return flagId == flagA || flagId == flagB; }

    /** True when the carrier is idle at a flag and can accept a shipment. */
    public boolean free() { return carrying == null && !returning; }

    /** Load a shipment travelling from {@code fromFlag} to the other end. */
    public void load(Shipment s, int fromFlag) {
        this.carrying = s;
        this.towardB = (fromFlag == flagA);
        this.elapsed = 0f;
    }

    /**
     * Advance the carrier. When a loaded leg finishes, returns the arrived
     * shipment together with the flag it arrived at (so the network can deliver
     * or re-queue it); otherwise returns null.
     */
    public Arrival update(float dt) {
        if (carrying != null) {
            elapsed += dt;
            if (elapsed >= legTime) {
                Shipment s = carrying;
                int arrivedFlag = towardB ? flagB : flagA;
                carrying = null;
                returning = true;
                elapsed = 0f;
                return new Arrival(s, arrivedFlag);
            }
        } else if (returning) {
            elapsed += dt;
            if (elapsed >= legTime) { returning = false; elapsed = 0f; }
        }
        return null;
    }

    /** Interpolated carrier position for rendering, or null when idle. */
    public double[] carrierPos() {
        if (carrying == null && !returning) return null;
        float t = Math.min(1f, elapsed / legTime);
        float f = returning ? (1f - t) : t;         // returning walks back
        boolean dirB = returning ? !towardB : towardB;
        float along = dirB ? f : (1f - f);
        return sampleAlong(along);
    }

    public boolean loaded() { return carrying != null; }

    private double[] sampleAlong(float frac) {
        if (path.size() == 1) return new double[] { path.get(0)[0], path.get(0)[1] };
        float pos = frac * (path.size() - 1);
        int i = (int) Math.floor(pos);
        if (i >= path.size() - 1) i = path.size() - 2;
        float local = pos - i;
        int[] a = path.get(i), b = path.get(i + 1);
        return new double[] { a[0] + (b[0] - a[0]) * local, a[1] + (b[1] - a[1]) * local };
    }

    /** Result of a completed carry leg. */
    public static final class Arrival {
        public final Shipment shipment;
        public final int flagId;
        Arrival(Shipment shipment, int flagId) { this.shipment = shipment; this.flagId = flagId; }
    }
}

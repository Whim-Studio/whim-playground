package com.whim.settlers.transport;

import java.util.ArrayList;
import java.util.List;

/**
 * A road segment joining two flags along a fixed tile path, worked by one or two
 * carriers (relay couriers). A carrier picks up one shipment at a flag, walks the
 * length to the far flag, sets it down, then walks back empty before it can carry
 * again — so each carrier has a finite throughput.
 *
 * <p>Splitting a long route with more flags gives it more carriers in parallel.
 * In addition, a single busy segment under sustained congestion can be upgraded
 * with a <b>second carrier (a donkey)</b> ({@link #addCarrier()}), doubling its
 * throughput without changing the flag-relay model — the extra courier still
 * walks the whole leg, never teleports.
 */
public final class Road {

    /** Tiles a carrier walks per second. */
    private static final float SPEED = 3.0f;
    private static final int MAX_CARRIERS = 2;

    private final int id;
    private final int flagA, flagB;
    private final List<int[]> path;   // tile path, flagA .. flagB
    private final float legTime;      // seconds to traverse the road one way

    private final List<Carrier> carriers = new ArrayList<Carrier>();

    public Road(int id, int flagA, int flagB, List<int[]> path) {
        this.id = id;
        this.flagA = flagA;
        this.flagB = flagB;
        this.path = path;
        this.legTime = Math.max(0.2f, (path.size() - 1) / SPEED);
        carriers.add(new Carrier());
    }

    public int id()      { return id; }
    public int flagA()   { return flagA; }
    public int flagB()   { return flagB; }
    public List<int[]> path() { return path; }

    public int otherEnd(int flagId) { return flagId == flagA ? flagB : flagA; }
    public boolean connects(int flagId) { return flagId == flagA || flagId == flagB; }

    /** How many carriers (1, or 2 once upgraded with a donkey). */
    public int carrierCount() { return carriers.size(); }
    public boolean upgraded() { return carriers.size() > 1; }

    /** Add a second carrier (donkey) under sustained congestion. No-op at the cap. */
    public boolean addCarrier() {
        if (carriers.size() >= MAX_CARRIERS) return false;
        carriers.add(new Carrier());
        return true;
    }

    /** True when at least one carrier is idle at a flag and can accept a shipment. */
    public boolean free() {
        for (int i = 0; i < carriers.size(); i++) if (carriers.get(i).idle()) return true;
        return false;
    }

    /** Load a shipment travelling from {@code fromFlag} onto the first free carrier. */
    public void load(Shipment s, int fromFlag) {
        for (int i = 0; i < carriers.size(); i++) {
            if (carriers.get(i).idle()) { carriers.get(i).load(s, fromFlag == flagA); return; }
        }
    }

    /**
     * Advance every carrier. Returns the shipments that completed a loaded leg this
     * tick, each paired with the flag it arrived at (empty list if none).
     */
    public List<Arrival> update(float dt) {
        List<Arrival> arrivals = null;
        for (int i = 0; i < carriers.size(); i++) {
            Arrival a = carriers.get(i).update(dt);
            if (a != null) {
                if (arrivals == null) arrivals = new ArrayList<Arrival>(2);
                arrivals.add(a);
            }
        }
        return arrivals == null ? java.util.Collections.<Arrival>emptyList() : arrivals;
    }

    /** Interpolated positions of the active carriers, for rendering. */
    public List<double[]> carrierPositions() {
        List<double[]> out = new ArrayList<double[]>(carriers.size());
        for (int i = 0; i < carriers.size(); i++) {
            double[] p = carriers.get(i).pos();
            if (p != null) out.add(p);
        }
        return out;
    }

    /** One relay courier working this segment. */
    private final class Carrier {
        private Shipment carrying;
        private boolean towardB;
        private float elapsed;
        private boolean returning;

        boolean idle() { return carrying == null && !returning; }

        void load(Shipment s, boolean towardB) {
            this.carrying = s;
            this.towardB = towardB;
            this.elapsed = 0f;
        }

        Arrival update(float dt) {
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

        /** {x, y, loaded?1:0} of this carrier, or null when idle. */
        double[] pos() {
            if (carrying == null && !returning) return null;
            float t = Math.min(1f, elapsed / legTime);
            float f = returning ? (1f - t) : t;
            boolean dirB = returning ? !towardB : towardB;
            float along = dirB ? f : (1f - f);
            double[] xy = sampleAlong(along);
            return new double[] { xy[0], xy[1], carrying != null ? 1 : 0 };
        }
    }

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

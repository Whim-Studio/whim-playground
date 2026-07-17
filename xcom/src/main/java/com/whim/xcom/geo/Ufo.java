package com.whim.xcom.geo;

import com.whim.xcom.rules.def.UfoDef;

/**
 * A UFO on the Geoscape. Position is normalised {@code (x,y)} on an
 * equirectangular world (x = longitude 0..1, y = latitude 0..1). It flies toward
 * a wandering destination until detected/intercepted; when shot down it becomes a
 * crash site the player can assault.
 */
public final class Ufo {

    public enum Status { FLYING, CRASHED, LANDED, DESTROYED, ESCAPED }

    private final String id;
    private final UfoDef def;
    private double x;
    private double y;
    private double destX;
    private double destY;
    private int hull;
    private boolean detected;
    private Status status = Status.FLYING;
    private long spawnedAtSeconds;

    public Ufo(String id, UfoDef def, double x, double y, long spawnedAtSeconds) {
        this.id = id;
        this.def = def;
        this.x = x;
        this.y = y;
        this.destX = x;
        this.destY = y;
        this.hull = def.hullPoints();
        this.spawnedAtSeconds = spawnedAtSeconds;
    }

    public String id() { return id; }
    public UfoDef def() { return def; }
    public double x() { return x; }
    public double y() { return y; }
    public void setPos(double x, double y) { this.x = x; this.y = y; }
    public double destX() { return destX; }
    public double destY() { return destY; }
    public void setDest(double x, double y) { this.destX = x; this.destY = y; }
    public int hull() { return hull; }
    public boolean detected() { return detected; }
    public void setDetected(boolean d) { this.detected = d; }
    public Status status() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public long spawnedAtSeconds() { return spawnedAtSeconds; }

    /** Apply air-combat damage; returns true if this blow downs the craft. */
    public boolean damageHull(int amount) {
        hull -= amount;
        if (hull <= 0) {
            hull = 0;
            status = Status.CRASHED;
            return true;
        }
        return false;
    }

    public boolean active() {
        return status == Status.FLYING || status == Status.LANDED;
    }
}

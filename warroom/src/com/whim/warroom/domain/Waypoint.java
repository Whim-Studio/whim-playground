package com.whim.warroom.domain;

/**
 * One point on a {@link Route}: a world position and the absolute tick (from
 * scenario start) at which the unit should arrive. A waypoint may optionally be
 * a synchronized detonation that fires a blast when its tick is reached.
 */
public class Waypoint {
    private final Vec2 pos;
    private final int arrivalTick;

    private boolean detonation;
    private double blastRadius;
    private double blastDamage;

    public Waypoint(Vec2 pos, int arrivalTick) {
        this.pos = pos;
        this.arrivalTick = arrivalTick;
    }

    public Vec2 getPos() {
        return pos;
    }

    public int getArrivalTick() {
        return arrivalTick;
    }

    public boolean isDetonation() {
        return detonation;
    }

    public void setDetonation(boolean detonation) {
        this.detonation = detonation;
    }

    public double getBlastRadius() {
        return blastRadius;
    }

    public void setBlastRadius(double blastRadius) {
        this.blastRadius = blastRadius;
    }

    public double getBlastDamage() {
        return blastDamage;
    }

    public void setBlastDamage(double blastDamage) {
        this.blastDamage = blastDamage;
    }
}

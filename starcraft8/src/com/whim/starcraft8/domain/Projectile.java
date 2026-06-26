package com.whim.starcraft8.domain;

import java.awt.Color;

/** Live, engine-mutated homing projectile. Plain mutable POJO. */
public final class Projectile {
    private double x, y;
    private final long targetId;
    private final int damage;
    private final DamageType damageType;
    private final int splashRadius;
    private final double speed;
    private final Color color;
    private boolean done;

    public Projectile(double x, double y, long targetId, int damage, DamageType dmgType,
                      int splashRadius, double speed, Color color) {
        this.x = x;
        this.y = y;
        this.targetId = targetId;
        this.damage = damage;
        this.damageType = dmgType;
        this.splashRadius = splashRadius;
        this.speed = speed;
        this.color = color;
        this.done = false;
    }

    public double x() { return x; }
    public double y() { return y; }
    public void setPos(double x, double y) { this.x = x; this.y = y; }

    public long targetId() { return targetId; }
    public int damage() { return damage; }
    public DamageType damageType() { return damageType; }
    public int splashRadius() { return splashRadius; }
    public double speed() { return speed; }
    public Color color() { return color; }

    public boolean done() { return done; }
    public void setDone(boolean d) { this.done = d; }
}

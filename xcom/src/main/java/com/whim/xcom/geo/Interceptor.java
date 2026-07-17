package com.whim.xcom.geo;

/**
 * An X-COM interceptor craft. Sits at its base until dispatched at a UFO, flies
 * to intercept, resolves air combat, then returns to base. Normalised world
 * coordinates like {@link Ufo}.
 */
public final class Interceptor {

    public enum Status { READY, PURSUING, RETURNING }

    private final String name;
    private final double baseX;
    private final double baseY;
    private double x;
    private double y;
    private final double speed;   // world-fraction per hour
    private final int cannonDamage;
    private int hp;
    private final int maxHp;
    private Status status = Status.READY;
    private Ufo target;

    public Interceptor(String name, double baseX, double baseY,
                       double speed, int cannonDamage, int maxHp) {
        this.name = name;
        this.baseX = baseX;
        this.baseY = baseY;
        this.x = baseX;
        this.y = baseY;
        this.speed = speed;
        this.cannonDamage = cannonDamage;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public String name() { return name; }
    public double x() { return x; }
    public double y() { return y; }
    public void setPos(double x, double y) { this.x = x; this.y = y; }
    public double baseX() { return baseX; }
    public double baseY() { return baseY; }
    public double speed() { return speed; }
    public int cannonDamage() { return cannonDamage; }
    public int hp() { return hp; }
    public int maxHp() { return maxHp; }
    public void damage(int amount) { hp = Math.max(0, hp - amount); }
    public void repairFull() { hp = maxHp; }
    public Status status() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public Ufo target() { return target; }
    public void setTarget(Ufo t) { this.target = t; }

    public boolean ready() { return status == Status.READY; }

    public boolean atBase() {
        return Math.abs(x - baseX) < 1e-6 && Math.abs(y - baseY) < 1e-6;
    }
}

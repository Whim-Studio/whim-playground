package com.whim.starcraft8.domain;

import java.util.concurrent.atomic.AtomicLong;

/** Live, engine-mutated unit instance. Plain mutable POJO. */
public final class Unit {
    private static final AtomicLong NEXT_ID = new AtomicLong(0L);

    private final long id;
    private final UnitType type;
    private final int ownerId;
    private double x, y;
    private int hp;
    private int shield;
    private UnitState state;
    private int cooldownLeft;

    private double targetX, targetY;
    private long targetEntityId = -1L;

    private int carriedResource;
    private ResourceType carriedType;
    private int progressTicks;

    public Unit(UnitType type, int ownerId, double x, double y) {
        this.id = NEXT_ID.getAndIncrement();
        this.type = type;
        this.ownerId = ownerId;
        this.x = x;
        this.y = y;
        this.hp = type.maxHp();
        this.shield = type.maxShield();
        this.state = UnitState.IDLE;
        this.cooldownLeft = 0;
        this.targetX = x;
        this.targetY = y;
        this.carriedResource = 0;
        this.carriedType = null;
        this.progressTicks = 0;
    }

    public UnitType type() { return type; }
    public int ownerId() { return ownerId; }
    public double x() { return x; }
    public double y() { return y; }
    public void setPos(double x, double y) { this.x = x; this.y = y; }

    public int hp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public int shield() { return shield; }
    public void setShield(int s) { this.shield = s; }

    public UnitState state() { return state; }
    public void setState(UnitState s) { this.state = s; }

    public int cooldownLeft() { return cooldownLeft; }
    public void setCooldownLeft(int t) { this.cooldownLeft = t; }

    public long id() { return id; }
    public boolean alive() { return hp > 0 && state != UnitState.DEAD; }

    public double distanceTo(double tx, double ty) {
        double dx = x - tx;
        double dy = y - ty;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double targetX() { return targetX; }
    public double targetY() { return targetY; }
    public void setTarget(double x, double y) { this.targetX = x; this.targetY = y; }

    public long targetEntityId() { return targetEntityId; }
    public void setTargetEntityId(long id) { this.targetEntityId = id; }

    public int carriedResource() { return carriedResource; }
    public ResourceType carriedType() { return carriedType; }
    public void setCarried(ResourceType t, int amt) { this.carriedType = t; this.carriedResource = amt; }

    public int progressTicks() { return progressTicks; }
    public void setProgressTicks(int t) { this.progressTicks = t; }
}

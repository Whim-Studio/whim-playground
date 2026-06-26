package com.whim.starcraft8.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

/** Live, engine-mutated building instance. Plain mutable POJO. */
public final class Building {
    private static final AtomicLong NEXT_ID = new AtomicLong(0L);

    private final long id;
    private final BuildingType type;
    private final int ownerId;
    private final int tileX, tileY;
    private int hp;
    private BuildState state;

    private int buildProgress;
    private final Deque<UnitType> productionQueue = new ArrayDeque<UnitType>();
    private int productionTicksLeft;
    private double rallyX, rallyY;

    public Building(BuildingType type, int ownerId, int tileX, int tileY) {
        this.id = NEXT_ID.getAndIncrement();
        this.type = type;
        this.ownerId = ownerId;
        this.tileX = tileX;
        this.tileY = tileY;
        this.hp = type.maxHp();
        this.state = BuildState.UNDER_CONSTRUCTION;
        this.buildProgress = 0;
        this.productionTicksLeft = 0;
        // default rally to the building's own footprint centre
        this.rallyX = tileX + type.widthTiles() / 2.0;
        this.rallyY = tileY + type.heightTiles() / 2.0;
    }

    public BuildingType type() { return type; }
    public int ownerId() { return ownerId; }
    public int tileX() { return tileX; }
    public int tileY() { return tileY; }

    public int hp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }

    public BuildState state() { return state; }
    public void setState(BuildState s) { this.state = s; }

    public long id() { return id; }
    public boolean alive() { return hp > 0; }

    public int buildProgress() { return buildProgress; }
    public void setBuildProgress(int t) { this.buildProgress = t; }

    public Deque<UnitType> productionQueue() { return productionQueue; }

    public int productionTicksLeft() { return productionTicksLeft; }
    public void setProductionTicksLeft(int t) { this.productionTicksLeft = t; }

    public double rallyX() { return rallyX; }
    public double rallyY() { return rallyY; }
    public void setRally(double x, double y) { this.rallyX = x; this.rallyY = y; }
}

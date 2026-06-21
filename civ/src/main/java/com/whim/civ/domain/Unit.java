package com.whim.civ.domain;

/** A single mobile unit owned by a civilization. */
public final class Unit {
    private final UnitType type;
    private final int ownerCivId;
    private int x;
    private int y;
    private int movesLeft;
    private boolean veteran;
    private int hitPoints;
    private boolean fortified;

    public Unit(UnitType type, int ownerCivId, int x, int y) {
        this.type = type;
        this.ownerCivId = ownerCivId;
        this.x = x;
        this.y = y;
        this.movesLeft = type.getMovement();
        this.veteran = false;
        this.hitPoints = maxHitPoints();
        this.fortified = false;
    }

    public UnitType getType() { return type; }
    public int getOwnerCivId() { return ownerCivId; }
    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public int getMovesLeft() { return movesLeft; }   // reset each turn to type.getMovement()
    public void setMovesLeft(int m) { this.movesLeft = m; }

    public int getHitPoints() { return hitPoints; }   // start = maxHitPoints()
    public void setHitPoints(int hp) { this.hitPoints = hp; }

    public int maxHitPoints() { return veteran ? 15 : 10; }   // 10 normal, 15 veteran

    public boolean isVeteran() { return veteran; }
    public void setVeteran(boolean v) { this.veteran = v; }

    public boolean isFortified() { return fortified; }
    public void setFortified(boolean f) { this.fortified = f; }

    public boolean isAlive() { return hitPoints > 0; }
}

package com.whim.warroom.domain;

/** A live, mutable unit instance placed on the battlefield. */
public class Unit {
    private final int id;
    private final UnitType type;
    private final Faction faction;

    private Vec2 pos;
    private double heading;      // radians
    private double health;
    private double morale;
    private Stance stance = Stance.DEFENSIVE;
    private Route route;         // nullable
    private boolean routed;

    public Unit(int id, UnitType type, Faction faction, Vec2 pos) {
        this.id = id;
        this.type = type;
        this.faction = faction;
        this.pos = pos;
        this.health = type.getMaxHealth();
        this.morale = type.getMaxMorale();
    }

    public int getId() {
        return id;
    }

    public UnitType getType() {
        return type;
    }

    public Faction getFaction() {
        return faction;
    }

    public Vec2 getPos() {
        return pos;
    }

    public void setPos(Vec2 pos) {
        this.pos = pos;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getMorale() {
        return morale;
    }

    public void setMorale(double morale) {
        this.morale = morale;
    }

    public Stance getStance() {
        return stance;
    }

    public void setStance(Stance stance) {
        this.stance = stance;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public boolean isRouted() {
        return routed;
    }

    public void setRouted(boolean routed) {
        this.routed = routed;
    }

    public boolean isAlive() {
        return health > 0;
    }
}

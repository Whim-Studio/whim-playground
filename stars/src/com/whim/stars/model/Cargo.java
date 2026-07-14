package com.whim.stars.model;

import java.io.Serializable;

/**
 * A mutable bundle of cargo carried by a fleet or held on a planet's surface:
 * the three minerals plus colonists and fuel. All quantities are in kilotons
 * (kT), matching the original game's unit; colonists are stored in kT of
 * population where 1 kT = 100 colonists (Stars! convention).
 */
public final class Cargo implements Serializable {
    private static final long serialVersionUID = 1L;

    private long ironium;
    private long boranium;
    private long germanium;
    private long colonists;
    private long fuel;

    public Cargo() {
    }

    public Cargo(long ironium, long boranium, long germanium, long colonists, long fuel) {
        this.ironium = ironium;
        this.boranium = boranium;
        this.germanium = germanium;
        this.colonists = colonists;
        this.fuel = fuel;
    }

    public long ironium() { return ironium; }
    public long boranium() { return boranium; }
    public long germanium() { return germanium; }
    public long colonists() { return colonists; }
    public long fuel() { return fuel; }

    public void setIronium(long v) { ironium = Math.max(0, v); }
    public void setBoranium(long v) { boranium = Math.max(0, v); }
    public void setGermanium(long v) { germanium = Math.max(0, v); }
    public void setColonists(long v) { colonists = Math.max(0, v); }
    public void setFuel(long v) { fuel = Math.max(0, v); }

    public long get(Mineral m) {
        switch (m) {
            case IRONIUM: return ironium;
            case BORANIUM: return boranium;
            case GERMANIUM: return germanium;
            default: return 0;
        }
    }

    public void add(Mineral m, long amount) {
        switch (m) {
            case IRONIUM: setIronium(ironium + amount); break;
            case BORANIUM: setBoranium(boranium + amount); break;
            case GERMANIUM: setGermanium(germanium + amount); break;
            default: break;
        }
    }

    /** Total mineral mass (excludes colonists and fuel), used for freighter mass. */
    public long mineralMass() {
        return ironium + boranium + germanium;
    }

    public Cargo copy() {
        return new Cargo(ironium, boranium, germanium, colonists, fuel);
    }

    @Override
    public String toString() {
        return "Cargo[I=" + ironium + " B=" + boranium + " G=" + germanium
                + " col=" + colonists + " fuel=" + fuel + "]";
    }
}

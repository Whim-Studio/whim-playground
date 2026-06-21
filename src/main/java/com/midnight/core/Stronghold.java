package com.midnight.core;

/**
 * A fortified place on the map — a citadel, keep, or tower — that can be held by
 * either {@link Side} and garrisoned with defending warriors. Two strongholds
 * matter to the victory conditions: {@code Ushgarak} (Doomdark's home citadel,
 * the wargame target) and {@code Xajorkith} (the Citadel of the Moon, Luxor's
 * home, whose fall means defeat).
 */
public class Stronghold {

    private final String name;
    private final Location location;
    private final Terrain type;
    private Side owner;
    private int garrison;

    public Stronghold(String name, Location location, Terrain type, Side owner, int garrison) {
        this.name = name;
        this.location = location;
        this.type = type;
        this.owner = owner;
        this.garrison = garrison;
    }

    public String name() {
        return name;
    }

    public Location location() {
        return location;
    }

    /** The structure type: {@link Terrain#CITADEL}, {@link Terrain#KEEP}, or {@link Terrain#TOWER}. */
    public Terrain type() {
        return type;
    }

    public Side owner() {
        return owner;
    }

    public void setOwner(Side s) {
        this.owner = s;
    }

    /** Defending warriors stationed here. */
    public int garrison() {
        return garrison;
    }

    public void setGarrison(int n) {
        this.garrison = n < 0 ? 0 : n;
    }

    /** True for Doomdark's home citadel — capturing it wins the wargame for FREE. */
    public boolean isUshgarak() {
        return "Ushgarak".equals(name);
    }

    /** True for the Citadel of the Moon — losing it to DOOMDARK loses the game. */
    public boolean isXajorkith() {
        return "Xajorkith".equals(name);
    }
}

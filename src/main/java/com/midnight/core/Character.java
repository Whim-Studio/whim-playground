package com.midnight.core;

/**
 * A Lord (or army) of Midnight — a mutable agent the engine and AI move and
 * fight by mutating directly through its setters. A character belongs to a
 * {@link Side}, stands at a {@link Location} facing a {@link Direction}, and
 * carries stamina ({@code energy}), {@code courage}, the daylight hours it has
 * left to act today, and a host of foot {@code warriors} and mounted
 * {@code riders}.
 *
 * <p>Independent FREE lords start with {@link #isRecruited()} false; once a
 * recruiter reaches them they come under the player's command. Only the lord
 * named "Morkin" may bear the Ice Crown.
 */
public class Character {

    /** Maximum stamina and courage, per the contract's 0..127 range. */
    public static final int MAX_STAT = 127;

    private final String name;
    private Side side;
    private Location location;
    private Direction facing;
    private int energy;
    private int courage;
    private int hoursRemaining;
    private int warriors;
    private int riders;
    private boolean alive;
    private boolean recruited;
    private boolean carriesIceCrown;

    public Character(String name, Side side, Location location, Direction facing) {
        this.name = name;
        this.side = side;
        this.location = location;
        this.facing = facing;
        this.energy = MAX_STAT;
        this.courage = MAX_STAT;
        this.hoursRemaining = 0;
        this.warriors = 0;
        this.riders = 0;
        this.alive = true;
        this.recruited = false;
        this.carriesIceCrown = false;
    }

    public String name() {
        return name;
    }

    /** This lord's allegiance; recruited independents are {@link Side#FREE}. */
    public Side side() {
        return side;
    }

    public void setSide(Side s) {
        this.side = s;
    }

    public Location location() {
        return location;
    }

    public void setLocation(Location loc) {
        this.location = loc;
    }

    public Direction facing() {
        return facing;
    }

    public void setFacing(Direction d) {
        this.facing = d;
    }

    /** Stamina, 0..127. Drained by travel, recovered each dawn. */
    public int energy() {
        return energy;
    }

    public void setEnergy(int e) {
        this.energy = clamp(e);
    }

    public int courage() {
        return courage;
    }

    public void setCourage(int c) {
        this.courage = clamp(c);
    }

    /** Daylight hours (action points) left to spend today. */
    public int hoursRemaining() {
        return hoursRemaining;
    }

    public void setHoursRemaining(int h) {
        this.hoursRemaining = h < 0 ? 0 : h;
    }

    public int warriors() {
        return warriors;
    }

    public void setWarriors(int n) {
        this.warriors = n < 0 ? 0 : n;
    }

    public int riders() {
        return riders;
    }

    public void setRiders(int n) {
        this.riders = n < 0 ? 0 : n;
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        this.alive = false;
    }

    /** True when this lord rides — mounted lords travel faster. */
    public boolean isMounted() {
        return riders > 0;
    }

    /** True once the lord is under the player's command. */
    public boolean isRecruited() {
        return recruited;
    }

    public void setRecruited(boolean b) {
        this.recruited = b;
    }

    public boolean isMorkin() {
        return "Morkin".equals(name);
    }

    public boolean isLuxor() {
        return "Luxor".equals(name);
    }

    /** Whether this lord bears the Ice Crown (only ever true for Morkin). */
    public boolean carriesIceCrown() {
        return carriesIceCrown;
    }

    /** Set the Ice Crown borne flag; ignored for any lord other than Morkin. */
    public void setCarriesIceCrown(boolean b) {
        if (isMorkin()) {
            this.carriesIceCrown = b;
        }
    }

    private static int clamp(int v) {
        if (v < 0) {
            return 0;
        }
        if (v > MAX_STAT) {
            return MAX_STAT;
        }
        return v;
    }
}

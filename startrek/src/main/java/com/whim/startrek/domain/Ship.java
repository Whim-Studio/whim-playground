package com.whim.startrek.domain;

/**
 * A single starship. Carries TBS-relevant stats (hull/shields/energy/crew) plus a
 * live RTS battle position used only while a {@code BattleSimulator} is running.
 */
public class Ship {

    private final String name;
    private final String shipClass;
    private final Race owner;

    private int maxHull = 100;
    private int hull = 100;
    private int maxShields = 50;
    private int shields = 50;
    private int maxEnergy = 100;
    private int energy = 100;

    private boolean cloakCapable;
    private boolean cloaked;

    private int officersRequired = 1;
    private int weaponDamage = 10;
    // RTS-arena tuned defaults (arena is 800x600): without these, ships crawl and
    // never reach weapon range, so the battle view would render with no fire.
    private int weaponRange = 150;
    private double speed = 60.0; // RTS units/sec

    // RTS live-battle position (ignored on the TBS map).
    private double x;
    private double y;

    public Ship(String name, String shipClass, Race owner) {
        this.name = name;
        this.shipClass = shipClass;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public String getShipClass() {
        return shipClass;
    }

    public Race getOwner() {
        return owner;
    }

    public int getMaxHull() {
        return maxHull;
    }

    public void setMaxHull(int h) {
        this.maxHull = h;
    }

    public int getHull() {
        return hull;
    }

    public void setHull(int h) {
        this.hull = h;
    }

    public int getMaxShields() {
        return maxShields;
    }

    public void setMaxShields(int s) {
        this.maxShields = s;
    }

    public int getShields() {
        return shields;
    }

    public void setShields(int s) {
        this.shields = s;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public void setMaxEnergy(int e) {
        this.maxEnergy = e;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int e) {
        this.energy = e;
    }

    public boolean isCloakCapable() {
        return cloakCapable;
    }

    public void setCloakCapable(boolean b) {
        this.cloakCapable = b;
        if (!b) {
            this.cloaked = false;
        }
    }

    public boolean isCloaked() {
        return cloaked;
    }

    public void setCloaked(boolean b) {
        // Only cloak-capable ships can actually cloak.
        this.cloaked = b && cloakCapable;
    }

    /** Ship cannot move/fight without crew. */
    public int getOfficersRequired() {
        return officersRequired;
    }

    public void setOfficersRequired(int n) {
        this.officersRequired = n;
    }

    public int getWeaponDamage() {
        return weaponDamage;
    }

    public void setWeaponDamage(int d) {
        this.weaponDamage = d;
    }

    public int getWeaponRange() {
        return weaponRange;
    }

    public void setWeaponRange(int r) {
        this.weaponRange = r;
    }

    /** RTS speed in units/sec. */
    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double s) {
        this.speed = s;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** A ship is destroyed once its hull is depleted. */
    public boolean isDestroyed() {
        return hull <= 0;
    }
}

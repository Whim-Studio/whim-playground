package com.rampart.model;

/**
 * A player cannon placed on enclosed land. Carries reload/cooldown and ammo
 * counters plus setters, but performs NO firing, targeting, or trajectory logic —
 * the engine (Task 2) drives all of that and writes the counters back here.
 */
public class Cannon implements CannonView {
    private final Coord position;
    private long reloadRemainingMillis;
    private int ammo;
    private boolean alive = true;

    /**
     * Creates a ready cannon with the default starting ammo from {@link Rules}.
     *
     * @param position the cannon's grid cell (must be non-null)
     */
    public Cannon(Coord position) {
        this(position, Rules.CANNON_START_AMMO);
    }

    /**
     * Creates a ready cannon with explicit ammo.
     *
     * @param position the cannon's grid cell (must be non-null)
     * @param ammo     starting ammo ({@code -1} == unlimited)
     */
    public Cannon(Coord position, int ammo) {
        if (position == null) throw new IllegalArgumentException("position must not be null");
        this.position = position;
        this.ammo = ammo;
    }

    @Override public Coord position() { return position; }
    @Override public long reloadRemainingMillis() { return reloadRemainingMillis; }
    @Override public int ammo() { return ammo; }
    @Override public boolean alive() { return alive; }

    @Override
    public boolean ready() {
        return alive && reloadRemainingMillis <= 0L && ammo != 0;
    }

    /**
     * Sets the remaining reload time (engine only). Clamped at zero.
     *
     * @param millis remaining reload in milliseconds
     */
    public void setReloadRemainingMillis(long millis) {
        this.reloadRemainingMillis = Math.max(0L, millis);
    }

    /**
     * Decrements the reload timer by the elapsed time (engine tick helper). Clamped
     * at zero. Does not fire anything.
     *
     * @param dtMillis elapsed milliseconds since the last tick
     */
    public void decReload(long dtMillis) {
        reloadRemainingMillis = Math.max(0L, reloadRemainingMillis - Math.max(0L, dtMillis));
    }

    /**
     * Sets ammunition (engine only).
     *
     * @param ammo new ammo count ({@code -1} == unlimited)
     */
    public void setAmmo(int ammo) { this.ammo = ammo; }

    /** @return {@code true} if this cannon has unlimited ammo */
    public boolean unlimitedAmmo() { return ammo < 0; }

    /**
     * Sets whether the cannon is still intact (engine only).
     *
     * @param alive new alive state
     */
    public void setAlive(boolean alive) { this.alive = alive; }

    @Override
    public String toString() {
        return "Cannon" + position + ",reload=" + reloadRemainingMillis + ",ammo=" + ammo;
    }
}

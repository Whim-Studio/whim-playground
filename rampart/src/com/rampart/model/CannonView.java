package com.rampart.model;

/**
 * Read-only snapshot of a placed cannon. Concrete class: {@link Cannon}. Reload
 * countdown and ammo are surfaced so the HUD can render readiness; the engine owns
 * the actual firing.
 */
public interface CannonView {
    /** @return the cannon's grid position */
    Coord position();

    /** @return remaining reload time in milliseconds ({@code 0} == ready to fire) */
    long reloadRemainingMillis();

    /** @return {@code true} if the cannon is off cooldown and has ammo */
    boolean ready();

    /** @return remaining ammunition, or {@code -1} for unlimited */
    int ammo();

    /** @return {@code true} while this cannon has not been destroyed */
    boolean alive();
}

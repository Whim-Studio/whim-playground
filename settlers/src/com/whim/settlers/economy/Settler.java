package com.whim.settlers.economy;

/**
 * An individual settler with a {@link Profession} and a simple activity state.
 * Phase 3 uses settlers as building staff (transport is stubbed instant); Phase
 * 4 gives carriers real positions and flag-relay movement, at which point this
 * class grows a tile position and path. Kept per-settler (not a bare counter) so
 * that job-specific behaviour and appearance have a home.
 */
public final class Settler {

    public enum State { IDLE, WORKING, WALKING }

    private Profession profession;
    private State state;

    public Settler(Profession profession) {
        this.profession = profession;
        this.state = profession == Profession.IDLE ? State.IDLE : State.WORKING;
    }

    public Profession profession() { return profession; }
    public State state()           { return state; }

    public void assign(Profession p) {
        this.profession = p;
        this.state = State.WORKING;
    }
}

package com.midnight.ai;

import com.midnight.core.Location;
import com.midnight.core.Side;

/**
 * The outcome of a single night battle at one tile. Owned by Task 2
 * ({@code com.midnight.ai}); declared here only so {@link NightReport} and the
 * core's {@code endDay} signature can compile and be tested in isolation.
 */
public final class BattleResult {

    private final Location where;
    private final Side victor;
    private final int freeLosses;
    private final int doomdarkLosses;
    private final String text;

    public BattleResult(Location where, Side victor, int freeLosses, int doomdarkLosses, String text) {
        this.where = where;
        this.victor = victor;
        this.freeLosses = freeLosses;
        this.doomdarkLosses = doomdarkLosses;
        this.text = text;
    }

    public Location where() {
        return where;
    }

    /** The winning side, or {@code null} if the clash was indecisive. */
    public Side victor() {
        return victor;
    }

    public int freeLosses() {
        return freeLosses;
    }

    public int doomdarkLosses() {
        return doomdarkLosses;
    }

    public String text() {
        return text;
    }
}

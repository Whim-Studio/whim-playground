package com.midnight.ai;

import com.midnight.core.Location;
import com.midnight.core.Side;

/**
 * Immutable record of one resolved battle: where it happened, who prevailed,
 * how many soldiers each side lost, and a plain-English description for the UI.
 */
public final class BattleResult {

    private final Location where;
    private final Side victor;
    private final int freeLosses;
    private final int doomdarkLosses;
    private final String text;

    /**
     * @param where          the battlefield location
     * @param victor         the winning side, or {@code null} when indecisive
     * @param freeLosses     FREE soldiers lost (warriors + riders + garrison)
     * @param doomdarkLosses DOOMDARK soldiers lost
     * @param text           plain-English summary
     */
    public BattleResult(Location where, Side victor, int freeLosses, int doomdarkLosses, String text) {
        this.where = where;
        this.victor = victor;
        this.freeLosses = freeLosses;
        this.doomdarkLosses = doomdarkLosses;
        this.text = text == null ? "" : text;
    }

    public Location where() {
        return where;
    }

    /** The winning side, or {@code null} when the clash was indecisive. */
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

    @Override
    public String toString() {
        return text;
    }
}

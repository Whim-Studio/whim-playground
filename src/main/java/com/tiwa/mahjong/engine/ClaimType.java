package com.tiwa.mahjong.engine;

/**
 * The kinds of claim a player may make on another player's discard (Section 2-3 of the rulebook).
 *
 * <p>Note that a CHOW can never be claimed from a discard, so it is deliberately absent here.
 * Claim priority is strict: {@link #MAHJONG} &gt; {@link #KONG} &gt; {@link #PUNG}.</p>
 *
 * <p>{@link #KONG} appears as a priority level so the generic {@link ClaimResolver} can rank it,
 * but forming a kong directly from a discard is itself forbidden - see
 * {@link ClaimRules#canClaimFromDiscard} and {@link KongRules}.</p>
 */
public enum ClaimType {
    MAHJONG(3),
    KONG(2),
    PUNG(1);

    private final int priority;

    ClaimType(int priority) {
        this.priority = priority;
    }

    /** Higher wins. Mahjong (3) &gt; Kong (2) &gt; Pung (1). */
    public int priority() {
        return priority;
    }
}

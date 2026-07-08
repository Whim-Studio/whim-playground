package com.whim.necromunda.engine;

/**
 * The five phases of an active player's turn, in strict order. RECOVERY and END
 * are engine-automatic (resolved as soon as they are entered); MOVEMENT,
 * SHOOTING and CLOSE_COMBAT are player-driven (the player ends them explicitly).
 */
public enum Phase {
    RECOVERY("Recovery", true),
    MOVEMENT("Movement", false),
    SHOOTING("Shooting", false),
    CLOSE_COMBAT("Close Combat", false),
    END("End", true);

    private final String label;
    private final boolean automatic;

    Phase(String label, boolean automatic) {
        this.label = label;
        this.automatic = automatic;
    }

    public String label() { return label; }

    /** True if the engine resolves this phase automatically (no player action). */
    public boolean isAutomatic() { return automatic; }

    /** The next phase in the cycle; {@code END} wraps back to {@code RECOVERY}. */
    public Phase next() {
        switch (this) {
            case RECOVERY:     return MOVEMENT;
            case MOVEMENT:     return SHOOTING;
            case SHOOTING:     return CLOSE_COMBAT;
            case CLOSE_COMBAT: return END;
            case END:          return RECOVERY;
            default:           return RECOVERY;
        }
    }

    /** True if advancing from this phase completes the active player's turn. */
    public boolean isLastInTurn() {
        return this == END;
    }
}

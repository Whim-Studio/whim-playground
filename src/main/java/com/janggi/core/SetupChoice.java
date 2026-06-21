package com.janggi.core;

/**
 * Pre-game Elephant/Horse transposition. The four arrangements describe the two
 * minor pieces on each wing of the back rank, read across columns [1, 2, 6, 7].
 * Letters: M = HORSE, S = ELEPHANT. MSSM is the orthodox "both elephants inside".
 */
public final class SetupChoice {

    public enum Arrangement {
        MSSM, SMMS, MSMS, SMSM;
    }

    private final Arrangement arrangement;

    public SetupChoice(Arrangement arrangement) {
        this.arrangement = arrangement;
    }

    public Arrangement arrangement() {
        return arrangement;
    }

    @Override
    public String toString() {
        return "SetupChoice(" + arrangement + ")";
    }
}

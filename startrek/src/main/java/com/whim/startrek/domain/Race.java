package com.whim.startrek.domain;

/**
 * Playable / NPC civilizations. Civilization level (1..5) drives the starting
 * tech-point pool ({@code civLevel * 10}, of a 50 max). Each race has a per-tree
 * maximum tech level it can ever research, given as six caps in TechType order:
 * BIOTECH, PROPULSION, WEAPON, CONSTRUCTION, ENERGY, COMPUTER.
 */
public enum Race {
    // name, civLevel(1..5), capBio, capProp, capWeap, capConst, capEnergy, capComp
    AKAALI    (1, 3, 3, 2, 2, 2, 3),
    OCAMPA    (1, 4, 2, 2, 2, 3, 2),
    // Intermediate (level 2-4) races — optional additions per the contract.
    XINDI     (2, 5, 4, 5, 4, 4, 4),
    THOLIAN   (3, 5, 6, 6, 6, 7, 6),
    BREEN     (4, 6, 7, 7, 8, 6, 6),
    // The five major level-5 powers.
    FEDERATION(5, 7, 10, 6, 7, 8, 9),
    DOMINION  (5, 10, 7, 10, 10, 6, 5),
    KLINGON   (5, 6, 8, 10, 7, 7, 5),
    ROMULAN   (5, 6, 9, 8, 7, 7, 8);

    private final int civLevel;
    private final int[] caps; // indexed by TechType.ordinal()

    Race(int civLevel, int bio, int prop, int weap, int cons, int energy, int comp) {
        this.civLevel = civLevel;
        this.caps = new int[] { bio, prop, weap, cons, energy, comp };
    }

    /** Civilization level, 1..5. */
    public int getCivLevel() {
        return civLevel;
    }

    /** Starting tech-point pool: civLevel * 10, capped at 50. */
    public int getTechPointPool() {
        int pool = civLevel * 10;
        return pool > 50 ? 50 : pool;
    }

    /** Per-tree maximum achievable tech level for this race. */
    public int getCap(TechType t) {
        return caps[t.ordinal()];
    }
}

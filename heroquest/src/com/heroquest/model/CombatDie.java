package com.heroquest.model;

/**
 * The six faces of a HeroQuest combat die:
 * 3 Skulls (hits), 2 White Shields (hero defence), 1 Black Shield (monster defence).
 */
public enum CombatDie {
    SKULL,
    WHITE_SHIELD,
    BLACK_SHIELD;

    /** The physical face layout of a single HeroQuest combat die, in face order 1..6. */
    public static final CombatDie[] FACES = {
        SKULL, SKULL, SKULL,
        WHITE_SHIELD, WHITE_SHIELD,
        BLACK_SHIELD
    };
}

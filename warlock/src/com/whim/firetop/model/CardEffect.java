package com.whim.firetop.model;

/**
 * The resolvable outcome of drawing a {@link Card}. The engine dispatches on
 * this enum to apply the card's mechanical effect to the active adventurer.
 */
public enum CardEffect {
    /** Gain gold equal to magnitude. */
    GAIN_GOLD,
    /** Lose STAMINA equal to magnitude. */
    LOSE_STAMINA,
    /** Restore STAMINA equal to magnitude (up to initial). */
    RESTORE_STAMINA,
    /** Restore LUCK equal to magnitude (up to initial). */
    RESTORE_LUCK,
    /** Permanently raise current SKILL by magnitude (up to initial). */
    GAIN_SKILL,
    /** Receive a healing potion item (magnitude = heal amount). */
    GAIN_POTION,
    /** Receive extra provisions equal to magnitude. */
    GAIN_PROVISION,
    /** Receive a treasure item worth magnitude gold. */
    GAIN_TREASURE,
    /** A trap: Test Luck; Unlucky loses magnitude STAMINA, Lucky avoids it. */
    TEST_LUCK_TRAP,
    /** Spawn a monster to fight (encounter deck). */
    ENCOUNTER_MONSTER,
    /** Nothing happens (a red herring). */
    NOTHING
}

package com.whim.firetop.model;

/** Which of the three decks a {@link Card} belongs to. */
public enum CardType {
    /** Monsters that ambush the party. */
    ENCOUNTER,
    /** Gold, potions, weapons, keys and artefacts. */
    TREASURE,
    /** Traps, boons and dungeon happenings. */
    EVENT
}

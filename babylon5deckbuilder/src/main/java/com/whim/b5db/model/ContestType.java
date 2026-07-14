package com.whim.b5db.model;

/**
 * The four attribute / conflict lanes carried over from the B5 CCG stats.
 * Every attribute is a per-turn pool; DIPLOMACY additionally converts to
 * INFLUENCE (see the GDD economy rules).
 */
public enum ContestType {
    DIPLOMACY,
    INTRIGUE,
    MILITARY,
    PSI
}

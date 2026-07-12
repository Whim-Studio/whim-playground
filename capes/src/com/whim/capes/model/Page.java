package com.whim.capes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A Page (p.22): a unit of narration within a Scene. Each Page has a Starter
 * (advances one seat clockwise each Page) and runs the fixed phase order:
 * Overdrawn checks &rarr; Claim/add &rarr; free narration &rarr; Actions &rarr;
 * Resolve. Super Abilities may be used once per Page; this record supports the
 * engine's per-Page bookkeeping.
 */
public final class Page implements java.io.Serializable {
    /** Ordered phases of a Page (see turn-structure diagram). */
    public enum Phase { OVERDRAWN_CHECK, CLAIM, FREE_NARRATION, ACTIONS, RESOLVE, DONE }

    private final int number;
    private final String starterPlayerId;
    private Phase phase = Phase.OVERDRAWN_CHECK;

    /** Keys "characterId#abilityName" of super Abilities already used this Page (p.38). */
    private final List<String> usedSuperAbilityKeys = new ArrayList<String>();

    public Page(int number, String starterPlayerId) {
        this.number = number;
        this.starterPlayerId = starterPlayerId;
    }

    public int number() { return number; }
    public String starterPlayerId() { return starterPlayerId; }
    public Phase phase() { return phase; }
    public void setPhase(Phase phase) { this.phase = phase; }

    public boolean isSuperAbilityUsed(String characterId, String abilityName) {
        return usedSuperAbilityKeys.contains(characterId + "#" + abilityName);
    }
    public void markSuperAbilityUsed(String characterId, String abilityName) {
        String key = characterId + "#" + abilityName;
        if (!usedSuperAbilityKeys.contains(key)) usedSuperAbilityKeys.add(key);
    }
}

package com.whim.capes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A Scene (p.20): a place and time. Declared by the player to the left of the
 * previous declarer; players then choose roles. A Scene runs a sequence of
 * {@link Page}s. Mundane Abilities "Block" for the duration of a Scene, so the
 * Scene tracks which have been used.
 */
public final class Scene implements java.io.Serializable {
    private final int number;
    private final String declaringPlayerId;
    private String title;
    private final List<Page> pages = new ArrayList<Page>();

    /** Live Conflicts on the table this Scene (carry across Pages until Resolved). */
    private final List<Conflict> conflicts = new ArrayList<Conflict>();

    /** Keys "characterId#abilityName" of mundane Abilities already used this Scene (Block, p.38). */
    private final List<String> blockedAbilityKeys = new ArrayList<String>();

    public Scene(int number, String declaringPlayerId, String title) {
        this.number = number;
        this.declaringPlayerId = declaringPlayerId;
        this.title = title;
    }

    public int number() { return number; }
    public String declaringPlayerId() { return declaringPlayerId; }
    public String title() { return title; }
    public void setTitle(String t) { this.title = t; }

    public List<Page> pages() { return pages; }
    public List<Conflict> conflicts() { return conflicts; }

    public boolean isAbilityBlocked(String characterId, String abilityName) {
        return blockedAbilityKeys.contains(characterId + "#" + abilityName);
    }
    public void blockAbility(String characterId, String abilityName) {
        String key = characterId + "#" + abilityName;
        if (!blockedAbilityKeys.contains(key)) blockedAbilityKeys.add(key);
    }
}

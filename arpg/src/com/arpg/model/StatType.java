package com.arpg.model;

/**
 * The kinds of numeric modifiers an item, buff, or attribute contribution can
 * apply to a {@link CombatParticipant}. Pure data enum — no behaviour.
 */
public enum StatType {
    STRENGTH("Strength"),
    AGILITY("Agility"),
    INTELLECT("Intellect"),
    VITALITY("Vitality"),
    MAX_HEALTH("Max Health"),
    MAX_RESOURCE("Max Resource"),
    ATTACK_POWER("Attack Power"),
    SPELL_POWER("Spell Power"),
    ARMOR("Armor"),
    CRIT_CHANCE("Crit Chance");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

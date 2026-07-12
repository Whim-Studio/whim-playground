package com.whim.firetop.model;

import java.io.Serializable;

/**
 * A dungeon creature. Has a fixed SKILL and a STAMINA pool that is depleted
 * during combat. When STAMINA reaches zero the monster is defeated.
 */
public final class Monster implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final int skill;
    private int stamina;
    private final int maxStamina;
    private final String description;

    /**
     * @param name        display name
     * @param skill       fixed combat SKILL
     * @param stamina     starting (and maximum) STAMINA
     * @param description original flavor text
     */
    public Monster(String name, int skill, int stamina, String description) {
        this.name = name;
        this.skill = skill;
        this.stamina = stamina;
        this.maxStamina = stamina;
        this.description = description;
    }

    public String getName() { return name; }
    public int getSkill() { return skill; }
    public int getStamina() { return stamina; }
    public int getMaxStamina() { return maxStamina; }
    public String getDescription() { return description; }

    /** Applies damage, clamping STAMINA at zero. */
    public void wound(int amount) {
        stamina -= amount;
        if (stamina < 0) {
            stamina = 0;
        }
    }

    /** Heals the monster (used by the "graze" luck result), never above max. */
    public void heal(int amount) {
        stamina += amount;
        if (stamina > maxStamina) {
            stamina = maxStamina;
        }
    }

    public boolean isDefeated() { return stamina <= 0; }

    /** A fresh copy at full STAMINA (so a shared template can spawn many foes). */
    public Monster copy() {
        return new Monster(name, skill, maxStamina, description);
    }
}

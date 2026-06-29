package com.tiwas.rpg.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for player-forged Advanced Skills (the "Epiphany" born from a failed
 * doubles roll). An Advanced Skill is an ordinary {@link Skill} marked
 * {@code advanced = true} so it round-trips through the existing JSON skill
 * list: its tier is the base skill's tier + 1, and its formula is the base
 * skill's attribute codes plus exactly ONE additional attribute chosen by the
 * player.
 */
public final class AdvancedSkill {

    private AdvancedSkill() {
    }

    /**
     * Build an Advanced Skill from a base skill and the player's choices.
     *
     * @param base       the skill the epiphany sprang from
     * @param extraCode  the one additional attribute code to add to the formula
     * @param name       the player-chosen name (falls back to a default if blank)
     * @param startValue the chosen starting value (clamped to {@code [1, maxCap]}
     *                   against the owner once the formula is known)
     * @param owner      the character, used to clamp the start value to the cap
     * @param weaponClass optional weapon class carried over (nullable)
     */
    public static Skill create(Skill base, String extraCode, String name,
                               int startValue, Character owner, String weaponClass) {
        if (base == null) {
            throw new IllegalArgumentException("base must not be null");
        }
        int newTier = base.getTier() + 1;

        List<String> codes = new ArrayList<String>(base.getAttributeCodes());
        if (extraCode != null && extraCode.trim().length() > 0) {
            codes.add(extraCode.trim().toLowerCase());
        }

        String finalName = (name == null || name.trim().length() == 0)
                ? base.getName() + " Mastery"
                : name.trim();

        Skill skill = new Skill(finalName, newTier, codes, 0);
        skill.setAdvanced(true);
        if (weaponClass != null && weaponClass.trim().length() > 0) {
            skill.setWeaponClass(weaponClass.trim());
        }

        int cap = owner == null ? startValue : skill.maxCap(owner);
        int v = startValue;
        if (v < 1) {
            v = 1;
        }
        if (owner != null && v > cap) {
            v = cap < 1 ? 1 : cap;
        }
        skill.setValue(v);
        return skill;
    }
}

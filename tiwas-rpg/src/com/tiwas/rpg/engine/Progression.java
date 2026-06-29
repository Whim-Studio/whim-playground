package com.tiwas.rpg.engine;

import com.tiwas.rpg.domain.Character;
import com.tiwas.rpg.domain.Skill;

/**
 * Applies the "Failing Forward" XP progression and the "Advanced Skill"
 * (Epiphany) system to a character after a skill test.
 *
 * <p>{@link ActionResolver} computes the raw outcome (roll, doubles,
 * advanced-skill unlock) but deliberately mutates nothing about a character's
 * growth. This class is that missing application layer: it raises the failed
 * skill via a temporary Skill Roll Pool, cascades level-ups, and deposits the
 * leftover into the General XP Pool. On a failed doubles it does NOT invent the
 * Advanced Skill itself — it only flags that an Epiphany is pending; the player
 * forges the skill interactively (see {@link com.tiwas.rpg.domain.AdvancedSkill}
 * and the UI dialog). All math rounds DOWN.
 *
 * <p>Per the user brief, Failure XP is computed against the skill's
 * <em>base</em> value ({@link Skill#getValue()}), NOT the DM-modified effective
 * skill that {@link ActionResult#getFailureXP()} reports.
 */
public final class Progression {

    private Progression() {
    }

    /**
     * Apply growth from a single resolved action. No-op (empty outcome) on
     * success. Mutates {@code skill} (its value) and {@code actor} (general XP).
     * Does NOT create the Advanced Skill — when an Epiphany is unlocked the
     * outcome simply reports it as pending and carries the base skill so the
     * caller can drive the interactive forge.
     */
    public static ProgressionOutcome apply(Character actor, Skill skill, ActionResult result) {
        if (actor == null || skill == null || result == null) {
            throw new IllegalArgumentException("actor, skill and result must not be null");
        }
        ProgressionOutcome out = new ProgressionOutcome(skill.getName());
        if (result.isSuccess()) {
            return out;
        }

        // Rule 1 — Failure XP from the BASE skill value, clamped to >= 0.
        int failureXP = Math.max(0, result.getRoll() - skill.getValue());
        out.failureXP = failureXP;
        out.oldValue = skill.getValue();
        out.newValue = skill.getValue();

        // Rule 2 — apply via a temporary Skill Roll Pool, cascading level-ups.
        // Cost to raise by 1 = the skill's CURRENT value (re-read each step), so
        // the price rises as it levels. Capped at the skill's attribute cap.
        int pool = failureXP;
        int cap = skill.maxCap(actor);
        while (true) {
            int current = skill.getValue();
            if (current >= cap) {
                break; // at cap: no further leveling, leftover spills to General XP
            }
            int cost = current <= 0 ? 1 : current; // guard: a 0-value skill costs 1
            if (pool < cost) {
                break;
            }
            pool -= cost;
            skill.setValue(current + 1);
            out.levelsGained++;
        }
        out.newValue = skill.getValue();

        // Rule 3 — deposit whatever the pool could not spend into the General XP Pool.
        if (pool > 0) {
            actor.setGeneralXP(actor.getGeneralXP() + pool);
            out.remainderToGeneral = pool;
        }

        // Rule 4 — Epiphany: a failed doubles roll UNLOCKS a Tier+1 Advanced
        // Skill, but the player forges it interactively. Flag it as pending and
        // carry the base skill; the caller pops the forge dialog.
        if (result.isUnlockedAdvancedSkill()) {
            out.epiphanyPending = true;
            out.baseSkill = skill;
        }
        return out;
    }
}

package com.tiwas.rpg.engine;

import java.util.ArrayList;
import java.util.List;

import com.tiwas.rpg.domain.AttributeCode;
import com.tiwas.rpg.domain.Character;
import com.tiwas.rpg.domain.Skill;

/**
 * Applies the "Failing Forward" XP progression and the "Advanced Skill"
 * (Epiphany) system to a character after a skill test.
 *
 * <p>{@link ActionResolver} computes the raw outcome (roll, doubles,
 * advanced-skill unlock) but deliberately mutates nothing about a character's
 * growth. This class is that missing application layer: it raises the failed
 * skill via a temporary Skill Roll Pool, cascades level-ups, deposits the
 * leftover into the General XP Pool, and — on a failed doubles — invents a new
 * Tier+1 Advanced Skill. All math rounds DOWN.
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
     * success. Mutates {@code skill} (its value), {@code actor} (general XP and,
     * on epiphany, a new skill).
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

        // Rule 4 — Epiphany: a failed doubles roll invents a Tier+1 Advanced Skill.
        if (result.isUnlockedAdvancedSkill()) {
            Skill advanced = createAdvancedSkill(actor, skill);
            if (advanced != null) {
                actor.putSkill(advanced);
                out.createdSkill = advanced;
            }
        }
        return out;
    }

    /**
     * Forge a Tier+1 Advanced Skill from the failed skill: its formula gains one
     * extra attribute (the character's strongest in the same Body/Mind group not
     * already in the formula). Starting value = its cap / 2; a weapon-linked
     * source carries its weapon class and gets +5 for focused training.
     */
    private static Skill createAdvancedSkill(Character actor, Skill base) {
        int newTier = base.getTier() + 1;
        List<String> codes = new ArrayList<String>(base.getAttributeCodes());
        AttributeCode extra = pickExtraAttribute(actor, base, codes);
        if (extra != null) {
            codes.add(extra.code());
        }

        Skill advanced = new Skill(uniqueName(actor, base), newTier, codes, 0);
        int startValue = advanced.maxCap(actor) / 2;
        if (base.getWeaponClass() != null) {
            advanced.setWeaponClass(base.getWeaponClass());
            startValue += 5; // weapon-specific Advanced Skills train faster (Section 10)
        }
        advanced.setValue(startValue);
        return advanced;
    }

    /** Highest-value attribute in the source skill's group not already used. */
    private static AttributeCode pickExtraAttribute(Character actor, Skill base, List<String> used) {
        boolean mind = base.isMind();
        List<AttributeCode> group = mind ? AttributeCode.mindAttributes() : AttributeCode.bodyAttributes();
        AttributeCode best = null;
        int bestValue = -1;
        for (AttributeCode a : group) {
            if (used.contains(a.code())) {
                continue;
            }
            int v = actor.getAttribute(a);
            if (v > bestValue) {
                bestValue = v;
                best = a;
            }
        }
        return best;
    }

    /** A skill name unique within the character's current skill set. */
    private static String uniqueName(Character actor, Skill base) {
        String root = base.getName() + " Mastery";
        if (actor.getSkill(root) == null) {
            return root;
        }
        int n = 2;
        while (actor.getSkill(root + " " + n) != null) {
            n++;
        }
        return root + " " + n;
    }
}

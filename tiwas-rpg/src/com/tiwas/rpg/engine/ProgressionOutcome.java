package com.tiwas.rpg.engine;

import com.tiwas.rpg.domain.Skill;

/**
 * What the "Failing Forward" / "Advanced Skill" application did to a character
 * for a single failed skill test. Pure data — produced by {@link Progression},
 * read by the UI for logging. A success produces an empty outcome.
 */
public final class ProgressionOutcome {

    private final String skillName;
    int failureXP;
    int oldValue;
    int newValue;
    int levelsGained;
    int remainderToGeneral;
    boolean epiphanyPending;
    Skill baseSkill; // nullable; the skill the pending epiphany sprang from

    ProgressionOutcome(String skillName) {
        this.skillName = skillName;
    }

    public String getSkillName() { return skillName; }
    public int getFailureXP() { return failureXP; }
    public int getOldValue() { return oldValue; }
    public int getNewValue() { return newValue; }
    public int getLevelsGained() { return levelsGained; }
    public int getRemainderToGeneral() { return remainderToGeneral; }

    /** True when a failed doubles roll unlocked an Advanced Skill the player must forge. */
    public boolean isEpiphanyPending() { return epiphanyPending; }

    /** The skill the pending epiphany derives from, or null when none is pending. */
    public Skill getBaseSkill() { return baseSkill; }

    /** True if anything at all happened (XP earned or an epiphany unlocked). */
    public boolean isAnything() {
        return failureXP > 0 || epiphanyPending;
    }

    /** Human-readable summary, or null when nothing happened. */
    public String describe() {
        if (!isAnything()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Failing Forward: +").append(failureXP).append(" XP");
        if (levelsGained > 0) {
            sb.append(" -> ").append(skillName).append(' ')
              .append(oldValue).append(" => ").append(newValue)
              .append(" (+").append(levelsGained).append(')');
        } else {
            sb.append(" into ").append(skillName).append(" (no level)");
        }
        if (remainderToGeneral > 0) {
            sb.append(", ").append(remainderToGeneral).append(" to General XP Pool");
        }
        if (epiphanyPending) {
            sb.append("; EPIPHANY unlocked — forge a new Advanced Skill!");
        }
        return sb.toString();
    }
}

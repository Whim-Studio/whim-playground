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
    Skill createdSkill; // nullable

    ProgressionOutcome(String skillName) {
        this.skillName = skillName;
    }

    public String getSkillName() { return skillName; }
    public int getFailureXP() { return failureXP; }
    public int getOldValue() { return oldValue; }
    public int getNewValue() { return newValue; }
    public int getLevelsGained() { return levelsGained; }
    public int getRemainderToGeneral() { return remainderToGeneral; }
    public Skill getCreatedSkill() { return createdSkill; }

    /** True if anything at all happened (XP earned or an advanced skill born). */
    public boolean isAnything() {
        return failureXP > 0 || createdSkill != null;
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
        if (createdSkill != null) {
            sb.append("; EPIPHANY! invented \"").append(createdSkill.getName())
              .append("\" (Tier ").append(createdSkill.getTier())
              .append(", value ").append(createdSkill.getValue()).append(')');
        }
        return sb.toString();
    }
}

package com.whim.coda.model;

/**
 * A skill possessed at a rank, optionally with a specialty.
 * Base-skill uniqueness rule: the SAME Skill name may appear MULTIPLE times only if specialties differ.
 */
public class SkillRank {

    private final Skill skill;
    private final int rank;
    private final String specialty;

    public SkillRank(Skill skill, int rank, String specialty) {
        this.skill = skill;
        this.rank = rank;
        this.specialty = specialty;
    }

    public Skill getSkill() {
        return skill;
    }

    public int getRank() {
        return rank;
    }

    /** May be null/empty. */
    public String getSpecialty() {
        return specialty;
    }
}

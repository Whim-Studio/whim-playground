package com.whim.coda.engine;

import com.whim.coda.model.CharacterSheet;
import com.whim.coda.model.Edge;
import com.whim.coda.model.Flaw;
import com.whim.coda.model.Skill;
import com.whim.coda.model.SkillRank;

/**
 * Builds free-form Custom Personal / Custom Professional packages. There are NO
 * predefined package lists: the caller chooses any skills, edges, and flaws.
 *
 * <p>Skill rule: the same BASE skill name may not repeat UNLESS specialties
 * differ. Two ranks with the same base skill and the same (normalized) specialty
 * collide; ranks with distinct specialties may coexist.</p>
 */
public final class PackageBuilder {

    private PackageBuilder() {
    }

    /**
     * Add a skill rank. Throws {@link IllegalArgumentException} if it duplicates
     * an existing BASE skill WITHOUT a distinct specialty (same skill name allowed
     * only when specialties differ).
     */
    public static void addSkill(CharacterSheet sheet, SkillRank rank) {
        if (sheet == null) {
            throw new IllegalArgumentException("sheet must not be null");
        }
        if (rank == null || rank.getSkill() == null) {
            throw new IllegalArgumentException("rank and its skill must not be null");
        }
        if (!canAddSkill(sheet, rank.getSkill(), rank.getSpecialty())) {
            String specialty = normalize(rank.getSpecialty());
            String skillName = rank.getSkill().getName();
            if (specialty.isEmpty()) {
                throw new IllegalArgumentException(
                        "Skill \"" + skillName + "\" already present; add a distinct specialty to repeat it.");
            }
            throw new IllegalArgumentException(
                    "Skill \"" + skillName + "\" with specialty \"" + rank.getSpecialty()
                            + "\" already present.");
        }
        sheet.getSkills().add(rank);
    }

    /**
     * Predicate form of {@link #addSkill}: returns {@code true} when a rank for
     * {@code skill} with {@code specialty} could be added without violating the
     * base-skill uniqueness rule.
     */
    public static boolean canAddSkill(CharacterSheet sheet, Skill skill, String specialty) {
        if (sheet == null || skill == null) {
            return false;
        }
        String skillKey = key(skill.getName());
        String specKey = normalize(specialty);
        for (SkillRank existing : sheet.getSkills()) {
            if (existing == null || existing.getSkill() == null) {
                continue;
            }
            if (!key(existing.getSkill().getName()).equals(skillKey)) {
                continue;
            }
            // Same base skill: only allowed when specialties differ.
            if (normalize(existing.getSpecialty()).equals(specKey)) {
                return false;
            }
        }
        return true;
    }

    /** Add an Edge to the sheet. */
    public static void addEdge(CharacterSheet sheet, Edge e) {
        if (sheet == null) {
            throw new IllegalArgumentException("sheet must not be null");
        }
        if (e == null) {
            throw new IllegalArgumentException("edge must not be null");
        }
        sheet.getEdges().add(e);
    }

    /** Add a Flaw to the sheet. */
    public static void addFlaw(CharacterSheet sheet, Flaw f) {
        if (sheet == null) {
            throw new IllegalArgumentException("sheet must not be null");
        }
        if (f == null) {
            throw new IllegalArgumentException("flaw must not be null");
        }
        sheet.getFlaws().add(f);
    }

    private static String normalize(String specialty) {
        if (specialty == null) {
            return "";
        }
        return specialty.trim().toLowerCase();
    }

    private static String key(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase();
    }
}

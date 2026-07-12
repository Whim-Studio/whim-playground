package com.whim.capes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A Capes character (Ch.3). Built by either Freeform (p.72) or Click-and-Lock
 * (p.80); both converge on the same shape:
 * <ul>
 *   <li>Up to 12 Abilities in three columns; each column holds 3-5 Abilities,
 *       numbered sequentially from 1 (p.72).</li>
 *   <li>Super-powered characters use POWER + ATTITUDE + STYLE; mundane
 *       characters use SKILL + ATTITUDE + STYLE (p.69).</li>
 * </ul>
 *
 * <p>Drives (p.74):
 * <ul>
 *   <li><b>Detailed</b>: five Drives with Strengths 1-5 totaling exactly 9.</li>
 *   <li><b>Undifferentiated</b>: a single Debt stack; Overdrawn above 5; may
 *       Stake up to 3 on any one Conflict (p.74). Modeled with
 *       {@code undifferentiated == true} and a single pseudo-Drive.</li>
 * </ul>
 * Total Debt at rest across a character's Drives may not exceed 9 (p.130).
 */
public final class Character implements java.io.Serializable {
    public static final int MAX_DEBT_TOTAL = 9;
    public static final int UNDIFF_OVERDRAW = 5;
    public static final int UNDIFF_STAKE_CAP = 3;

    private final String id;
    private String name;
    private String concept;
    private final boolean superPowered;
    private boolean undifferentiated;
    private boolean nonPerson; // Ch.5 participant (Thing/Location/Phenomenon/Situation)

    private final List<Ability> abilities = new ArrayList<Ability>();
    private final List<Drive> drives = new ArrayList<Drive>();
    private final List<Exemplar> exemplars = new ArrayList<Exemplar>();

    /** Debt stack for an Undifferentiated character (single pool; Overdrawn above 5, p.74). */
    private int undifferentiatedDebt;

    public Character(String id, String name, boolean superPowered) {
        this.id = id;
        this.name = name;
        this.superPowered = superPowered;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public String concept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }
    public boolean isSuperPowered() { return superPowered; }
    public boolean isUndifferentiated() { return undifferentiated; }
    public void setUndifferentiated(boolean u) { this.undifferentiated = u; }
    public boolean isNonPerson() { return nonPerson; }
    public void setNonPerson(boolean n) { this.nonPerson = n; }

    public List<Ability> abilities() { return abilities; }
    public List<Drive> drives() { return drives; }
    public List<Exemplar> exemplars() { return exemplars; }

    public List<Ability> abilitiesOfKind(AbilityKind kind) {
        List<Ability> out = new ArrayList<Ability>();
        for (Ability a : abilities) if (a.kind() == kind) out.add(a);
        return out;
    }

    public Drive drive(DriveType type) {
        for (Drive d : drives) if (d.type() == type) return d;
        return null;
    }

    /** The single highest-valued Ability score the character has (used as a ceiling in play). */
    public int highestAbilityScore() {
        int max = 0;
        for (Ability a : abilities) if (a.score() > max) max = a.score();
        return max;
    }

    public int totalDriveStrength() {
        int t = 0;
        for (Drive d : drives) t += d.strength();
        return t;
    }

    /** Total Debt currently at rest on all Drives (excludes Staked Debt). */
    public int totalRestingDebt() {
        int t = 0;
        for (Drive d : drives) t += d.debt();
        return t;
    }

    // --- Undifferentiated Debt stack (p.74) ---
    public int undifferentiatedDebt() { return undifferentiatedDebt; }
    public void addUndifferentiatedDebt(int n) { undifferentiatedDebt += n; }
    public void removeUndifferentiatedDebt(int n) {
        undifferentiatedDebt -= n;
        if (undifferentiatedDebt < 0) undifferentiatedDebt = 0;
    }
    /** Undifferentiated characters are Overdrawn above five Tokens (p.74). */
    public boolean isUndifferentiatedOverdrawn() { return undifferentiatedDebt > UNDIFF_OVERDRAW; }

    /**
     * Validates the ability-column shape shared by Freeform and Click-and-Lock
     * (p.72): three columns present for the character's type, each 3-5 large,
     * &le;12 total, numbered 1-up with no gaps. Returns null if valid, else a
     * human-readable reason. (Full builder-specific checks live in Phase 2.)
     */
    public String validateAbilityShape() {
        AbilityKind primary = superPowered ? AbilityKind.POWER : AbilityKind.SKILL;
        int total = abilities.size();
        if (total > 12) return "A character may have at most 12 Abilities (has " + total + ").";
        String c1 = validateColumn(primary);
        if (c1 != null) return c1;
        String c2 = validateColumn(AbilityKind.ATTITUDE);
        if (c2 != null) return c2;
        String c3 = validateColumn(AbilityKind.STYLE);
        if (c3 != null) return c3;
        return null;
    }

    private String validateColumn(AbilityKind kind) {
        List<Ability> col = abilitiesOfKind(kind);
        int n = col.size();
        if (n < 3 || n > 5) {
            return kind.displayName() + " column must have 3-5 Abilities (has " + n + ").";
        }
        boolean[] seen = new boolean[n + 1];
        for (Ability a : col) {
            int s = a.score();
            if (s < 1 || s > n || seen[s]) {
                return kind.displayName() + " column must be numbered 1.." + n + " with no gaps or repeats.";
            }
            seen[s] = true;
        }
        return null;
    }

    /** Detailed-Drive validity: exactly five Drives whose Strengths total nine (p.74). */
    public String validateDrives() {
        if (!superPowered) return null;    // only super-powered characters have Drives (p.68)
        if (undifferentiated) return null; // undifferentiated characters skip Drive assignment
        if (drives.size() != 5) return "A detailed character needs exactly 5 Drives (has " + drives.size() + ").";
        for (Drive d : drives) {
            if (d.strength() < 1 || d.strength() > 5) return "Each Drive Strength must be 1-5.";
        }
        if (totalDriveStrength() != 9) return "Drive Strengths must total exactly 9 (total " + totalDriveStrength() + ").";
        return null;
    }

    @Override public String toString() { return name + " (" + id + ")"; }
}

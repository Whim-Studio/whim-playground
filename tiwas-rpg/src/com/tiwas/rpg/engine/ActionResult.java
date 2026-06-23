package com.tiwas.rpg.engine;

/**
 * Immutable carrier for the outcome of a single skill test. Built by
 * {@link ActionResolver}; the UI reads it through the public getters.
 */
public final class ActionResult {
    private final String skillName;
    private final int roll;
    private final int effectiveSkill;
    private final boolean success;
    private final int cost;
    private final int overflowDamage;
    private final int margin;
    private final boolean doubles;
    private final int failureXP;
    private final boolean unlockedAdvancedSkill;
    private final int recovered;
    private final boolean usedMind;
    private final int newPoolValue;
    private final int newHP;

    public ActionResult(String skillName, int roll, int effectiveSkill, boolean success,
                        int cost, int overflowDamage, int margin, boolean doubles,
                        int failureXP, boolean unlockedAdvancedSkill, int recovered,
                        boolean usedMind, int newPoolValue, int newHP) {
        this.skillName = skillName;
        this.roll = roll;
        this.effectiveSkill = effectiveSkill;
        this.success = success;
        this.cost = cost;
        this.overflowDamage = overflowDamage;
        this.margin = margin;
        this.doubles = doubles;
        this.failureXP = failureXP;
        this.unlockedAdvancedSkill = unlockedAdvancedSkill;
        this.recovered = recovered;
        this.usedMind = usedMind;
        this.newPoolValue = newPoolValue;
        this.newHP = newHP;
    }

    public int getRoll() { return roll; }
    public int getEffectiveSkill() { return effectiveSkill; }
    public boolean isSuccess() { return success; }
    public int getCost() { return cost; }
    public int getOverflowDamage() { return overflowDamage; }
    public int getMargin() { return margin; }
    public boolean isDoubles() { return doubles; }
    public int getFailureXP() { return failureXP; }
    public boolean isUnlockedAdvancedSkill() { return unlockedAdvancedSkill; }
    public int getRecovered() { return recovered; }
    public boolean isUsedMind() { return usedMind; }
    public int getNewPoolValue() { return newPoolValue; }
    public int getNewHP() { return newHP; }

    /** One-line human-readable summary. */
    public String describe() {
        String pool = usedMind ? "MP" : "PE";
        StringBuilder sb = new StringBuilder();
        sb.append(skillName == null ? "Skill" : skillName).append(": rolled ")
          .append(roll).append(success ? " ≤ " : " > ").append(effectiveSkill)
          .append(success ? " → SUCCESS" : " → FAILURE")
          .append(", paid ").append(cost).append(' ').append(pool);
        if (overflowDamage > 0) {
            sb.append(" (overflow ").append(overflowDamage).append(" to HP)");
        }
        if (success) {
            sb.append(", margin +").append(margin);
        } else {
            sb.append(", failXP ").append(failureXP);
        }
        if (doubles) {
            sb.append(", doubles");
        }
        if (unlockedAdvancedSkill) {
            sb.append(", unlocked advanced skill");
        }
        return sb.toString();
    }
}

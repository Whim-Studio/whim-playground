package com.whim.swd6.api;

/**
 * A weapon stat block. Damage is a dice code rolled against the target's
 * resistance (Strength + armor). Ranged weapons carry short/medium/long range
 * bands (metres) and the difficulty tier for each band; melee weapons may leave
 * ranges at 0 and use {@code melee == true}.
 *
 * Owned by the orchestrator (api). Plain data holder.
 */
public final class Weapon {

    private String name;
    private String skill;        // governing skill name, e.g. "blaster", "melee combat"
    private DiceCode damage;
    private boolean melee;
    private int shortRange;      // metres
    private int mediumRange;
    private int longRange;
    private DifficultyTier shortDifficulty;
    private DifficultyTier mediumDifficulty;
    private DifficultyTier longDifficulty;
    private int cost;            // credits
    private String notes;

    public Weapon() {
        this.name = "";
        this.skill = "";
        this.damage = DiceCode.ZERO;
        this.shortDifficulty = DifficultyTier.VERY_EASY;
        this.mediumDifficulty = DifficultyTier.EASY;
        this.longDifficulty = DifficultyTier.MODERATE;
        this.notes = "";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public DiceCode getDamage() { return damage; }
    public void setDamage(DiceCode damage) { this.damage = damage == null ? DiceCode.ZERO : damage; }

    public boolean isMelee() { return melee; }
    public void setMelee(boolean melee) { this.melee = melee; }

    public int getShortRange() { return shortRange; }
    public void setShortRange(int shortRange) { this.shortRange = shortRange; }

    public int getMediumRange() { return mediumRange; }
    public void setMediumRange(int mediumRange) { this.mediumRange = mediumRange; }

    public int getLongRange() { return longRange; }
    public void setLongRange(int longRange) { this.longRange = longRange; }

    public DifficultyTier getShortDifficulty() { return shortDifficulty; }
    public void setShortDifficulty(DifficultyTier t) { this.shortDifficulty = t; }

    public DifficultyTier getMediumDifficulty() { return mediumDifficulty; }
    public void setMediumDifficulty(DifficultyTier t) { this.mediumDifficulty = t; }

    public DifficultyTier getLongDifficulty() { return longDifficulty; }
    public void setLongDifficulty(DifficultyTier t) { this.longDifficulty = t; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null ? "" : notes; }
}

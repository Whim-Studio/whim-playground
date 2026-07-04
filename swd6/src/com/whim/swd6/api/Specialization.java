package com.whim.swd6.api;

/**
 * A character's specialization under a skill (e.g. "blaster: heavy blaster pistol").
 * Holds the extra dice added on top of the parent skill's code. The effective
 * specialization code is (parent skill code + bonus).
 *
 * Owned by the orchestrator (api). Mutable per-character state.
 */
public final class Specialization {

    private String name;
    private DiceCode bonus;

    public Specialization() {
        this.name = "";
        this.bonus = DiceCode.ZERO;
    }

    public Specialization(String name, DiceCode bonus) {
        this.name = name;
        this.bonus = bonus == null ? DiceCode.ZERO : bonus;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DiceCode getBonus() {
        return bonus;
    }

    public void setBonus(DiceCode bonus) {
        this.bonus = bonus == null ? DiceCode.ZERO : bonus;
    }
}

package com.whim.swd6.api;

/**
 * Armor stat block. Armor adds dice to the wearer's physical resistance roll
 * ({@code physicalBonus}) and/or energy resistance ({@code energyBonus}). Some
 * armor imposes a Dexterity penalty represented as a negative dice code applied
 * to DEX-based actions.
 *
 * Owned by the orchestrator (api). Plain data holder.
 */
public final class Armor {

    private String name;
    private DiceCode physicalBonus;
    private DiceCode energyBonus;
    private DiceCode dexPenalty;   // magnitude subtracted from DEX actions
    private int cost;
    private String notes;

    public Armor() {
        this.name = "";
        this.physicalBonus = DiceCode.ZERO;
        this.energyBonus = DiceCode.ZERO;
        this.dexPenalty = DiceCode.ZERO;
        this.notes = "";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DiceCode getPhysicalBonus() { return physicalBonus; }
    public void setPhysicalBonus(DiceCode d) { this.physicalBonus = d == null ? DiceCode.ZERO : d; }

    public DiceCode getEnergyBonus() { return energyBonus; }
    public void setEnergyBonus(DiceCode d) { this.energyBonus = d == null ? DiceCode.ZERO : d; }

    public DiceCode getDexPenalty() { return dexPenalty; }
    public void setDexPenalty(DiceCode d) { this.dexPenalty = d == null ? DiceCode.ZERO : d; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null ? "" : notes; }
}

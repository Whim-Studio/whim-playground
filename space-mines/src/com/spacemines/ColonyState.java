package com.spacemines;

/**
 * Mutable holder for the colony's state for a single turn / year.
 *
 * Each field maps to a variable from the original C64 BASIC "Space Mines"
 * program (the BASIC letter is noted alongside each field).
 */
public class ColonyState {

    /** V — game year, starts at 1. */
    public int year;

    /** P — current colony population. */
    public int population;

    /** M — money / credits in the treasury. */
    public int money;

    /** C — ore currently held in storage (not yet sold). */
    public int storedOre;

    /** L — number of mines owned. */
    public int mines;

    /** S — satisfaction / morale, 0..100. */
    public int satisfaction;

    /** FP — current market price paid per unit of food. */
    public int foodPrice;

    /** CE — ore produced per mine this year. */
    public int orePerMine;

    /**
     * @return a deep copy of this state. Since every field is a primitive int,
     *         a field-by-field copy is sufficient and fully independent.
     */
    public ColonyState copy() {
        ColonyState c = new ColonyState();
        c.year = this.year;
        c.population = this.population;
        c.money = this.money;
        c.storedOre = this.storedOre;
        c.mines = this.mines;
        c.satisfaction = this.satisfaction;
        c.foodPrice = this.foodPrice;
        c.orePerMine = this.orePerMine;
        return c;
    }
}

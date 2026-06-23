package com.spacemines;

/**
 * The set of decisions a player makes during one turn (year).
 * Plain mutable data carrier filled in by the UI and consumed by the engine.
 */
public class PlayerActions {

    /** Number of new mines to buy this turn (each costs {@link GameConstants#MINE_COST}). */
    public int minesToBuy;

    /** Units of stored ore (C) to sell this turn. */
    public int oreToSell;

    /** Units of food to buy this turn (priced at FP per unit). */
    public int foodToBuy;
}

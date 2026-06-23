package com.spacemines;

/**
 * Faithful constants for the C64 BASIC "Space Mines" game, plus a factory
 * that seeds a fresh {@link ColonyState} from the INITIAL_* values.
 */
public final class GameConstants {

    private GameConstants() {
        // static-only utility class
    }

    /** Total number of years (turns) a game runs for. */
    public static final int TOTAL_YEARS = 10;

    /** Minimum population required to staff each mine. */
    public static final int MIN_PEOPLE_PER_MINE = 10;

    /** If satisfaction drops to/below this, the colony revolts. */
    public static final int SATISFACTION_REVOLT_THRESHOLD = 20;

    /** V — starting game year. */
    public static final int INITIAL_YEAR = 1;

    /** P — starting population. */
    public static final int INITIAL_POPULATION = 100;

    /** M — starting money / credits. */
    public static final int INITIAL_MONEY = 2000;

    /** L — starting number of mines. */
    public static final int INITIAL_MINES = 3;

    /** C — starting stored ore. */
    public static final int INITIAL_ORE = 0;

    /** S — starting satisfaction / morale (0..100). */
    public static final int INITIAL_SATISFACTION = 50;

    /** FP — starting food price. */
    public static final int INITIAL_FOOD_PRICE = 10;

    /** CE — starting ore produced per mine. */
    public static final int INITIAL_ORE_PER_MINE = 15;

    /** Cost to purchase one additional mine. */
    public static final int MINE_COST = 500;

    /**
     * @return a fresh {@link ColonyState} seeded entirely from the INITIAL_*
     *         constants above.
     */
    public static ColonyState newGame() {
        ColonyState c = new ColonyState();
        c.year = INITIAL_YEAR;
        c.population = INITIAL_POPULATION;
        c.money = INITIAL_MONEY;
        c.storedOre = INITIAL_ORE;
        c.mines = INITIAL_MINES;
        c.satisfaction = INITIAL_SATISFACTION;
        c.foodPrice = INITIAL_FOOD_PRICE;
        c.orePerMine = INITIAL_ORE_PER_MINE;
        return c;
    }
}

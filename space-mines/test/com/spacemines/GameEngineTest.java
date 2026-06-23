package com.spacemines;

import java.util.Random;

/**
 * Self-contained test runner for {@link GameEngine} — no JUnit.
 *
 * Run with:  java com.spacemines.GameEngineTest
 * Prints PASS/FAIL per check and "ALL TESTS PASSED" on success;
 * exits with status 1 on the first failure so it can gate a build.
 *
 * Determinism: a fixed-seed Random is used everywhere. Assertions only check
 * quantities the engine derives deterministically from player actions and
 * starting state (mines, money, storedOre, game-over flags/reasons). Values
 * that come from RandomEvents rolls (orePerMine, the next foodPrice) are never
 * asserted directly, and tests that trade use mines=0 so unpredictable mine
 * production cannot perturb storedOre.
 */
public class GameEngineTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        testBuyMinesTurn();
        testTradeFoodTurn();
        testRevoltGameOver();
        testDepopulationGameOver();
        testVictoryAfterAllYears();

        if (failures > 0) {
            System.out.println();
            System.out.println(failures + " of " + checks + " checks FAILED.");
            System.exit(1);
        }
        System.out.println();
        System.out.println("ALL TESTS PASSED (" + checks + " checks)");
    }

    // ------------------------------------------------------------------ tests

    /** Buying mines raises mine count and debits money by minesToBuy*MINE_COST. */
    private static void testBuyMinesTurn() {
        section("buy-mines turn");

        ColonyState s = GameConstants.newGame();
        s.year = 1;
        s.money = 100000;
        s.mines = 2;
        s.population = 1000;
        s.satisfaction = 100;     // independent of the unknown revolt threshold
        s.foodPrice = 1;
        s.storedOre = 0;

        int moneyBefore = s.money;
        int minesBefore = s.mines;

        PlayerActions a = new PlayerActions();
        a.minesToBuy = 3;
        a.oreToSell = 0;
        a.foodToBuy = s.population;   // neutralise the food/satisfaction swing

        GameEngine engine = new GameEngine(s, new Random(12345));
        TurnResult r = engine.processYear(a);

        int expectedMoney = moneyBefore
                - 3 * GameConstants.MINE_COST
                - s.population * 1;   // food bought at price 1

        assertEquals("mines increased by 3", minesBefore + 3, s.mines);
        assertEquals("money debited by mines + food cost", expectedMoney, s.money);
        assertFalse("turn did not end the game", r.gameOver);
        assertFalse("engine not game over", engine.isGameOver());
        assertEquals("year advanced", 2, s.year);
    }

    /** Selling ore credits money at foodPrice; buying food debits it again. */
    private static void testTradeFoodTurn() {
        section("trade-food turn");

        ColonyState s = GameConstants.newGame();
        s.year = 1;
        s.money = 1000;
        s.mines = 0;              // no production => storedOre only moves via trade
        s.storedOre = 500;
        s.population = 100;
        s.satisfaction = 100;
        s.foodPrice = 5;

        int priceAtTrade = s.foodPrice;
        int moneyBefore = s.money;
        int oreBefore = s.storedOre;

        PlayerActions a = new PlayerActions();
        a.minesToBuy = 0;
        a.oreToSell = 100;
        a.foodToBuy = s.population;   // surplus-neutral: bought == eaten

        GameEngine engine = new GameEngine(s, new Random(777));
        TurnResult r = engine.processYear(a);

        // sell 100 ore at 5 (+500), buy 100 food at 5 (-500) => net 0.
        int expectedMoney = moneyBefore
                + 100 * priceAtTrade
                - s.population * priceAtTrade;
        int expectedOre = oreBefore - 100;   // mines==0 so no production

        assertEquals("ore reduced by amount sold", expectedOre, s.storedOre);
        assertEquals("money reflects sell minus food buy", expectedMoney, s.money);
        assertFalse("turn did not end the game", r.gameOver);
    }

    /** Starving the colony pushes satisfaction past the revolt threshold. */
    private static void testRevoltGameOver() {
        section("revolt game over");

        ColonyState s = GameConstants.newGame();
        s.year = 1;
        s.money = 100000;
        s.mines = 0;                  // isolate the satisfaction trigger
        s.population = 100;
        s.foodPrice = 1;
        s.storedOre = 0;
        // Start one point above the threshold; buying no food forces a drop.
        s.satisfaction = GameConstants.SATISFACTION_REVOLT_THRESHOLD + 1;

        PlayerActions a = new PlayerActions();
        a.minesToBuy = 0;
        a.oreToSell = 0;
        a.foodToBuy = 0;              // hunger => negative satisfaction swing

        GameEngine engine = new GameEngine(s, new Random(42));
        TurnResult r = engine.processYear(a);

        assertTrue("game over flagged", r.gameOver);
        assertTrue("engine reports game over", engine.isGameOver());
        assertFalse("not a victory", engine.isVictory());
        assertEquals("revolt reason", "The people revolted!", r.gameOverReason);
    }

    /** Too few colonists for the mines ends the game with the depopulation reason. */
    private static void testDepopulationGameOver() {
        section("depopulation game over");

        ColonyState s = GameConstants.newGame();
        s.year = 1;
        s.money = 100000;
        s.mines = 5;                  // requires 5 * MIN_PEOPLE_PER_MINE workers
        s.population = 20;
        s.satisfaction = 100;         // high => no revolt; check is the trigger
        s.foodPrice = 1;
        s.storedOre = 0;

        PlayerActions a = new PlayerActions();
        a.minesToBuy = 0;
        a.oreToSell = 0;
        a.foodToBuy = 200;            // well-fed; keeps satisfaction high

        GameEngine engine = new GameEngine(s, new Random(99));
        TurnResult r = engine.processYear(a);

        // Even after any satisfaction-driven growth, population stays below
        // mines * MIN_PEOPLE_PER_MINE (5 * 10 = 50), so the colony collapses.
        assertTrue("population below mine staffing => " + s.population
                        + " < " + (s.mines * GameConstants.MIN_PEOPLE_PER_MINE),
                s.population < s.mines * GameConstants.MIN_PEOPLE_PER_MINE);
        assertTrue("game over flagged", r.gameOver);
        assertFalse("not a victory", engine.isVictory());
        assertEquals("depopulation reason",
                "Not enough people to work the mines.", r.gameOverReason);
    }

    /** Surviving past TOTAL_YEARS is a victory. */
    private static void testVictoryAfterAllYears() {
        section("victory after all years");

        ColonyState s = GameConstants.newGame();
        s.year = GameConstants.TOTAL_YEARS;   // this turn pushes year past the end
        s.money = 100000;
        s.mines = 1;
        s.population = 1000;                   // plenty to staff the mine
        s.satisfaction = 100;
        s.foodPrice = 1;
        s.storedOre = 0;

        PlayerActions a = new PlayerActions();
        a.minesToBuy = 0;
        a.oreToSell = 0;
        a.foodToBuy = s.population;            // keep morale up, avoid revolt

        GameEngine engine = new GameEngine(s, new Random(2024));
        TurnResult r = engine.processYear(a);

        assertTrue("game over after final year", r.gameOver);
        assertTrue("victory flagged", engine.isVictory());
        assertTrue("year advanced past total",
                s.year > GameConstants.TOTAL_YEARS);
    }

    // ---------------------------------------------------------------- helpers

    private static void section(String name) {
        System.out.println("--- " + name + " ---");
    }

    private static void assertTrue(String what, boolean cond) {
        record(what, cond);
    }

    private static void assertFalse(String what, boolean cond) {
        record(what, !cond);
    }

    private static void assertEquals(String what, int expected, int actual) {
        boolean ok = expected == actual;
        record(what + " (expected " + expected + ", got " + actual + ")", ok);
    }

    private static void assertEquals(String what, String expected, String actual) {
        boolean ok = (expected == null) ? actual == null : expected.equals(actual);
        record(what + " (expected \"" + expected + "\", got \"" + actual + "\")", ok);
    }

    private static void record(String what, boolean ok) {
        checks++;
        if (ok) {
            System.out.println("  PASS: " + what);
        } else {
            failures++;
            System.out.println("  FAIL: " + what);
        }
    }
}

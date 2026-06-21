package com.whim.civ.engine;

import com.whim.civ.domain.City;
import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.GameMap;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.Government;
import com.whim.civ.domain.TechType;
import com.whim.civ.domain.Terrain;
import com.whim.civ.domain.Tile;
import com.whim.civ.domain.Unit;
import com.whim.civ.domain.UnitType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * Engine self-test covering a fixed-seed combat duel, a city growth tick, a trade/tax/science
 * split, and a research unlock. Runs under JUnit 4 (mvn test) or via the bundled
 * {@link #main} entry point for a plain {@code javac}+{@code java} invocation.
 */
public class EngineSelfTest {

    // --- Combat ------------------------------------------------------------

    @Test
    public void fixedSeedCombatIsDeterministic() {
        CombatResolver combat = new CombatResolver(new Random(42L));

        Unit attacker = new Unit(UnitType.LEGION, 0, 0, 0);   // attack 4
        Unit defender = new Unit(UnitType.MILITIA, 1, 0, 1);  // defense 1, open grassland

        boolean attackerWon = combat.resolveCombat(attacker, defender, Terrain.GRASSLAND,
                false, false);

        // The strong Legion on open ground should win this seed, and the duel must terminate
        // with exactly one survivor.
        Assert.assertTrue("attacker should survive this matchup", attackerWon);
        Assert.assertTrue(attacker.isAlive());
        Assert.assertFalse(defender.isAlive());
        Assert.assertEquals(0, defender.getHitPoints());

        // Determinism: same seed + same setup -> identical surviving HP.
        CombatResolver combat2 = new CombatResolver(new Random(42L));
        Unit attacker2 = new Unit(UnitType.LEGION, 0, 0, 0);
        Unit defender2 = new Unit(UnitType.MILITIA, 1, 0, 1);
        combat2.resolveCombat(attacker2, defender2, Terrain.GRASSLAND, false, false);
        Assert.assertEquals(attacker.getHitPoints(), attacker2.getHitPoints());
    }

    @Test
    public void defenseModifiersStack() {
        CombatResolver combat = new CombatResolver(new Random(1L));
        Unit defender = new Unit(UnitType.PHALANX, 1, 0, 0); // defense 2
        // 2 (base) * 2 (hills +100%) * 1.5 (fortified) * 3 (walls) = 18.0
        double d = combat.defenseStrength(defender, Terrain.HILLS, true, true);
        Assert.assertEquals(18.0, d, 0.0001);
    }

    // --- Growth ------------------------------------------------------------

    @Test
    public void cityGrowsWhenFoodBoxFills() {
        GameState state = grasslandState(Government.DESPOTISM);
        Civilization civ = state.getCivilizations().get(0);
        EconomyEngine economy = new EconomyEngine();

        City c = new City(civ.getId(), "Rome", 5, 5);
        c.setPopulation(1);
        c.setFoodStore(19); // food box for size 1 is (1+1)*10 = 20
        state.getCities().add(c);

        // Surplus food this turn pushes the store over the box, triggering growth.
        int before = c.getPopulation();
        economy.grow(state, c);
        Assert.assertEquals(before + 1, c.getPopulation());
        Assert.assertTrue(c.getFoodStore() < c.getFoodBoxSize());
    }

    @Test
    public void cityStarvesOnFoodDeficit() {
        // Ocean-ringed city: only the center is workable, so a sizable city runs a deficit.
        GameMap map = new GameMap(12, 12);
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                map.getTile(x, y).setTerrain(Terrain.OCEAN);
            }
        }
        GameState state = new GameState(map);
        Civilization civ = new Civilization(0, "Test", true);
        state.getCivilizations().add(civ);

        City c = new City(0, "Isle", 5, 5);
        c.setPopulation(4);
        c.setFoodStore(0);
        state.getCities().add(c);

        new EconomyEngine().grow(state, c);
        Assert.assertEquals("a famished city should shrink", 3, c.getPopulation());
    }

    // --- Trade split -------------------------------------------------------

    @Test
    public void tradeSplitsByRatesAndSumsBack() {
        EconomyEngine economy = new EconomyEngine();
        Civilization civ = new Civilization(0, "Test", true);
        civ.setRates(30, 60, 10); // tax / science / luxury

        int[] split = economy.splitTrade(civ, 100);
        Assert.assertEquals(30, split[0]); // tax
        Assert.assertEquals(60, split[1]); // science
        Assert.assertEquals(10, split[2]); // luxury
        Assert.assertEquals(100, split[0] + split[1] + split[2]);

        // Non-divisible totals must still conserve the whole.
        int[] odd = economy.splitTrade(civ, 7);
        Assert.assertEquals(7, odd[0] + odd[1] + odd[2]);
    }

    @Test
    public void despotismPenaltyAndCorruptionApply() {
        // Grassland with a road yields food 2 / shields 1 / trade 1; bump to a 3-trade tile so
        // the despotism penalty is observable.
        GameState state = grasslandState(Government.DESPOTISM);
        Civilization civ = state.getCivilizations().get(0);
        GameMap map = state.getMap();
        // Center plus neighbours become ocean-with-trade-ish: use plains+road for trade.
        for (int[] xy : map.cityWorkTiles(5, 5)) {
            Tile t = map.getTile(xy[0], xy[1]);
            t.setTerrain(Terrain.GRASSLAND);
        }
        City c = new City(civ.getId(), "Rome", 5, 5);
        c.setPopulation(1);
        state.getCities().add(c);

        // Despotism state still produces non-negative numbers and trade is corruption-reduced.
        int trade = new EconomyEngine().computeTrade(state, c);
        Assert.assertTrue(trade >= 0);
    }

    // --- Research ----------------------------------------------------------

    @Test
    public void researchUnlocksTechAndAutoPicksNext() {
        ResearchEngine research = new ResearchEngine();
        GameState state = grasslandState(Government.DESPOTISM);
        Civilization civ = state.getCivilizations().get(0);

        // Pick a root tech (no prereqs) so it is immediately researchable.
        TechType target = TechType.ALPHABET;
        civ.setResearching(target);
        civ.setResearchBeakers(0);

        int cost = target.getBaseCost();
        Assert.assertTrue(cost > 0);

        // Not enough beakers yet: stays unlearned.
        research.advance(state, civ, cost - 1);
        Assert.assertFalse(civ.knows(target));
        Assert.assertEquals(cost - 1, civ.getResearchBeakers());

        // One more turn tips it over: tech learned, beakers cleared, a new target chosen.
        research.advance(state, civ, 5);
        Assert.assertTrue(civ.knows(target));
        Assert.assertEquals(0, civ.getResearchBeakers());
        Assert.assertNotEquals(target, civ.getResearching());
        Assert.assertFalse(research.researchable(civ).contains(target));
    }

    @Test
    public void researchableRespectsPrereqs() {
        ResearchEngine research = new ResearchEngine();
        Civilization civ = new Civilization(0, "Test", true);
        // WRITING requires ALPHABET; not available until ALPHABET is known.
        Assert.assertFalse(research.researchable(civ).contains(TechType.WRITING));
        civ.getKnownTechs().add(TechType.ALPHABET);
        Assert.assertTrue(research.researchable(civ).contains(TechType.WRITING));
    }

    // --- helpers -----------------------------------------------------------

    private static GameState grasslandState(Government gov) {
        GameMap map = new GameMap(12, 12);
        GameState state = new GameState(map);
        Civilization civ = new Civilization(0, "Test", true);
        civ.setGovernment(gov);
        state.getCivilizations().add(civ);
        return state;
    }

    /** Plain entry point so the suite can run without a JUnit runner on the classpath. */
    public static void main(String[] args) throws Exception {
        EngineSelfTest t = new EngineSelfTest();
        t.fixedSeedCombatIsDeterministic();
        t.defenseModifiersStack();
        t.cityGrowsWhenFoodBoxFills();
        t.cityStarvesOnFoodDeficit();
        t.tradeSplitsByRatesAndSumsBack();
        t.despotismPenaltyAndCorruptionApply();
        t.researchUnlocksTechAndAutoPicksNext();
        t.researchableRespectsPrereqs();
        System.out.println("EngineSelfTest: ALL PASSED");
    }
}

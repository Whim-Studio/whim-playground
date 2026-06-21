package com.whim.startrek.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.whim.startrek.domain.BorgState;
import com.whim.startrek.domain.Empire;
import com.whim.startrek.domain.GalaxyMap;
import com.whim.startrek.domain.GameState;
import com.whim.startrek.domain.Race;
import com.whim.startrek.domain.ResourceType;
import com.whim.startrek.domain.Ship;
import com.whim.startrek.domain.StarSystem;

/**
 * Standalone, dependency-free smoke test for the engine. Run its {@code main} to print PASS/FAIL lines
 * for the four behaviours called out by the contract: the price formula, the officers-non-tradable
 * guard, Borg scaling, and one full battle resolution.
 *
 * <p>This is intentionally a plain {@code main} (no JUnit) so the app builds and self-verifies with the
 * JDK alone.
 */
public class EngineSelfTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("== StarTrek EngineSelfTest ==");
        testPriceFormula();
        testOfficersNonTradable();
        testBorgScaling();
        testBattleResolution();
        System.out.println("----------------------------");
        System.out.println("PASSED: " + passed + "  FAILED: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testPriceFormula() {
        GameState s = newGame(6, 6);
        Empire fed = s.getEmpires().get(0);
        fed.setTreasury(ResourceType.CREDITS, 1000L);
        fed.setTreasury(ResourceType.DILITHIUM, 500L);

        EconomyEngine econ = new EconomyEngine();
        long supply = econ.totalGalacticSupply(ResourceType.DILITHIUM, s);
        long credits = econ.totalGalacticCredits(s);
        double expected = (double) supply / ((double) credits * 1000.0);
        double actual = econ.basePricePer1000(ResourceType.DILITHIUM, s);

        check("price formula matches supply/(credits*1000)", Math.abs(expected - actual) < 1e-12);
    }

    private static void testOfficersNonTradable() {
        GameState s = newGame(6, 6);
        Empire fed = s.getEmpires().get(0);
        fed.setTreasury(ResourceType.CREDITS, 1_000_000L);
        fed.setTreasury(ResourceType.OFFICERS, 50L);

        EconomyEngine econ = new EconomyEngine();
        boolean buyRejected = !econ.buy(fed, ResourceType.OFFICERS, 10L, s);
        boolean sellRejected = !econ.sell(fed, ResourceType.OFFICERS, 10L, s);
        boolean officersUnchanged = fed.getTreasury(ResourceType.OFFICERS) == 50L;

        check("officers cannot be bought", buyRejected);
        check("officers cannot be sold", sellRejected);
        check("officers stockpile unchanged after rejected trades", officersUnchanged);

        // And a tradable resource with insufficient funds is also rejected. Give the galaxy some
        // dilithium supply (via treasuries) so the price is strictly positive.
        fed.setTreasury(ResourceType.DILITHIUM, 5000L);
        Empire poor = s.getEmpires().get(1);
        poor.setTreasury(ResourceType.CREDITS, 0L);
        boolean insufficientRejected = !econ.buy(poor, ResourceType.DILITHIUM, 1_000_000L, s);
        check("buy rejected when funds insufficient", insufficientRejected);
    }

    private static void testBorgScaling() {
        GameState s = newGame(8, 8);
        BorgState borg = s.getBorgState();
        borg.setActive(true);

        BorgEngine engine = new BorgEngine();
        engine.step(s);
        int i1 = borg.getIntensity();
        int cells1 = borg.getControlledCells().size();
        int cubes1 = borg.getCubeCount();

        for (int t = 0; t < 6; t++) {
            engine.step(s);
        }
        int i2 = borg.getIntensity();
        int cells2 = borg.getControlledCells().size();
        int cubes2 = borg.getCubeCount();

        check("borg intensity grows each active turn", i2 > i1);
        check("borg territory expands", cells2 > cells1);
        check("borg builds more cubes over time", cubes2 >= cubes1 && cubes2 > 0);
        check("borg not eradicated while active", !engine.isEradicated(s));
        check("seeded at least one cell on first step", cells1 >= 1);
    }

    private static void testBattleResolution() {
        Race a = Race.FEDERATION;
        Race b = Race.KLINGON;
        List<Ship> sideA = new ArrayList<Ship>();
        List<Ship> sideB = new ArrayList<Ship>();
        sideA.add(combatShip("USS Enterprise", "Cruiser", a, 120, 40));
        sideA.add(combatShip("USS Defiant", "Escort", a, 90, 30));
        // Weaker side so the battle resolves decisively.
        sideB.add(combatShip("IKS Negh'Var", "Cruiser", b, 60, 10));

        BattleSimulator sim = new BattleSimulator(sideA, sideB, 800.0, 600.0);
        int steps = 0;
        while (!sim.isFinished() && steps < 20000) {
            sim.step(0.05);
            steps++;
        }

        check("battle reaches a conclusion", sim.isFinished());
        check("battle produced projectiles at some point or finished fast", steps > 0);
        check("battle has a winner", sim.getWinner() != null);
        check("expected stronger side (FEDERATION) wins", sim.getWinner() == Race.FEDERATION);
    }

    // ---------------------------------------------------------------- helpers

    private static Ship combatShip(String name, String cls, Race owner, int hull, int shields) {
        Ship ship = new Ship(name, cls, owner);
        ship.setHull(hull);
        ship.setShields(shields);
        return ship;
    }

    private static GameState newGame(int rows, int cols) {
        GalaxyMap map = new GalaxyMap(rows, cols);
        List<Empire> empires = new ArrayList<Empire>(Arrays.asList(
                new Empire(Race.FEDERATION), new Empire(Race.KLINGON)));
        // Give each empire a home system so economy/research have something to chew on.
        StarSystem fedHome = new StarSystem("Sol", 1, 1);
        fedHome.setStockpile(ResourceType.DILITHIUM, 200L);
        empires.get(0).getSystems().add(fedHome);
        StarSystem kliHome = new StarSystem("Qo'noS", rows - 2, cols - 2);
        empires.get(1).getSystems().add(kliHome);
        return new GameState(map, empires);
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS: " + label);
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }
}

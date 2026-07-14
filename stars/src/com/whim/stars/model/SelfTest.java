package com.whim.stars.model;

import com.whim.stars.model.formulas.Formulas;
import com.whim.stars.model.race.Race;
import com.whim.stars.model.ship.Catalogue;
import com.whim.stars.model.ship.ShipDesign;

/**
 * Dependency-free (no JUnit) smoke test for the Phase 1 model layer. Run with
 * {@code java -cp out com.whim.stars.model.SelfTest}. The same assertions are
 * mirrored as JUnit tests under {@code test/} for the Maven build.
 */
public final class SelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        Race race = Race.humanoid("Human");

        // --- Habitability ---
        double ideal = Formulas.habitability(race, 50, 50, 50);
        check("ideal world hab ~= 1.0", Math.abs(ideal - 1.0) < 1e-9);
        check("hostile world hab < 0", Formulas.habitability(race, 0, 50, 50) < 0);
        double edge = Formulas.habitability(race, 75, 50, 50);
        check("band edge hab >= 0 and < ideal", edge >= 0 && edge < ideal);

        // --- Population ---
        long maxPop = Formulas.maxPopulation(race, ideal);
        check("green world has positive max pop", maxPop > 0);
        check("small colony grows", Formulas.populationGrowth(race, 10_000, maxPop, ideal) > 0);
        check("at-capacity colony does not grow", Formulas.populationGrowth(race, maxPop, maxPop, ideal) <= 0);
        double hostile = Formulas.habitability(race, 0, 50, 50);
        check("hostile world kills colonists", Formulas.populationGrowth(race, 10_000, 0, hostile) < 0);

        // --- Research cost curve ---
        long lowLevel = Formulas.researchCost(TechField.ENERGY, 1, 5, 1.0);
        long highLevel = Formulas.researchCost(TechField.ENERGY, 2, 5, 1.0);
        check("higher level costs more research", highLevel > lowLevel);
        long fewFields = Formulas.researchCost(TechField.ENERGY, 1, 1, 1.0);
        long manyFields = Formulas.researchCost(TechField.ENERGY, 1, 10, 1.0);
        check("cross-field levels raise cost", manyFields > fewFields);

        // --- Movement ---
        check("warp 5 = 25 ly/yr", Formulas.warpDistance(5) == 25);
        check("warp 9 = 81 ly/yr", Formulas.warpDistance(9) == 81);

        // --- Ship design ---
        ShipDesign scout = new ShipDesign("Test Scout", Catalogue.scoutHull());
        scout.place(0, Catalogue.QUICK_JUMP_5);
        scout.place(1, Catalogue.BAT_SCANNER);
        check("design has engine", scout.isValid() && scout.engineCount() == 1);

        ShipDesign fast = new ShipDesign("Fast", Catalogue.frigateHull());
        fast.place(0, Catalogue.LONG_HUMP_6);
        check("design max warp = 6", fast.maxWarp() == 6);

        int expectedMass = Catalogue.scoutHull().baseMass()
                + Catalogue.QUICK_JUMP_5.mass() + Catalogue.BAT_SCANNER.mass();
        check("design mass = hull + parts", scout.totalMass() == expectedMass);

        Cargo cost = scout.mineralCost();
        long expectedIr = Catalogue.scoutHull().ironium()
                + Catalogue.QUICK_JUMP_5.ironium() + Catalogue.BAT_SCANNER.ironium();
        check("design mineral cost sums", cost.ironium() == expectedIr);

        boolean rejected = false;
        try {
            scout.place(0, Catalogue.LASER); // weapon into an engine slot
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check("wrong-category placement rejected", rejected);

        // --- Galaxy bookkeeping ---
        Galaxy galaxy = new Galaxy(Galaxy.UniverseSize.SMALL);
        check("small galaxy is 800 ly", galaxy.width() == 800);
        check("start year 2400", galaxy.year() == 2400);

        Player human = new Player(0, "You", race, false);
        Player rival = new Player(1, "Rival", race, true);
        galaxy.addPlayer(human);
        galaxy.addPlayer(rival);

        Planet terra = new Planet(1, "Terra", 100, 100);
        terra.setEnvironment(50, 50, 50);
        terra.setOwnerId(human.id());
        terra.setPopulation(25_000);
        galaxy.addPlanet(terra);
        Planet rigel = new Planet(2, "Rigel", 400, 400);
        rigel.setOwnerId(rival.id());
        galaxy.addPlanet(rigel);

        check("planet lookup by id", galaxy.planet(1) == terra);
        check("planetsOf returns owned", galaxy.planetsOf(human).size() == 1
                && galaxy.planetsOf(human).get(0) == terra);

        Fleet scoutFleet = galaxy.newFleet(human.id(), "Scout #1", 100, 100);
        check("fleet id allocated", scoutFleet.id() >= 1);
        check("galaxy tracks fleet", galaxy.fleet(scoutFleet.id()) == scoutFleet
                && galaxy.fleetsOf(human).size() == 1);

        int before = galaxy.year();
        galaxy.advanceYear();
        check("year advances", galaxy.year() == before + 1);

        System.out.println();
        System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
        System.out.println("Phase 1 self-test: " + (failures == 0 ? "ALL PASS" : "FAILURES PRESENT"));
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  PASS  " + name);
        } else {
            failures++;
            System.out.println("  FAIL  " + name);
        }
    }
}

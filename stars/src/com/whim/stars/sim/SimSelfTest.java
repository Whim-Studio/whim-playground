package com.whim.stars.sim;

import com.whim.stars.model.Fleet;
import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Mineral;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;
import com.whim.stars.model.TechField;
import com.whim.stars.model.Waypoint;
import com.whim.stars.model.production.ProductionItem;
import com.whim.stars.model.race.Race;
import com.whim.stars.model.ship.Catalogue;
import com.whim.stars.model.ship.ShipDesign;

/**
 * Dependency-free self-test for the Phase 2 simulation engine. Builds a live
 * two-player galaxy, runs real turns, and asserts economy, colonization, combat
 * and determinism. Run: {@code java -cp out com.whim.stars.sim.SimSelfTest}.
 */
public final class SimSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        // Re-run the Phase 1 checks first for a single green signal.
        com.whim.stars.model.SelfTest.main(new String[0]);
        System.out.println("--- Phase 2 ---");

        Galaxy g = buildScenario();
        Planet vega = g.planet(2);
        check("target starts unowned", !vega.isColonized());

        Player human = g.player(0);
        Planet terra = g.planet(1);
        long popBefore = terra.population();
        int factoriesBefore = terra.factories();

        TurnEngine engine = new TurnEngine(g);
        for (int i = 0; i < 10; i++) {
            engine.generateTurn();
        }

        check("year advanced 10", g.year() == Galaxy.START_YEAR + 10);
        check("factories were built", terra.factories() > factoriesBefore);
        check("population grew", terra.population() > popBefore);
        check("minerals were mined", terra.surface(Mineral.IRONIUM) > 0);
        check("research advanced a level", human.tech().total() > 0);
        check("target colonized", vega.isColonized() && vega.ownerId() == human.id());
        check("colonists delivered as population", vega.population() > 0);

        Fleet rivalScout = findFleet(g, "Rival Scout");
        Fleet humanFrigate = findFleet(g, "Human Frigate");
        check("unarmed weak fleet destroyed", rivalScout == null || rivalScout.isEmpty());
        check("armed fleet survives", humanFrigate != null && !humanFrigate.isEmpty());

        // Determinism: two fresh identical galaxies, 15 turns each, same hash.
        Galaxy a = buildScenario();
        Galaxy b = buildScenario();
        TurnEngine ea = new TurnEngine(a);
        TurnEngine eb = new TurnEngine(b);
        for (int i = 0; i < 15; i++) {
            ea.generateTurn();
            eb.generateTurn();
        }
        check("identical inputs give identical output", stateHash(a) == stateHash(b));

        System.out.println();
        System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
        System.out.println("Phase 2 sim self-test: " + (failures == 0 ? "ALL PASS" : "FAILURES PRESENT"));
        if (failures != 0) {
            System.exit(1);
        }
    }

    /** Deterministic scenario builder — no randomness, so it is reproducible. */
    private static Galaxy buildScenario() {
        Galaxy g = new Galaxy(Galaxy.UniverseSize.SMALL);
        Race race = Race.humanoid("Human");

        Player human = new Player(0, "You", race, false);
        Player rival = new Player(1, "Rival", race, true);
        human.setRelation(rival.id(), Player.Relation.ENEMY);
        rival.setRelation(human.id(), Player.Relation.ENEMY);
        g.addPlayer(human);
        g.addPlayer(rival);

        ShipDesign colony = Catalogue.colonyDesign();
        ShipDesign frigate = Catalogue.frigateDesign();
        ShipDesign scout = Catalogue.scoutDesign();
        human.addDesign(colony);
        human.addDesign(frigate);
        rival.addDesign(scout);

        // Human homeworld.
        Planet terra = new Planet(1, "Terra", 100, 100);
        terra.setEnvironment(50, 50, 50);
        terra.setOwnerId(human.id());
        terra.setHomeworld(true);
        terra.setPopulation(100_000);
        terra.setFactories(10);
        terra.setMines(10);
        for (Mineral m : Mineral.values()) {
            terra.setConcentration(m, 50);
            terra.setSurface(m, 200);
        }
        terra.productionQueue().add(ProductionItem.auto(ProductionItem.Kind.AUTO_FACTORY));
        terra.productionQueue().add(ProductionItem.auto(ProductionItem.Kind.AUTO_MINE));
        g.addPlanet(terra);

        // Nearby uncolonized target.
        Planet vega = new Planet(2, "Vega", 110, 100);
        vega.setEnvironment(50, 50, 50);
        for (Mineral m : Mineral.values()) {
            vega.setConcentration(m, 40);
        }
        g.addPlanet(vega);

        // Colony fleet heading to Vega.
        Fleet colonyFleet = g.newFleet(human.id(), "Pilgrim", 100, 100);
        colonyFleet.addShips(colony, 1);
        colonyFleet.setFuel(colonyFleet.fuelCapacity());
        colonyFleet.cargo().setColonists(5_000);
        colonyFleet.waypoints().add(Waypoint.toPlanet(vega, 5, Waypoint.Task.COLONIZE));

        // Combat pair, far from the economy, at (200,200).
        Fleet humanFrigate = g.newFleet(human.id(), "Human Frigate", 200, 200);
        humanFrigate.addShips(frigate, 1);
        humanFrigate.setFuel(humanFrigate.fuelCapacity());

        Fleet rivalScout = g.newFleet(rival.id(), "Rival Scout", 200, 200);
        rivalScout.addShips(scout, 1);
        rivalScout.setFuel(rivalScout.fuelCapacity());

        return g;
    }

    private static Fleet findFleet(Galaxy g, String name) {
        for (Fleet f : g.fleets()) {
            if (f.name().equals(name)) {
                return f;
            }
        }
        return null;
    }

    /** Order-independent hash of the whole galaxy state, for determinism checks. */
    private static long stateHash(Galaxy g) {
        long h = 1125899906842597L + g.year();
        for (Planet p : g.planets()) {
            h = h * 1000003 + p.id();
            h = h * 1000003 + p.ownerId();
            h = h * 1000003 + p.population();
            h = h * 1000003 + p.factories();
            h = h * 1000003 + p.mines();
            for (Mineral m : Mineral.values()) {
                h = h * 1000003 + p.surface(m);
                h = h * 1000003 + p.concentration(m);
            }
        }
        for (Player pl : g.players()) {
            for (TechField f : TechField.values()) {
                h = h * 1000003 + pl.tech().get(f);
                h = h * 1000003 + pl.researchPoints(f);
            }
        }
        for (Fleet f : g.fleets()) {
            h = h * 1000003 + f.id();
            h = h * 1000003 + Math.round(f.x());
            h = h * 1000003 + Math.round(f.y());
            h = h * 1000003 + f.shipCount();
            h = h * 1000003 + f.fuel();
        }
        return h;
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

package com.whim.stars.sim.ai;

import com.whim.stars.model.Fleet;
import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Mineral;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;
import com.whim.stars.model.TechField;
import com.whim.stars.model.production.ProductionItem;
import com.whim.stars.model.race.Race;
import com.whim.stars.model.ship.Catalogue;
import com.whim.stars.model.ship.ShipDesign;
import com.whim.stars.sim.TurnEngine;

/**
 * Dependency-free self-test for the Phase 7 AI. Builds a galaxy with an AI
 * player and an empty habitable target, runs AI planning + real turns, and
 * asserts the AI expands and stays deterministic.
 * Run: {@code java -cp out com.whim.stars.sim.ai.AiSelfTest}.
 */
public final class AiSelfTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) {
        Galaxy g = scenario();
        Player ai = g.player(1);
        Planet target = g.planet(2);

        check("target starts unowned", !target.isColonized());
        check("AI colony fleet exists", findFleet(g, "AI Pilgrim") != null);

        SimpleAi brain = new SimpleAi(g);
        TurnEngine engine = new TurnEngine(g);
        for (int i = 0; i < 12; i++) {
            brain.planAll();
            engine.generateTurn();
        }

        check("AI colonized a new world", g.planetsOf(ai).size() >= 2);
        boolean autoQueued = false;
        for (ProductionItem it : g.planet(1).productionQueue()) {
            if (it.kind() == ProductionItem.Kind.AUTO_FACTORY) autoQueued = true;
        }
        check("AI ensured auto-build on homeworld", autoQueued);
        check("AI homeworld grew factories", g.planet(1).factories() > 10);

        // Determinism: same seed + same AI => identical state.
        Galaxy a = scenario();
        Galaxy b = scenario();
        SimpleAi ba = new SimpleAi(a);
        SimpleAi bb = new SimpleAi(b);
        TurnEngine ea = new TurnEngine(a);
        TurnEngine eb = new TurnEngine(b);
        for (int i = 0; i < 15; i++) {
            ba.planAll();
            ea.generateTurn();
            bb.planAll();
            eb.generateTurn();
        }
        check("AI is deterministic", hash(a) == hash(b));

        System.out.println();
        System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
        System.out.println("Phase 7 AI self-test: " + (failures == 0 ? "ALL PASS" : "FAILURES PRESENT"));
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static Galaxy scenario() {
        Galaxy g = new Galaxy(Galaxy.UniverseSize.SMALL);
        Race race = Race.humanoid("Silicanoid");
        Player ai = new Player(1, "Rival", race, true);
        g.addPlayer(ai);

        ShipDesign colony = Catalogue.colonyDesign();
        ai.addDesign(colony);

        Planet home = new Planet(1, "Xylos", 100, 100);
        home.setEnvironment(50, 50, 50);
        home.setOwnerId(ai.id());
        home.setHomeworld(true);
        home.setPopulation(120_000);
        home.setFactories(10);
        home.setMines(10);
        for (Mineral m : Mineral.values()) {
            home.setConcentration(m, 60);
            home.setSurface(m, 400);
        }
        g.addPlanet(home);

        Planet target = new Planet(2, "Vega", 130, 100);
        target.setEnvironment(50, 50, 50);
        for (Mineral m : Mineral.values()) {
            target.setConcentration(m, 50);
        }
        g.addPlanet(target);

        Fleet pilgrim = g.newFleet(ai.id(), "AI Pilgrim", 100, 100);
        pilgrim.addShips(colony, 1);
        pilgrim.setFuel(pilgrim.fuelCapacity());
        return g;
    }

    private static Fleet findFleet(Galaxy g, String name) {
        for (Fleet f : g.fleets()) {
            if (f.name().equals(name)) return f;
        }
        return null;
    }

    private static long hash(Galaxy g) {
        long h = 1125899906842597L + g.year();
        for (Planet p : g.planets()) {
            h = h * 1000003 + p.id();
            h = h * 1000003 + p.ownerId();
            h = h * 1000003 + p.population();
            h = h * 1000003 + p.factories();
        }
        for (Player pl : g.players()) {
            for (TechField f : TechField.values()) {
                h = h * 1000003 + pl.tech().get(f);
            }
        }
        for (Fleet f : g.fleets()) {
            h = h * 1000003 + f.id();
            h = h * 1000003 + Math.round(f.x());
            h = h * 1000003 + Math.round(f.y());
        }
        return h;
    }

    private static void check(String name, boolean condition) {
        checks++;
        System.out.println((condition ? "  PASS  " : "  FAIL  ") + name);
        if (!condition) failures++;
    }
}

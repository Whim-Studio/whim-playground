package com.whim.stars.io;

import java.io.File;
import java.io.FileOutputStream;

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
 * Dependency-free round-trip test for {@link SaveGame}. Verifies field fidelity,
 * shared-reference integrity, determinism across the save boundary, and clean
 * rejection of a corrupt file. Run: {@code java -cp out com.whim.stars.io.SaveGameTest}.
 */
public final class SaveGameTest {

    private static int checks = 0;
    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Galaxy g = buildScenario();
        // Advance a few years so there is non-trivial state to round-trip.
        TurnEngine engine = new TurnEngine(g);
        for (int i = 0; i < 5; i++) {
            engine.generateTurn();
        }

        File file = File.createTempFile("stars-test", SaveGame.EXTENSION);
        file.deleteOnExit();
        SaveGame.save(g, file);
        check("save file exists and is non-empty", file.exists() && file.length() > 0);

        Galaxy loaded = SaveGame.load(file);
        Planet terra = g.planet(1);
        Planet terraLoaded = loaded.planet(1);

        check("year round-trips", loaded.year() == g.year());
        check("planet count round-trips", loaded.planets().size() == g.planets().size());
        check("population round-trips", terraLoaded.population() == terra.population());
        check("factories round-trip", terraLoaded.factories() == terra.factories());
        check("surface minerals round-trip",
                terraLoaded.surface(Mineral.IRONIUM) == terra.surface(Mineral.IRONIUM));
        check("tech round-trips",
                loaded.player(0).tech().get(TechField.ENERGY) == g.player(0).tech().get(TechField.ENERGY));

        // Shared-reference integrity: the loaded player's first design must be
        // the SAME instance the loaded fleet carries.
        Player humanLoaded = loaded.player(0);
        ShipDesign designInList = humanLoaded.designs().get(0);
        boolean identityPreserved = false;
        for (Fleet f : loaded.fleetsOf(humanLoaded)) {
            if (f.ships().containsKey(designInList)) {
                identityPreserved = true;
                break;
            }
        }
        check("shared ShipDesign identity preserved across reload", identityPreserved);

        // Determinism across the save boundary: original and reloaded copy must
        // advance identically.
        Galaxy reloaded = SaveGame.load(file);
        TurnEngine e1 = new TurnEngine(g);
        TurnEngine e2 = new TurnEngine(reloaded);
        for (int i = 0; i < 6; i++) {
            e1.generateTurn();
            e2.generateTurn();
        }
        check("reloaded game continues identically", stateHash(g) == stateHash(reloaded));

        // A garbage file must be rejected cleanly.
        File junk = File.createTempFile("stars-junk", SaveGame.EXTENSION);
        junk.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(junk);
        fos.write(new byte[]{0, 1, 2, 3, 4, 5, 6, 7});
        fos.close();
        boolean rejected = false;
        try {
            SaveGame.load(junk);
        } catch (Exception ex) {
            rejected = true;
        }
        check("garbage file is rejected", rejected);

        System.out.println();
        System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
        System.out.println("Phase 3 persistence self-test: " + (failures == 0 ? "ALL PASS" : "FAILURES PRESENT"));
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static Galaxy buildScenario() {
        Galaxy g = new Galaxy(Galaxy.UniverseSize.SMALL);
        Race race = Race.humanoid("Human");
        Player human = new Player(0, "You", race, false);
        g.addPlayer(human);
        human.tech().set(TechField.ENERGY, 2);

        ShipDesign frigate = Catalogue.frigateDesign();
        human.addDesign(frigate);

        Planet terra = new Planet(1, "Terra", 100, 100);
        terra.setEnvironment(50, 50, 50);
        terra.setOwnerId(human.id());
        terra.setPopulation(100_000);
        terra.setFactories(10);
        terra.setMines(10);
        for (Mineral m : Mineral.values()) {
            terra.setConcentration(m, 50);
            terra.setSurface(m, 200);
        }
        terra.productionQueue().add(ProductionItem.auto(ProductionItem.Kind.AUTO_FACTORY));
        g.addPlanet(terra);

        Fleet fleet = g.newFleet(human.id(), "Home Guard", 100, 100);
        fleet.addShips(frigate, 2);
        fleet.setFuel(fleet.fuelCapacity());
        return g;
    }

    private static long stateHash(Galaxy g) {
        long h = 1125899906842597L + g.year();
        for (Planet p : g.planets()) {
            h = h * 1000003 + p.id();
            h = h * 1000003 + p.population();
            h = h * 1000003 + p.factories();
            h = h * 1000003 + p.mines();
            for (Mineral m : Mineral.values()) {
                h = h * 1000003 + p.surface(m);
            }
        }
        for (Player pl : g.players()) {
            for (TechField f : TechField.values()) {
                h = h * 1000003 + pl.tech().get(f);
                h = h * 1000003 + pl.researchPoints(f);
            }
        }
        return h;
    }

    private static void check(String name, boolean condition) {
        checks++;
        System.out.println((condition ? "  PASS  " : "  FAIL  ") + name);
        if (!condition) {
            failures++;
        }
    }
}

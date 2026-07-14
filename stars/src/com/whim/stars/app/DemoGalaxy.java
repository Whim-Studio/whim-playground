package com.whim.stars.app;

import java.util.Random;

import com.whim.stars.model.Fleet;
import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Mineral;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;
import com.whim.stars.model.Waypoint;
import com.whim.stars.model.production.ProductionItem;
import com.whim.stars.model.race.Race;
import com.whim.stars.model.ship.Catalogue;
import com.whim.stars.model.ship.ShipDesign;

/**
 * Builds a ready-to-play demo galaxy for the Swing shell: a small universe with
 * two players (a human "You" and an AI "Rival"), scattered neutral worlds, two
 * developed homeworlds, and a couple of starting fleets.
 *
 * <p>Generation is <b>deterministic</b> — it is driven by a fixed-seed
 * {@link Random}, so "New Game" with the same seed always yields the same map.
 * That keeps the demo reproducible and testable, matching the engine's no-hidden-
 * randomness contract.
 */
public final class DemoGalaxy {

    /** Player id of the human in every demo galaxy. */
    public static final int HUMAN_ID = 0;
    public static final int AI_ID = 1;

    private static final String[] NAMES = {
            "Terra", "Vega", "Rigel", "Sirius", "Antares", "Polaris", "Deneb",
            "Altair", "Procyon", "Capella", "Arcturus", "Bellatrix", "Mizar",
            "Fomalhaut", "Aldebaran", "Spica", "Regulus", "Castor", "Pollux", "Alcor"
    };

    private DemoGalaxy() {
    }

    public static Galaxy build() {
        return build(20250714L);
    }

    public static Galaxy build(long seed) {
        Random rng = new Random(seed);
        Galaxy g = new Galaxy(Galaxy.UniverseSize.SMALL);
        int w = g.width();

        Race humanRace = Race.humanoid("Human");
        Race aiRace = Race.humanoid("Silicanoid");

        Player you = new Player(HUMAN_ID, "You", humanRace, false);
        you.setColorRgb(0x3B82F6); // blue
        Player rival = new Player(AI_ID, "Rival", aiRace, true);
        rival.setColorRgb(0xEF4444); // red
        you.setRelation(AI_ID, Player.Relation.ENEMY);
        rival.setRelation(HUMAN_ID, Player.Relation.ENEMY);
        g.addPlayer(you);
        g.addPlayer(rival);

        // Shared starting designs.
        ShipDesign colony = Catalogue.colonyDesign();
        ShipDesign scout = Catalogue.scoutDesign();
        ShipDesign frigate = Catalogue.frigateDesign();
        you.addDesign(scout);
        you.addDesign(colony);
        you.addDesign(frigate);
        rival.addDesign(scout);
        rival.addDesign(colony);

        int id = 1;

        // Human homeworld near the lower-left, AI homeworld near the upper-right.
        Planet home = homeworld(id++, "Terra", w * 0.2, w * 0.8, you);
        g.addPlanet(home);
        Planet aiHome = homeworld(id++, "Xylos", w * 0.8, w * 0.2, rival);
        g.addPlanet(aiHome);

        // Scatter neutral worlds across the map.
        int neutrals = g.size().typicalPlanets() / 4; // a lighter demo density
        for (int i = 0; i < neutrals && id <= NAMES.length; i++) {
            double x = 40 + rng.nextInt(w - 80);
            double y = 40 + rng.nextInt(w - 80);
            Planet p = new Planet(id, NAMES[(id - 1) % NAMES.length], x, y);
            p.setEnvironment(20 + rng.nextInt(60), 20 + rng.nextInt(60), 20 + rng.nextInt(60));
            for (Mineral m : Mineral.values()) {
                p.setConcentration(m, 15 + rng.nextInt(85));
            }
            g.addPlanet(p);
            id++;
        }

        // Human starting fleets: a scout to explore and a colony ship parked at home.
        Fleet scoutFleet = g.newFleet(HUMAN_ID, "Scout #1", home.x(), home.y());
        scoutFleet.addShips(scout, 1);
        scoutFleet.setFuel(scoutFleet.fuelCapacity());
        // Send the scout toward the galactic centre to reveal worlds.
        scoutFleet.waypoints().add(new Waypoint(w / 2.0, w / 2.0, 6, Waypoint.Task.NONE));

        Fleet colonyFleet = g.newFleet(HUMAN_ID, "Pilgrim", home.x(), home.y());
        colonyFleet.addShips(colony, 1);
        colonyFleet.setFuel(colonyFleet.fuelCapacity());
        colonyFleet.cargo().setColonists(5_000);

        // AI gets a scout too, so the map shows opposing forces.
        Fleet aiScout = g.newFleet(AI_ID, "Rival Scout", aiHome.x(), aiHome.y());
        aiScout.addShips(scout, 1);
        aiScout.setFuel(aiScout.fuelCapacity());

        return g;
    }

    private static Planet homeworld(int id, String name, double x, double y, Player owner) {
        Planet p = new Planet(id, name, x, y);
        p.setEnvironment(50, 50, 50); // ideal for a humanoid race
        p.setOwnerId(owner.id());
        p.setHomeworld(true);
        p.setPopulation(100_000);
        p.setFactories(10);
        p.setMines(10);
        p.setPlanetaryScanner(true);
        for (Mineral m : Mineral.values()) {
            p.setConcentration(m, 60);
            p.setSurface(m, 300);
        }
        p.productionQueue().add(ProductionItem.auto(ProductionItem.Kind.AUTO_FACTORY));
        p.productionQueue().add(ProductionItem.auto(ProductionItem.Kind.AUTO_MINE));
        return p;
    }
}

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
 * Builds a full galaxy from a {@link GameSetup}: one human plus N AI players,
 * their spread-out developed homeworlds, scattered neutral worlds scaled to the
 * universe size, and each player's starting designs and fleets.
 *
 * <p>Deterministic: everything random is driven by {@code setup.seed}, so the
 * same setup always yields the same galaxy.
 */
public final class GalaxyFactory {

    /** The human is always player id 0. */
    public static final int HUMAN_ID = 0;

    private static final int[] COLORS = {
            0x3B82F6, // blue  (human)
            0xEF4444, // red
            0x22C55E, // green
            0xF59E0B, // amber
    };

    private static final String[] AI_RACES = {
            "Silicanoid", "Rabbitoid", "Insectoid", "Nucleotid"
    };

    private static final String[] NAMES = {
            "Terra", "Vega", "Rigel", "Sirius", "Antares", "Polaris", "Deneb",
            "Altair", "Procyon", "Capella", "Arcturus", "Bellatrix", "Mizar",
            "Fomalhaut", "Aldebaran", "Spica", "Regulus", "Castor", "Pollux", "Alcor"
    };

    private GalaxyFactory() {
    }

    public static Galaxy build(GameSetup setup) {
        Random rng = new Random(setup.seed);
        Galaxy g = new Galaxy(setup.size);
        int w = g.width();
        double cx = w / 2.0;
        double cy = w / 2.0;

        int players = 1 + Math.max(0, Math.min(setup.aiOpponents, COLORS.length - 1));
        int id = 1;

        // --- Players and their homeworlds, spread evenly around a ring. ---
        double ring = w * 0.32;
        for (int i = 0; i < players; i++) {
            boolean human = (i == 0);
            Race race = human
                    ? Race.humanoid(setup.humanRaceName, setup.humanPrt)
                    : Race.humanoid(AI_RACES[(i - 1) % AI_RACES.length]);
            Player p = new Player(i, human ? "You" : AI_RACES[(i - 1) % AI_RACES.length], race, !human);
            p.setColorRgb(COLORS[i % COLORS.length]);
            g.addPlayer(p);

            // Shared starting designs.
            ShipDesign scout = Catalogue.scoutDesign();
            ShipDesign colony = Catalogue.colonyDesign();
            ShipDesign frigate = Catalogue.frigateDesign();
            p.addDesign(scout);
            p.addDesign(colony);
            p.addDesign(frigate);

            double angle = 2 * Math.PI * i / players;
            double hx = clamp(cx + ring * Math.cos(angle), w);
            double hy = clamp(cy + ring * Math.sin(angle), w);
            Planet home = homeworld(id++, human ? "Terra" : AI_RACES[(i - 1) % AI_RACES.length] + " Prime", hx, hy, p);
            g.addPlanet(home);

            // Starting fleets: a scout and a colony ship at the homeworld.
            Fleet scoutFleet = g.newFleet(p.id(), (human ? "Scout" : p.name() + " Scout") + " #1", hx, hy);
            scoutFleet.addShips(scout, 1);
            scoutFleet.setFuel(scoutFleet.fuelCapacity());

            Fleet colonyFleet = g.newFleet(p.id(), human ? "Pilgrim" : p.name() + " Pilgrim", hx, hy);
            colonyFleet.addShips(colony, 1);
            colonyFleet.setFuel(colonyFleet.fuelCapacity());
            if (human) {
                colonyFleet.cargo().setColonists(1_000); // ready for the player to send
            }
        }

        // Mutual enmity between every pair of players.
        for (Player a : g.players()) {
            for (Player b : g.players()) {
                if (a.id() != b.id()) {
                    a.setRelation(b.id(), Player.Relation.ENEMY);
                }
            }
        }

        // --- Neutral worlds, scaled to universe size. ---
        int neutrals = Math.max(6, g.size().typicalPlanets() / 3) - players;
        for (int i = 0; i < neutrals; i++) {
            double x = 40 + rng.nextInt(Math.max(1, w - 80));
            double y = 40 + rng.nextInt(Math.max(1, w - 80));
            Planet p = new Planet(id, planetName(id), x, y);
            p.setEnvironment(15 + rng.nextInt(70), 15 + rng.nextInt(70), 15 + rng.nextInt(70));
            for (Mineral m : Mineral.values()) {
                p.setConcentration(m, 15 + rng.nextInt(85));
            }
            g.addPlanet(p);
            id++;
        }

        return g;
    }

    private static Planet homeworld(int id, String name, double x, double y, Player owner) {
        Planet p = new Planet(id, name, x, y);
        p.setEnvironment(50, 50, 50);
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

    private static String planetName(int id) {
        int idx = (id - 1) % NAMES.length;
        int wrap = (id - 1) / NAMES.length;
        return wrap == 0 ? NAMES[idx] : NAMES[idx] + " " + toRoman(wrap + 1);
    }

    private static String toRoman(int n) {
        switch (n) {
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return Integer.toString(n);
        }
    }

    private static double clamp(double v, int w) {
        double margin = 40;
        if (v < margin) return margin;
        if (v > w - margin) return w - margin;
        return v;
    }
}

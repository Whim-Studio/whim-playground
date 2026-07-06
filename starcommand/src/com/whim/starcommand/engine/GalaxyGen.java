package com.whim.starcommand.engine;

import com.whim.starcommand.model.GameState;
import com.whim.starcommand.model.Mission;
import com.whim.starcommand.model.Planet;
import com.whim.starcommand.model.Sector;

/**
 * Builds the galaxy grid of "The Triangle": a human CORE on the left, the
 * pirate ALPHA Frontier upper-right, the insectoid BETA Frontier lower-right.
 * Also seeds the opening mission ladder (culminating in Blackbeard).
 */
public class GalaxyGen {

    private final Rng rng;

    public GalaxyGen(Rng rng) { this.rng = rng; }

    public void generate(GameState gs) {
        for (int x = 0; x < GameState.GALAXY_W; x++) {
            for (int y = 0; y < GameState.GALAXY_H; y++) {
                Sector.Frontier f;
                if (x <= 2) f = Sector.Frontier.CORE;
                else if (y < GameState.GALAXY_H / 2) f = Sector.Frontier.ALPHA;
                else f = Sector.Frontier.BETA;
                Sector s = new Sector(x, y, f);
                s.planet = maybePlanet(x, y, f);
                s.hostilePresence = f != Sector.Frontier.CORE && rng.chance(35);
                gs.galaxy[x][y] = s;
            }
        }

        // Star Command HQ starport at player origin.
        Sector home = gs.galaxy[0][GameState.GALAXY_H / 2];
        home.planet = new Planet("Star Command HQ", Planet.Kind.STARPORT);
        home.frontier = Sector.Frontier.CORE;
        home.hostilePresence = false;
        gs.shipX = home.x;
        gs.shipY = home.y;
        home.visited = true;

        // Blackbeard's lair: far corner of the Alpha Frontier.
        Sector lair = gs.galaxy[GameState.GALAXY_W - 1][0];
        lair.planet = new Planet("Blackbeard's Hideout", Planet.Kind.PIRATE_BASE);
        lair.hostilePresence = true;

        seedMissions(gs, lair);
    }

    private Planet maybePlanet(int x, int y, Sector.Frontier f) {
        if (!rng.chance(45)) return null;
        Planet.Kind kind;
        switch (f) {
            case ALPHA: kind = rng.chance(30) ? Planet.Kind.PIRATE_BASE : Planet.Kind.WORLD; break;
            case BETA:  kind = rng.chance(30) ? Planet.Kind.HIVE : Planet.Kind.DERELICT; break;
            default:    kind = Planet.Kind.WORLD; break;
        }
        return new Planet(planetName(x, y), kind);
    }

    private String planetName(int x, int y) {
        char letter = (char) ('A' + x);
        return "" + letter + "-" + (100 + y * 7);
    }

    private void seedMissions(GameState gs, Sector lair) {
        int lairCode = lair.x * 100 + lair.y;
        gs.missions.add(new Mission("m1", "Patrol the Alpha Frontier",
                "Pirate raiders have been sighted near the Alpha Frontier. "
                + "Travel beyond the core worlds, engage any hostiles, and prove your crew.",
                1500, -1));
        gs.missions.add(new Mission("m_blackbeard", "Hunt Blackbeard",
                "The pirate warlord Blackbeard rules the Alpha Frontier from a hidden base. "
                + "Locate his hideout in the far sector, disable his flagship and capture him.",
                8000, lairCode));
    }
}

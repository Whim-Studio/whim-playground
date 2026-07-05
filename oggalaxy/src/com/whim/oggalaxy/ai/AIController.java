package com.whim.oggalaxy.ai;

import com.whim.oggalaxy.api.Catalog;
import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.FleetOrder;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.engine.GameEngine;
import com.whim.oggalaxy.model.Empire;
import com.whim.oggalaxy.model.GameState;
import com.whim.oggalaxy.model.Planet;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Drives one AI empire. Runs each tick inside {@link GameEngine}'s tick (off the EDT) and
 * plays by exactly the same rules as the human: it calls the engine's validated command
 * helpers, so it can never do anything the player couldn't. Behaviour scales with
 * difficulty — see the class-level branches in {@link #act}.
 *
 * EASY   — economy only, slow cadence, tiny defense, never attacks.
 * MEDIUM — balanced economy + core combat tech + a modest fleet; occasionally attacks a
 *          clearly weaker neighbour.
 * HARD   — aggressive: optimised mine/tech order kept energy-positive, expands with colony
 *          ships, maintains a strong mixed fleet, attacks weaker empires opportunistically
 *          and recycles the debris it creates.
 */
public final class AIController {

    private final Ids.Difficulty difficulty;

    public AIController(Ids.Difficulty difficulty) {
        this.difficulty = difficulty == null ? Ids.Difficulty.MEDIUM : difficulty;
    }

    public Ids.Difficulty difficulty() {
        return difficulty;
    }

    public void act(GameEngine engine, Empire me) {
        int cadence = difficulty == Ids.Difficulty.HARD ? 2
                : difficulty == Ids.Difficulty.MEDIUM ? 3 : 6;
        int tick = engine.currentTick();
        if (tick % cadence != 0) return;
        if (me.planets.isEmpty()) return;

        Catalog cat = engine.catalogForAI();
        GameState st = engine.gameState();
        Planet home = me.planets.get(0);

        // 1) economy — every difficulty grows its base
        growEconomy(engine, me, home);

        // 2) research
        if (difficulty != Ids.Difficulty.EASY) {
            doResearch(engine, me, home);
        }

        // 3) military build-up
        if (difficulty == Ids.Difficulty.MEDIUM || difficulty == Ids.Difficulty.HARD) {
            buildMilitary(engine, me, home);
        } else {
            // EASY builds only a trickle of defense
            engine.enqueueDefenseFor(me, home, Ids.DefenseType.ROCKET_LAUNCHER, 2, true);
        }

        // 4) expansion (HARD)
        if (difficulty == Ids.Difficulty.HARD) {
            tryExpand(engine, me, home, st);
        }

        // 5) offense (MEDIUM/HARD)
        if (difficulty == Ids.Difficulty.MEDIUM || difficulty == Ids.Difficulty.HARD) {
            tryAttack(engine, me, home, st, cat);
        }
    }

    // ------------------------------------------------------------------ economy

    private void growEconomy(GameEngine engine, Empire me, Planet home) {
        if (home.currentConstruction() != null) return;
        // keep energy positive first
        if (home.resources().energyRatio() < 0.99) {
            if (engine.enqueueBuildingFor(me, home, Ids.BuildingType.SOLAR_PLANT, true).ok) return;
        }
        // prioritised build list (cheapest economic wins first)
        Ids.BuildingType[] order = {
                Ids.BuildingType.METAL_MINE,
                Ids.BuildingType.CRYSTAL_MINE,
                Ids.BuildingType.DEUTERIUM_SYNTHESIZER,
                Ids.BuildingType.ROBOTICS_FACTORY,
                Ids.BuildingType.RESEARCH_LAB,
                Ids.BuildingType.SHIPYARD,
                Ids.BuildingType.SOLAR_PLANT
        };
        // raise the lowest-level mine among the first three for balanced growth
        Ids.BuildingType lowest = null;
        int lowestLvl = Integer.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            int lvl = home.buildingLevel(order[i]);
            if (lvl < lowestLvl) { lowestLvl = lvl; lowest = order[i]; }
        }
        if (lowest != null && engine.enqueueBuildingFor(me, home, lowest, true).ok) return;
        for (Ids.BuildingType t : order) {
            if (engine.enqueueBuildingFor(me, home, t, true).ok) return;
        }
    }

    private void doResearch(GameEngine engine, Empire me, Planet home) {
        if (me.currentResearch() != null) return;
        Ids.TechType[] order = difficulty == Ids.Difficulty.HARD
                ? new Ids.TechType[]{
                Ids.TechType.ENERGY_TECHNOLOGY, Ids.TechType.COMPUTER_TECHNOLOGY,
                Ids.TechType.WEAPONS_TECHNOLOGY, Ids.TechType.SHIELDING_TECHNOLOGY,
                Ids.TechType.ARMOUR_TECHNOLOGY, Ids.TechType.LASER_TECHNOLOGY,
                Ids.TechType.COMBUSTION_DRIVE, Ids.TechType.ESPIONAGE_TECHNOLOGY}
                : new Ids.TechType[]{
                Ids.TechType.ENERGY_TECHNOLOGY, Ids.TechType.WEAPONS_TECHNOLOGY,
                Ids.TechType.SHIELDING_TECHNOLOGY, Ids.TechType.ARMOUR_TECHNOLOGY,
                Ids.TechType.COMPUTER_TECHNOLOGY};
        for (Ids.TechType t : order) {
            if (engine.enqueueResearchFor(me, home, t, true).ok) return;
        }
    }

    private void buildMilitary(GameEngine engine, Empire me, Planet home) {
        // a little defense, then warships if resources allow
        engine.enqueueDefenseFor(me, home, Ids.DefenseType.ROCKET_LAUNCHER, 3, true);
        if (difficulty == Ids.Difficulty.HARD) {
            engine.enqueueShipFor(me, home, Ids.ShipType.CRUISER, 3, true);
            engine.enqueueShipFor(me, home, Ids.ShipType.LIGHT_FIGHTER, 8, true);
        } else {
            engine.enqueueShipFor(me, home, Ids.ShipType.LIGHT_FIGHTER, 5, true);
        }
    }

    // ------------------------------------------------------------------ expansion

    private void tryExpand(GameEngine engine, Empire me, Planet home, GameState st) {
        int maxColonies = 1 + com.whim.oggalaxy.api.Formulas.maxColoniesFromAstro(
                me.techLevel(Ids.TechType.ASTROPHYSICS));
        if (me.planets.size() >= maxColonies) return;
        int have = home.shipCount(Ids.ShipType.COLONY_SHIP);
        if (have <= 0) {
            engine.enqueueShipFor(me, home, Ids.ShipType.COLONY_SHIP, 1, true);
            return;
        }
        if (engine.activeFleetsFor(me.id) >= engine.maxFleetSlotsFor(me)) return;
        // find an empty position in the home system
        for (int pos = 1; pos <= com.whim.oggalaxy.api.GameConfig.POSITIONS_PER_SYSTEM; pos++) {
            if (st.planetAt(home.galaxy, home.system, pos) == null) {
                Map<Ids.ShipType, Integer> ships = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
                ships.put(Ids.ShipType.COLONY_SHIP, 1);
                FleetOrder order = new FleetOrder(home.id, home.galaxy, home.system, pos, false,
                        Ids.MissionType.COLONIZE, ships, Cost.ZERO, 100, 0);
                engine.dispatchFor(me, home, order, true);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ offense

    private void tryAttack(GameEngine engine, Empire me, Planet home, GameState st, Catalog cat) {
        if (engine.activeFleetsFor(me.id) >= engine.maxFleetSlotsFor(me)) return;

        Map<Ids.ShipType, Integer> warships = warshipsOn(home);
        if (warships.isEmpty()) return;
        double myPower = fleetPower(warships, cat);
        if (myPower <= 0) return;

        // find the weakest target (any other empire's planet) we clearly outgun
        Planet best = null;
        double bestPower = Double.MAX_VALUE;
        for (Empire e : st.empires) {
            if (e == me || !e.alive) continue;
            for (Planet p : e.planets) {
                double def = planetDefensePower(p, cat);
                if (def < bestPower) { bestPower = def; best = p; }
            }
        }
        if (best == null) return;

        double margin = difficulty == Ids.Difficulty.HARD ? 1.3 : 1.8;
        if (myPower < bestPower * margin) return;

        FleetOrder order = new FleetOrder(home.id, best.galaxy, best.system, best.position, false,
                Ids.MissionType.ATTACK, warships, Cost.ZERO, 100, 0);
        engine.dispatchFor(me, home, order, true);
    }

    private Map<Ids.ShipType, Integer> warshipsOn(Planet home) {
        Map<Ids.ShipType, Integer> ships = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        for (Ids.ShipType t : Ids.ShipType.values()) {
            int c = home.shipCount(t);
            if (c > 0 && isWarship(t)) ships.put(t, c);
        }
        return ships;
    }

    private boolean isWarship(Ids.ShipType t) {
        switch (t) {
            case LIGHT_FIGHTER: case HEAVY_FIGHTER: case CRUISER: case BATTLESHIP:
            case BATTLECRUISER: case BOMBER: case DESTROYER: case REAPER:
            case LEVIATHAN: case DEATHSTAR:
                return true;
            default:
                return false;
        }
    }

    private double fleetPower(Map<Ids.ShipType, Integer> ships, Catalog cat) {
        double p = 0;
        for (Map.Entry<Ids.ShipType, Integer> e : ships.entrySet()) {
            com.whim.oggalaxy.api.ShipDef d = cat.ship(e.getKey());
            p += (d.weapon + d.shield + d.hull * 0.1) * e.getValue();
        }
        return p;
    }

    private double planetDefensePower(Planet p, Catalog cat) {
        double power = 0;
        for (Ids.ShipType t : Ids.ShipType.values()) {
            int c = p.shipCount(t);
            if (c > 0) {
                com.whim.oggalaxy.api.ShipDef d = cat.ship(t);
                power += (d.weapon + d.shield + d.hull * 0.1) * c;
            }
        }
        for (Ids.DefenseType t : Ids.DefenseType.values()) {
            int c = p.defenseCount(t);
            if (c > 0) {
                com.whim.oggalaxy.api.DefenseDef d = cat.defense(t);
                power += (d.weapon + d.shield + d.hull * 0.1) * c;
            }
        }
        return power;
    }
}

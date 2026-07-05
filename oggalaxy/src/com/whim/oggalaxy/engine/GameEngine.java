package com.whim.oggalaxy.engine;

import com.whim.oggalaxy.ai.AIController;
import com.whim.oggalaxy.api.Catalog;
import com.whim.oggalaxy.api.ClassDef;
import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.DefenseDef;
import com.whim.oggalaxy.api.Formulas;
import com.whim.oggalaxy.api.GameConfig;
import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.NewGameSetup;
import com.whim.oggalaxy.api.Requirement;
import com.whim.oggalaxy.api.Result;
import com.whim.oggalaxy.api.ShipDef;
import com.whim.oggalaxy.api.TechDef;
import com.whim.oggalaxy.api.BuildingDef;
import com.whim.oggalaxy.api.FleetOrder;
import com.whim.oggalaxy.api.Views;
import com.whim.oggalaxy.combat.CombatEngine;
import com.whim.oggalaxy.expedition.ExpeditionEngine;
import com.whim.oggalaxy.model.Empire;
import com.whim.oggalaxy.model.FleetMovement;
import com.whim.oggalaxy.model.GameState;
import com.whim.oggalaxy.model.Job;
import com.whim.oggalaxy.model.Planet;
import com.whim.oggalaxy.persistence.SaveLoad;

import javax.swing.SwingUtilities;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The real game simulation. Implements {@link GameController}: it owns all mutable state
 * ({@link GameState}), runs a background {@link TickLoop}, validates and applies player
 * commands, drives the AI, and resolves production, queues, research, fleets, combat and
 * expeditions.
 *
 * Threading: every state mutation and every {@link #state()} read is guarded by {@link #lock}
 * so the EDT never sees a half-updated world; the view classes additionally hand back copies
 * of their lists. The tick loop runs off the EDT and pushes events via {@code invokeLater}.
 */
public final class GameEngine implements GameController {

    private final Catalog catalog = Catalog.standard();
    private final Object lock = new Object();
    private final List<GameListener> listeners = new ArrayList<GameListener>();

    private volatile GameState state;
    private final Map<String, AIController> ais = new HashMap<String, AIController>();

    private TickLoop loop;
    private volatile boolean clockRunning;
    private volatile int speed = 1;

    // energy-consumption bases (OGame-standard; not stored in Catalog roleBase)
    private static final double METAL_ENERGY_BASE = 10;
    private static final double CRYSTAL_ENERGY_BASE = 10;
    private static final double DEUT_ENERGY_BASE = 20;

    public GameEngine() {
    }

    @Override public Catalog catalog() { return catalog; }
    @Override public Views.GameStateView state() { return state; }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void newGame(NewGameSetup setup) {
        synchronized (lock) {
            stopClockInternal();
            GameState st = new GameState();
            st.masterSeed = setup.seed;
            Random master = new Random(setup.seed);

            // --- player ---
            Empire player = new Empire();
            player.id = "p0";
            player.name = (setup.commanderName == null || setup.commanderName.trim().isEmpty())
                    ? "Commander" : setup.commanderName.trim();
            player.player = true;
            player.ai = false;
            player.playerClass = setup.playerClass == null ? Ids.PlayerClass.GENERAL : setup.playerClass;
            player.difficulty = null;
            player.darkMatter = GameConfig.START_DARK_MATTER;
            player.rng = new Random(setup.seed ^ 0x9E3779B97F4A7C15L);
            seedStartingTech(player);
            st.player = player;
            st.empires.add(player);

            int playerSystem = 20;
            Planet home = createHomePlanet(player, player.name + " Prime", 1, playerSystem, 7, master, true);
            player.planets.add(home);
            st.selectedPlanetId = home.id;

            // --- AI opponents ---
            List<NewGameSetup.AIConfig> opps = setup.opponents;
            int idx = 0;
            for (NewGameSetup.AIConfig cfg : opps) {
                idx++;
                Empire ai = new Empire();
                ai.id = "a" + idx;
                ai.name = (cfg.name == null || cfg.name.trim().isEmpty()) ? aiName(idx) : cfg.name.trim();
                ai.ai = true;
                ai.player = false;
                ai.playerClass = pickClass(master);
                ai.difficulty = resolveDifficulty(cfg.difficulty, master);
                ai.darkMatter = GameConfig.START_DARK_MATTER;
                ai.rng = new Random(setup.seed + 1000L * idx);
                seedStartingTech(ai);
                st.empires.add(ai);

                int sys = playerSystem + 3 * idx;
                if (sys > GameConfig.SYSTEMS_PER_GALAXY) sys = (sys % GameConfig.SYSTEMS_PER_GALAXY) + 1;
                int pos = 4 + (idx % 8);
                Planet aip = createHomePlanet(ai, ai.name + " Prime", 1, sys, pos, master, false);
                ai.planets.add(aip);
                ais.put(ai.id, new AIController(ai.difficulty));

                st.addLog(Ids.LogCategory.AI, ai.name + " enters the galaxy as a "
                        + ai.difficulty + " " + ai.playerClass + " empire.");
            }

            st.tick = 0;
            st.phase = Ids.Phase.RUNNING;
            this.state = st;

            recomputeAll();
            updateScoresAndSlots();
            st.addLog(Ids.LogCategory.SYSTEM, "New game started for " + player.name
                    + " (" + player.playerClass + ", " + opps.size() + " opponents).");
        }
    }

    private void seedStartingTech(Empire e) {
        e.setTech(Ids.TechType.ENERGY_TECHNOLOGY, 2);
        e.setTech(Ids.TechType.COMBUSTION_DRIVE, 2);
        e.setTech(Ids.TechType.COMPUTER_TECHNOLOGY, 2);
        e.setTech(Ids.TechType.WEAPONS_TECHNOLOGY, 1);
        e.setTech(Ids.TechType.SHIELDING_TECHNOLOGY, 1);
        e.setTech(Ids.TechType.ARMOUR_TECHNOLOGY, 1);
    }

    private Planet createHomePlanet(Empire owner, String name, int g, int s, int p, Random master, boolean isPlayer) {
        Planet pl = new Planet();
        pl.id = owner.id + "-1";
        pl.name = name;
        pl.ownerId = owner.id;
        pl.galaxy = g; pl.system = s; pl.position = p;
        pl.maxTemp = 20 + master.nextInt(50);
        pl.minTemp = pl.maxTemp - 40;
        pl.fieldsMax = GameConfig.HOME_FIELDS_BASE;

        pl.buildings.put(Ids.BuildingType.METAL_MINE, 6);
        pl.buildings.put(Ids.BuildingType.CRYSTAL_MINE, 5);
        pl.buildings.put(Ids.BuildingType.DEUTERIUM_SYNTHESIZER, 4);
        pl.buildings.put(Ids.BuildingType.SOLAR_PLANT, 7);
        pl.buildings.put(Ids.BuildingType.ROBOTICS_FACTORY, 2);
        pl.buildings.put(Ids.BuildingType.SHIPYARD, 4);
        pl.buildings.put(Ids.BuildingType.RESEARCH_LAB, 3);

        pl.res.metal = GameConfig.START_METAL;
        pl.res.crystal = GameConfig.START_CRYSTAL;
        pl.res.deuterium = GameConfig.START_DEUTERIUM;

        // starting forces so early battles are meaningful
        pl.addShips(Ids.ShipType.SMALL_CARGO, isPlayer ? 20 : 8);
        pl.addShips(Ids.ShipType.LIGHT_FIGHTER, isPlayer ? 40 : 20);
        pl.addShips(Ids.ShipType.CRUISER, isPlayer ? 5 : 2);
        pl.addDefense(Ids.DefenseType.ROCKET_LAUNCHER, isPlayer ? 20 : 25);
        pl.addDefense(Ids.DefenseType.LIGHT_LASER, isPlayer ? 8 : 10);
        return pl;
    }

    private String aiName(int idx) {
        String[] names = {"Zarkon Hegemony", "Vega Collective", "Orion Syndicate", "Krell Dominion",
                "Nova Compact", "Draco Ascendancy", "Cygnus League"};
        return names[(idx - 1) % names.length];
    }

    private Ids.PlayerClass pickClass(Random r) {
        Ids.PlayerClass[] v = Ids.PlayerClass.values();
        return v[r.nextInt(v.length)];
    }

    private Ids.Difficulty resolveDifficulty(Ids.Difficulty d, Random r) {
        if (d == null || d == Ids.Difficulty.RANDOM) {
            Ids.Difficulty[] concrete = {Ids.Difficulty.EASY, Ids.Difficulty.MEDIUM, Ids.Difficulty.HARD};
            return concrete[r.nextInt(concrete.length)];
        }
        return d;
    }

    // ------------------------------------------------------------------ clock

    @Override public void startClock() {
        synchronized (lock) {
            if (clockRunning) return;
            clockRunning = true;
            loop = new TickLoop(this);
            loop.start();
        }
    }

    @Override public void stopClock() {
        synchronized (lock) { stopClockInternal(); }
    }

    private void stopClockInternal() {
        clockRunning = false;
        if (loop != null) { loop.shutdown(); loop = null; }
    }

    @Override public boolean isClockRunning() { return clockRunning; }
    @Override public void setSpeed(int ticksPerSecond) { speed = Math.max(1, ticksPerSecond); }
    @Override public int getSpeed() { return speed; }

    @Override
    public void advance(int ticks) {
        List<Views.LogEntryView> pushed = new ArrayList<Views.LogEntryView>();
        synchronized (lock) {
            if (state == null) return;
            int before = state.log.size();
            for (int i = 0; i < ticks; i++) {
                if (state.phase == Ids.Phase.VICTORY || state.phase == Ids.Phase.DEFEAT) break;
                tickOnce();
            }
            for (int i = before; i < state.log.size(); i++) pushed.add(state.log.get(i));
        }
        fireTick(pushed);
    }

    // ------------------------------------------------------------------ the tick

    private void tickOnce() {
        GameState st = state;
        st.tick++;

        // (1) production, caps, energy
        recomputeAll();
        applyProduction();

        // (2) queues
        for (Empire e : st.empires) {
            advanceResearch(e);
            for (Planet p : e.planets) {
                advanceConstruction(e, p);
                advanceShipyard(p);
            }
        }

        // (3) fleets
        processFleets();

        // (4) AI
        for (Empire e : st.empires) {
            if (e.ai && e.alive) {
                AIController ctrl = ais.get(e.id);
                if (ctrl != null) {
                    try { ctrl.act(this, e); } catch (RuntimeException ex) { /* keep the sim alive */ }
                }
            }
        }

        // (5) scores / slots
        updateScoresAndSlots();

        // (6) victory / defeat
        checkEndConditions();
    }

    // ------------------------------------------------------------------ economy

    private void recomputeAll() {
        GameState st = state;
        for (Empire e : st.empires) {
            for (Planet p : e.planets) recomputePlanet(e, p);
        }
    }

    private void recomputePlanet(Empire e, Planet p) {
        ClassDef cd = catalog.playerClass(e.playerClass);
        int mLvl = p.buildingLevelOf(Ids.BuildingType.METAL_MINE);
        int cLvl = p.buildingLevelOf(Ids.BuildingType.CRYSTAL_MINE);
        int dLvl = p.buildingLevelOf(Ids.BuildingType.DEUTERIUM_SYNTHESIZER);
        int solarLvl = p.buildingLevelOf(Ids.BuildingType.SOLAR_PLANT);
        int fusionLvl = p.buildingLevelOf(Ids.BuildingType.FUSION_REACTOR);

        double solar = Formulas.solarOutput(catalog.building(Ids.BuildingType.SOLAR_PLANT).roleBase, solarLvl);
        double sat = p.shipCountOf(Ids.ShipType.SOLAR_SATELLITE) * satelliteEnergy(p.maxTemp);
        double fusion = Formulas.fusionOutput(catalog.building(Ids.BuildingType.FUSION_REACTOR).roleBase,
                fusionLvl, e.techLevelOf(Ids.TechType.ENERGY_TECHNOLOGY));
        double produced = (solar + sat + fusion) * cd.energyBonus;

        double consumed = Formulas.mineEnergyUse(METAL_ENERGY_BASE, mLvl)
                + Formulas.mineEnergyUse(CRYSTAL_ENERGY_BASE, cLvl)
                + Formulas.mineEnergyUse(DEUT_ENERGY_BASE, dLvl);

        double ratio = consumed <= 0 ? 1.0 : Math.min(1.0, produced / consumed);

        double baseMetal = 30.0 * GameConfig.ECONOMY_SPEED;
        double baseCrystal = 15.0 * GameConfig.ECONOMY_SPEED;

        double mineM = Formulas.mineProduction(catalog.building(Ids.BuildingType.METAL_MINE).roleBase, mLvl);
        double mineC = Formulas.mineProduction(catalog.building(Ids.BuildingType.CRYSTAL_MINE).roleBase, cLvl);
        double mineD = Formulas.mineProduction(catalog.building(Ids.BuildingType.DEUTERIUM_SYNTHESIZER).roleBase, dLvl)
                * Formulas.deutTempFactor(p.maxTemp);
        double fusionDeut = Formulas.fusionDeutUse(fusionLvl);

        p.res.prodMetal = baseMetal + mineM * ratio * cd.productionBonus;
        p.res.prodCrystal = baseCrystal + mineC * ratio * cd.productionBonus;
        p.res.prodDeut = mineD * ratio * cd.productionBonus - fusionDeut;
        p.res.eProduced = produced;
        p.res.eConsumed = consumed;

        p.res.capMetal = Formulas.storageCapacity(p.buildingLevelOf(Ids.BuildingType.METAL_STORAGE));
        p.res.capCrystal = Formulas.storageCapacity(p.buildingLevelOf(Ids.BuildingType.CRYSTAL_STORAGE));
        p.res.capDeut = Formulas.storageCapacity(p.buildingLevelOf(Ids.BuildingType.DEUTERIUM_TANK));
        p.res.darkMatter = e.darkMatter;
    }

    private double satelliteEnergy(int maxTemp) {
        return Math.floor((maxTemp + 160.0) / 6.0);
    }

    private void applyProduction() {
        for (Empire e : state.empires) {
            for (Planet p : e.planets) {
                p.res.metal = Math.min(p.res.capMetal, p.res.metal + p.res.prodMetal);
                p.res.crystal = Math.min(p.res.capCrystal, p.res.crystal + p.res.prodCrystal);
                double d = p.res.deuterium + p.res.prodDeut;
                if (d < 0) d = 0;
                p.res.deuterium = Math.min(p.res.capDeut, d);
            }
        }
    }

    // ------------------------------------------------------------------ queues

    private void advanceConstruction(Empire e, Planet p) {
        if (p.construction == null) return;
        p.construction.remainingTicks--;
        if (p.construction.remainingTicks <= 0) {
            Ids.BuildingType t = p.construction.building;
            p.buildings.put(t, p.buildingLevelOf(t) + 1);
            state.addLog(Ids.LogCategory.CONSTRUCTION,
                    catalog.building(t).name + " reaches level " + p.buildingLevelOf(t) + " on " + p.name);
            p.construction = null;
        }
    }

    private void advanceResearch(Empire e) {
        if (e.research == null) return;
        e.research.remainingTicks--;
        if (e.research.remainingTicks <= 0) {
            Ids.TechType t = e.research.tech;
            e.setTech(t, e.techLevelOf(t) + 1);
            if (!e.ai) {
                state.addLog(Ids.LogCategory.RESEARCH,
                        catalog.tech(t).name + " researched to level " + e.techLevelOf(t));
            }
            e.research = null;
        }
    }

    private void advanceShipyard(Planet p) {
        if (p.shipyard.isEmpty()) return;
        Job job = p.shipyard.get(0);
        job.remainingTicks--;
        job.unitRemaining--;
        if (job.unitRemaining <= 0) {
            if (job.kind == Job.Kind.SHIP) p.addShips(job.ship, 1);
            else p.addDefense(job.defense, 1);
            job.count--;
            job.unitRemaining = job.unitTicks;
            if (job.count <= 0) {
                p.shipyard.remove(0);
            }
        }
    }

    // ------------------------------------------------------------------ fleets

    private void processFleets() {
        GameState st = state;
        List<FleetMovement> snapshot = new ArrayList<FleetMovement>(st.fleets);
        for (FleetMovement f : snapshot) {
            if (!f.resolvedAtTarget && st.tick >= f.arrivalTick) {
                resolveArrival(f);
            } else if (f.returning && f.resolvedAtTarget && st.tick >= f.returnTick) {
                deliverHome(f);
            }
        }
    }

    private void resolveArrival(FleetMovement f) {
        f.resolvedAtTarget = true;
        switch (f.mission) {
            case ATTACK: resolveAttack(f); break;
            case EXPEDITION: resolveExpedition(f); break;
            case DEPLOY: resolveDeploy(f); return;      // one-way
            case COLONIZE: resolveColonize(f); break;
            case TRANSPORT: resolveTransport(f); break;
            case RECYCLE: resolveRecycle(f); break;
            case ESPIONAGE: resolveEspionage(f); break;
            default: break;
        }
        // schedule the return leg (unless the mission already removed the fleet)
        if (state.fleets.contains(f)) {
            f.returning = true;
            int oneWay = Math.max(1, f.arrivalTick - f.departTick);
            f.returnTick = state.tick + oneWay + Math.max(0, f.holdTicks);
        }
    }

    private void resolveAttack(FleetMovement f) {
        GameState st = state;
        Planet target = st.planetAt(f.target[0], f.target[1], f.target[2]);
        Empire attacker = st.findEmpire(f.ownerId);
        if (target == null || attacker == null) {
            st.addLog(Ids.LogCategory.FLEET, f.ownerName + "'s attack found no target and turns back.");
            return;
        }
        Empire defender = st.ownerOf(target);
        if (defender == null) return;

        CombatEngine.Profile aProf = profile(attacker);
        CombatEngine.Profile dProf = profile(defender);
        Cost stored = new Cost(target.res.metal, target.res.crystal, target.res.deuterium);

        CombatEngine.Outcome o = CombatEngine.resolve(f.target, f.ships, aProf,
                target.ships, target.defenses, dProf, catalog, true, stored, defender.rng);

        // apply survivors
        f.ships = new EnumMap<Ids.ShipType, Integer>(o.attackerSurvivors);
        target.ships.clear(); target.ships.putAll(o.defenderShipSurvivors);
        target.defenses.clear(); target.defenses.putAll(o.defenderDefenseSurvivors);

        // debris at the target
        if (o.debris.structurePoints() > 0) st.addDebris(f.target[0], f.target[1], f.target[2], o.debris);

        // plunder
        if (o.plunder.structurePoints() > 0) {
            target.res.metal -= o.plunder.metal;
            target.res.crystal -= o.plunder.crystal;
            target.res.deuterium -= o.plunder.deuterium;
            if (target.res.metal < 0) target.res.metal = 0;
            if (target.res.crystal < 0) target.res.crystal = 0;
            if (target.res.deuterium < 0) target.res.deuterium = 0;
            f.cargo = f.cargo.plus(o.plunder);
        }

        // moon
        if (o.moonCreated && !target.hasMoon) target.hasMoon = true;

        // report
        com.whim.oggalaxy.model.CombatReport rep = new com.whim.oggalaxy.model.CombatReport();
        rep.id = "c" + (st.combatReports.size() + 1);
        rep.tick = st.tick;
        rep.attackerName = attacker.name;
        rep.defenderName = defender.name;
        rep.location = new int[]{f.target[0], f.target[1], f.target[2]};
        rep.roundSummaries = new ArrayList<String>(o.roundSummaries);
        rep.attackerLosses = o.attackerLosses;
        rep.defenderShipLosses = o.defenderShipLosses;
        rep.defenderDefenseLosses = o.defenderDefenseLosses;
        rep.debris = o.debris;
        rep.plunder = o.plunder;
        rep.moonCreated = o.moonCreated && target.hasMoon;
        rep.outcome = o.outcomeText;
        st.combatReports.add(rep);
        while (st.combatReports.size() > 100) st.combatReports.remove(0);

        st.addLog(Ids.LogCategory.COMBAT, attacker.name + " attacked " + defender.name
                + " at " + f.target[0] + ":" + f.target[1] + ":" + f.target[2] + " — " + o.outcomeText);

        if (f.totalShips() == 0) {
            st.fleets.remove(f);
        }
    }

    private void resolveExpedition(FleetMovement f) {
        GameState st = state;
        Empire owner = st.findEmpire(f.ownerId);
        if (owner == null) return;
        ClassDef cd = catalog.playerClass(owner.playerClass);
        ExpeditionEngine.Outcome o = ExpeditionEngine.resolve(f.ships, cd.expeditionBonus, owner.rng, catalog);

        if (o.resourceGains.structurePoints() > 0) f.cargo = f.cargo.plus(o.resourceGains);
        if (o.darkMatter > 0) owner.darkMatter += o.darkMatter;
        for (Map.Entry<Ids.ShipType, Integer> e : o.gainedShips.entrySet()) {
            f.ships.put(e.getKey(), (f.ships.get(e.getKey()) == null ? 0 : f.ships.get(e.getKey())) + e.getValue());
        }
        for (Map.Entry<Ids.ShipType, Integer> e : o.lostShips.entrySet()) {
            int have = f.ships.get(e.getKey()) == null ? 0 : f.ships.get(e.getKey());
            int left = have - e.getValue();
            if (left <= 0) f.ships.remove(e.getKey()); else f.ships.put(e.getKey(), left);
        }
        f.holdTicks += o.extraDelayTicks;

        com.whim.oggalaxy.model.ExpeditionReport rep = new com.whim.oggalaxy.model.ExpeditionReport();
        rep.id = "e" + (st.expeditionReports.size() + 1);
        rep.tick = st.tick;
        rep.outcome = o.outcome;
        rep.detail = o.detail;
        rep.gains = o.resourceGains;
        rep.darkMatter = o.darkMatter;
        st.expeditionReports.add(rep);
        while (st.expeditionReports.size() > 100) st.expeditionReports.remove(0);

        if (!owner.ai) st.addLog(Ids.LogCategory.EXPEDITION, "Expedition: " + o.outcome + " — " + o.detail);

        if (f.totalShips() == 0) st.fleets.remove(f); // black hole / total loss
    }

    private void resolveDeploy(FleetMovement f) {
        GameState st = state;
        Planet target = st.planetAt(f.target[0], f.target[1], f.target[2]);
        Empire owner = st.findEmpire(f.ownerId);
        if (target != null && owner != null && owner.planets.contains(target)) {
            for (Map.Entry<Ids.ShipType, Integer> e : f.ships.entrySet()) target.addShips(e.getKey(), e.getValue());
            target.res.addCost(f.cargo);
            st.addLog(Ids.LogCategory.FLEET, owner.name + " deployed a fleet to " + target.name);
            st.fleets.remove(f);
        } else {
            // nowhere to deploy: bounce back
            f.returning = true;
            f.returnTick = st.tick + Math.max(1, f.arrivalTick - f.departTick);
        }
    }

    private void resolveColonize(FleetMovement f) {
        GameState st = state;
        Empire owner = st.findEmpire(f.ownerId);
        if (owner == null) return;
        Planet existing = st.planetAt(f.target[0], f.target[1], f.target[2]);
        int have = f.ships.get(Ids.ShipType.COLONY_SHIP) == null ? 0 : f.ships.get(Ids.ShipType.COLONY_SHIP);
        int maxColonies = 1 + Formulas.maxColoniesFromAstro(owner.techLevelOf(Ids.TechType.ASTROPHYSICS));
        if (existing == null && have > 0 && owner.planets.size() < maxColonies) {
            Planet colony = new Planet();
            colony.id = owner.id + "-" + (owner.planets.size() + 1);
            colony.name = owner.name + " Colony " + owner.planets.size();
            colony.ownerId = owner.id;
            colony.galaxy = f.target[0]; colony.system = f.target[1]; colony.position = f.target[2];
            colony.maxTemp = 20 + owner.rng.nextInt(50);
            colony.minTemp = colony.maxTemp - 40;
            colony.fieldsMax = 120 + owner.rng.nextInt(60);
            colony.res.addCost(f.cargo);
            owner.planets.add(colony);
            // consume one colony ship; the rest return
            if (have <= 1) f.ships.remove(Ids.ShipType.COLONY_SHIP);
            else f.ships.put(Ids.ShipType.COLONY_SHIP, have - 1);
            f.cargo = Cost.ZERO;
            st.addLog(Ids.LogCategory.FLEET, owner.name + " colonised a new world at "
                    + f.target[0] + ":" + f.target[1] + ":" + f.target[2]);
        } else if (!owner.ai) {
            st.addLog(Ids.LogCategory.FLEET, "Colonisation failed at "
                    + f.target[0] + ":" + f.target[1] + ":" + f.target[2] + " — fleet returns.");
        }
    }

    private void resolveTransport(FleetMovement f) {
        GameState st = state;
        Planet target = st.planetAt(f.target[0], f.target[1], f.target[2]);
        Empire owner = st.findEmpire(f.ownerId);
        if (target != null && owner != null && owner.planets.contains(target)) {
            target.res.addCost(f.cargo);
            f.cargo = Cost.ZERO;
            st.addLog(Ids.LogCategory.FLEET, owner.name + " transported resources to " + target.name);
        }
        // otherwise cargo returns with the fleet
    }

    private void resolveRecycle(FleetMovement f) {
        GameState st = state;
        Cost debris = st.debrisAt(f.target[0], f.target[1], f.target[2]);
        if (debris.structurePoints() <= 0) return;
        double capacity = fleetCargoCapacity(f);
        double used = f.cargo.structurePoints();
        double free = Math.max(0, capacity - used);
        double take = Math.min(free, debris.structurePoints());
        if (take <= 0) return;
        double share = take / debris.structurePoints();
        Cost got = new Cost(Math.floor(debris.metal * share), Math.floor(debris.crystal * share), 0);
        f.cargo = f.cargo.plus(got);
        Cost remaining = new Cost(debris.metal - got.metal, debris.crystal - got.crystal, 0);
        st.debrisFields.put(GameState.coordKey(f.target[0], f.target[1], f.target[2]), remaining);
        Empire owner = st.findEmpire(f.ownerId);
        if (owner != null && !owner.ai) {
            st.addLog(Ids.LogCategory.FLEET, "Recyclers harvested " + got + " of debris.");
        }
    }

    private void resolveEspionage(FleetMovement f) {
        GameState st = state;
        Planet target = st.planetAt(f.target[0], f.target[1], f.target[2]);
        Empire owner = st.findEmpire(f.ownerId);
        if (owner != null && !owner.ai) {
            if (target != null) {
                Empire def = st.ownerOf(target);
                st.addLog(Ids.LogCategory.ESPIONAGE, "Espionage report on " + target.name + " ("
                        + (def == null ? "?" : def.name) + "): "
                        + countUnits(target.ships) + " ships, " + countUnits(target.defenses) + " defenses.");
            } else {
                st.addLog(Ids.LogCategory.ESPIONAGE, "Probes found nothing at "
                        + f.target[0] + ":" + f.target[1] + ":" + f.target[2]);
            }
        }
    }

    private void deliverHome(FleetMovement f) {
        GameState st = state;
        Planet origin = st.findPlanet(f.originPlanetId);
        if (origin != null) {
            for (Map.Entry<Ids.ShipType, Integer> e : f.ships.entrySet()) origin.addShips(e.getKey(), e.getValue());
            origin.res.addCost(f.cargo);
        }
        st.fleets.remove(f);
    }

    private double fleetCargoCapacity(FleetMovement f) {
        double cap = 0;
        for (Map.Entry<Ids.ShipType, Integer> e : f.ships.entrySet()) {
            cap += catalog.ship(e.getKey()).cargo * e.getValue();
        }
        return cap;
    }

    private int countUnits(Map<?, Integer> m) {
        int n = 0;
        for (Integer v : m.values()) n += v;
        return n;
    }

    private CombatEngine.Profile profile(Empire e) {
        ClassDef cd = catalog.playerClass(e.playerClass);
        return new CombatEngine.Profile(cd.combatBonus,
                e.techLevelOf(Ids.TechType.WEAPONS_TECHNOLOGY),
                e.techLevelOf(Ids.TechType.SHIELDING_TECHNOLOGY),
                e.techLevelOf(Ids.TechType.ARMOUR_TECHNOLOGY));
    }

    // ------------------------------------------------------------------ scoring / end

    private void updateScoresAndSlots() {
        GameState st = state;
        for (Empire e : st.empires) {
            e.alive = !e.planets.isEmpty();
            e.score = computeScore(e);
        }
        st.maxFleetSlots = maxFleetSlots(st.player);
        st.usedFleetSlots = activeFleets(st.player.id);
    }

    private long computeScore(Empire e) {
        double points = 0;
        for (Planet p : e.planets) {
            for (Map.Entry<Ids.BuildingType, Integer> b : p.buildings.entrySet()) {
                BuildingDef def = catalog.building(b.getKey());
                for (int lvl = 1; lvl <= b.getValue(); lvl++) {
                    points += Formulas.levelCost(def.baseCost, def.costFactor, lvl).structurePoints();
                }
            }
            for (Map.Entry<Ids.ShipType, Integer> s : p.ships.entrySet()) {
                points += catalog.ship(s.getKey()).cost.structurePoints() * s.getValue();
            }
            for (Map.Entry<Ids.DefenseType, Integer> d : p.defenses.entrySet()) {
                points += catalog.defense(d.getKey()).cost.structurePoints() * d.getValue();
            }
            points += p.res.metal + p.res.crystal + p.res.deuterium;
        }
        for (Map.Entry<Ids.TechType, Integer> t : e.tech.entrySet()) {
            TechDef def = catalog.tech(t.getKey());
            for (int lvl = 1; lvl <= t.getValue(); lvl++) {
                points += Formulas.levelCost(def.baseCost, def.costFactor, lvl).structurePoints();
            }
        }
        return (long) (points / 1000.0 * GameConfig.POINTS_PER_1000_RES);
    }

    private void checkEndConditions() {
        GameState st = state;
        if (st.phase == Ids.Phase.VICTORY || st.phase == Ids.Phase.DEFEAT) return;
        if (!st.player.alive) {
            st.phase = Ids.Phase.DEFEAT;
            st.winnerName = firstAliveAI();
            st.addLog(Ids.LogCategory.SYSTEM, "Defeat — your empire has fallen.");
            return;
        }
        boolean anyAIAlive = false;
        for (Empire e : st.empires) if (e.ai && e.alive) anyAIAlive = true;
        if (!anyAIAlive && countAI() > 0) {
            st.phase = Ids.Phase.VICTORY;
            st.winnerName = st.player.name;
            st.addLog(Ids.LogCategory.SYSTEM, "Victory — all rival empires have been eliminated!");
        }
    }

    private int countAI() {
        int n = 0;
        for (Empire e : state.empires) if (e.ai) n++;
        return n;
    }

    private String firstAliveAI() {
        for (Empire e : state.empires) if (e.ai && e.alive) return e.name;
        return null;
    }

    private int maxFleetSlots(Empire e) {
        return 1 + e.techLevelOf(Ids.TechType.COMPUTER_TECHNOLOGY);
    }

    private int activeFleets(String ownerId) {
        int n = 0;
        for (FleetMovement f : state.fleets) if (ownerId.equals(f.ownerId)) n++;
        return n;
    }

    // ==================================================================
    //  Player commands (GameController)
    // ==================================================================

    @Override
    public Result enqueueBuilding(String planetId, Ids.BuildingType type) {
        synchronized (lock) {
            Planet p = state.findPlanet(planetId);
            if (p == null) return Result.fail("Unknown planet");
            Empire owner = state.ownerOf(p);
            if (owner == null || !owner.player) return Result.fail("Not your planet");
            return enqueueBuildingFor(owner, p, type, false);
        }
    }

    public Result enqueueBuildingFor(Empire owner, Planet p, Ids.BuildingType type, boolean silent) {
        BuildingDef def = catalog.building(type);
        if (def.moonOnly && !p.moon) return Result.fail(def.name + " can only be built on a moon");
        if (p.construction != null) return Result.fail("A building is already under construction here");
        if (p.usedFields() >= p.fieldsMax) return Result.fail("No free building fields on " + p.name);
        String req = checkRequirements(owner, p, def.requirements);
        if (req != null) return Result.fail("Requires " + req);
        int lvl = p.buildingLevelOf(type);
        if (def.maxLevel > 0 && lvl >= def.maxLevel) return Result.fail(def.name + " is at max level");
        Cost cost = def.costForNextLevel(lvl);
        if (!p.res.canAfford(cost)) return Result.fail("Not enough resources for " + def.name);
        p.res.spend(cost);
        int ticks = Formulas.buildTimeTicks(cost, p.buildingLevelOf(Ids.BuildingType.ROBOTICS_FACTORY),
                p.buildingLevelOf(Ids.BuildingType.NANITE_FACTORY));
        p.construction = Job.building(type, def.name + " " + (lvl + 1), ticks);
        if (!silent) state.addLog(Ids.LogCategory.CONSTRUCTION, "Queued " + def.name + " " + (lvl + 1) + " on " + p.name);
        return Result.ok("Building " + def.name + " (" + ticks + "h)");
    }

    @Override
    public Result cancelConstruction(String planetId) {
        synchronized (lock) {
            Planet p = state.findPlanet(planetId);
            if (p == null) return Result.fail("Unknown planet");
            if (p.construction == null) return Result.fail("Nothing under construction");
            // refund the building's cost
            Ids.BuildingType t = p.construction.building;
            Cost refund = catalog.building(t).costForNextLevel(p.buildingLevelOf(t));
            p.res.addCost(refund);
            p.construction = null;
            return Result.ok("Construction cancelled");
        }
    }

    @Override
    public Result enqueueResearch(Ids.TechType type, String labPlanetId) {
        synchronized (lock) {
            Planet lab = state.findPlanet(labPlanetId);
            if (lab == null) return Result.fail("Unknown lab planet");
            Empire owner = state.ownerOf(lab);
            if (owner == null || !owner.player) return Result.fail("Not your planet");
            return enqueueResearchFor(owner, lab, type, false);
        }
    }

    public Result enqueueResearchFor(Empire owner, Planet lab, Ids.TechType type, boolean silent) {
        if (owner.research != null) return Result.fail("Research already in progress");
        TechDef def = catalog.tech(type);
        String req = checkRequirements(owner, lab, def.requirements);
        if (req != null) return Result.fail("Requires " + req);
        int lvl = owner.techLevelOf(type);
        if (def.maxLevel > 0 && lvl >= def.maxLevel) return Result.fail(def.name + " is at max level");
        Cost cost = def.costForNextLevel(lvl);
        if (!lab.res.canAfford(cost)) return Result.fail("Not enough resources for " + def.name);
        lab.res.spend(cost);
        ClassDef cd = catalog.playerClass(owner.playerClass);
        int baseTicks = Formulas.researchTimeTicks(cost, lab.buildingLevelOf(Ids.BuildingType.RESEARCH_LAB));
        int ticks = Math.max(1, (int) Math.ceil(baseTicks * cd.researchSpeedBonus));
        owner.research = Job.research(type, def.name + " " + (lvl + 1), ticks, lab.id);
        if (!silent) state.addLog(Ids.LogCategory.RESEARCH, "Researching " + def.name + " " + (lvl + 1));
        return Result.ok("Researching " + def.name + " (" + ticks + "h)");
    }

    @Override
    public Result enqueueShip(String planetId, Ids.ShipType type, int count) {
        synchronized (lock) {
            if (count <= 0) return Result.fail("Count must be positive");
            Planet p = state.findPlanet(planetId);
            if (p == null) return Result.fail("Unknown planet");
            Empire owner = state.ownerOf(p);
            if (owner == null || !owner.player) return Result.fail("Not your planet");
            return enqueueShipFor(owner, p, type, count, false);
        }
    }

    public Result enqueueShipFor(Empire owner, Planet p, Ids.ShipType type, int count, boolean silent) {
        ShipDef def = catalog.ship(type);
        String req = checkRequirements(owner, p, def.requirements);
        if (req != null) return Result.fail("Requires " + req);
        Cost total = def.cost.scale(count);
        if (!p.res.canAfford(total)) {
            // build as many as affordable
            count = affordableCount(p.res, def.cost, count);
            if (count <= 0) return Result.fail("Not enough resources for " + def.name);
            total = def.cost.scale(count);
        }
        p.res.spend(total);
        int unitTicks = Formulas.shipBuildTimeTicks(def.cost, p.buildingLevelOf(Ids.BuildingType.SHIPYARD),
                p.buildingLevelOf(Ids.BuildingType.NANITE_FACTORY));
        p.shipyard.add(Job.shipBatch(type, count + "x " + def.name, count, unitTicks));
        if (!silent) state.addLog(Ids.LogCategory.CONSTRUCTION, "Building " + count + "x " + def.name + " on " + p.name);
        return Result.ok("Building " + count + "x " + def.name);
    }

    @Override
    public Result enqueueDefense(String planetId, Ids.DefenseType type, int count) {
        synchronized (lock) {
            if (count <= 0) return Result.fail("Count must be positive");
            Planet p = state.findPlanet(planetId);
            if (p == null) return Result.fail("Unknown planet");
            Empire owner = state.ownerOf(p);
            if (owner == null || !owner.player) return Result.fail("Not your planet");
            return enqueueDefenseFor(owner, p, type, count, false);
        }
    }

    public Result enqueueDefenseFor(Empire owner, Planet p, Ids.DefenseType type, int count, boolean silent) {
        DefenseDef def = catalog.defense(type);
        String req = checkRequirements(owner, p, def.requirements);
        if (req != null) return Result.fail("Requires " + req);
        if (def.maxCount > 0) {
            int existing = p.defenseCountOf(type);
            int queued = queuedDefense(p, type);
            int room = def.maxCount - existing - queued;
            if (room <= 0) return Result.fail("Only " + def.maxCount + " " + def.name + " allowed");
            count = Math.min(count, room);
        }
        Cost total = def.cost.scale(count);
        if (!p.res.canAfford(total)) {
            count = affordableCount(p.res, def.cost, count);
            if (count <= 0) return Result.fail("Not enough resources for " + def.name);
            total = def.cost.scale(count);
        }
        p.res.spend(total);
        int unitTicks = Formulas.shipBuildTimeTicks(def.cost, p.buildingLevelOf(Ids.BuildingType.SHIPYARD),
                p.buildingLevelOf(Ids.BuildingType.NANITE_FACTORY));
        p.shipyard.add(Job.defenseBatch(type, count + "x " + def.name, count, unitTicks));
        if (!silent) state.addLog(Ids.LogCategory.CONSTRUCTION, "Building " + count + "x " + def.name + " on " + p.name);
        return Result.ok("Building " + count + "x " + def.name);
    }

    private int queuedDefense(Planet p, Ids.DefenseType type) {
        int n = 0;
        for (Job j : p.shipyard) if (j.kind == Job.Kind.DEFENSE && j.defense == type) n += j.count;
        return n;
    }

    private int affordableCount(com.whim.oggalaxy.model.ResourceStore res, Cost unit, int desired) {
        int n = desired;
        while (n > 0 && !res.canAfford(unit.scale(n))) n--;
        return n;
    }

    @Override
    public Result dispatchFleet(FleetOrder order) {
        synchronized (lock) {
            Planet origin = state.findPlanet(order.originPlanetId);
            if (origin == null) return Result.fail("Unknown origin planet");
            Empire owner = state.ownerOf(origin);
            if (owner == null || !owner.player) return Result.fail("Not your planet");
            return dispatchFor(owner, origin, order, false);
        }
    }

    public Result dispatchFor(Empire owner, Planet origin, FleetOrder order, boolean silent) {
        if (order.ships == null || order.ships.isEmpty()) return Result.fail("No ships selected");
        if (activeFleets(owner.id) >= maxFleetSlots(owner)) return Result.fail("No free fleet slots");

        // ships available?
        for (Map.Entry<Ids.ShipType, Integer> e : order.ships.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            if (origin.shipCountOf(e.getKey()) < e.getValue()) {
                return Result.fail("Not enough " + catalog.ship(e.getKey()).name);
            }
        }
        // cargo available?
        if (!origin.res.canAfford(order.cargo)) return Result.fail("Not enough resources for cargo");

        // mission-specific checks
        if (order.mission == Ids.MissionType.COLONIZE
                && (order.ships.get(Ids.ShipType.COLONY_SHIP) == null || order.ships.get(Ids.ShipType.COLONY_SHIP) <= 0)) {
            return Result.fail("Colonisation needs a Colony Ship");
        }

        int distance = Formulas.distance(origin.galaxy, origin.system, origin.position,
                order.targetGalaxy, order.targetSystem, order.targetPosition);
        double slowest = slowestSpeed(order.ships);
        ClassDef cd = catalog.playerClass(owner.playerClass);
        double effSpeed = slowest * cd.fleetSpeedBonus;
        int flight = Formulas.flightTimeTicks(distance, effSpeed, order.speedPct);

        // fuel
        double fuel = 0;
        for (Map.Entry<Ids.ShipType, Integer> e : order.ships.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            fuel += Formulas.fuelConsumption(distance, catalog.ship(e.getKey()).fuel, e.getValue(), order.speedPct);
        }
        fuel = Math.floor(fuel * cd.fuelDiscount);
        if (origin.res.deuterium < fuel + order.cargo.deuterium) {
            return Result.fail("Not enough deuterium for fuel (" + (long) fuel + ")");
        }

        // commit
        origin.res.deuterium -= fuel;
        origin.res.spend(order.cargo);
        for (Map.Entry<Ids.ShipType, Integer> e : order.ships.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            origin.addShips(e.getKey(), -e.getValue());
        }

        FleetMovement f = new FleetMovement();
        f.id = "f" + System.identityHashCode(f);
        f.ownerId = owner.id;
        f.ownerName = owner.name;
        f.player = owner.player;
        f.mission = order.mission;
        f.originPlanetId = origin.id;
        f.origin = new int[]{origin.galaxy, origin.system, origin.position};
        f.target = new int[]{order.targetGalaxy, order.targetSystem, order.targetPosition};
        f.targetMoon = order.targetMoon;
        f.ships = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        for (Map.Entry<Ids.ShipType, Integer> e : order.ships.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) f.ships.put(e.getKey(), e.getValue());
        }
        f.cargo = order.cargo;
        f.speedPct = order.speedPct;
        f.departTick = state.tick;
        f.arrivalTick = state.tick + flight;
        f.holdTicks = order.mission == Ids.MissionType.EXPEDITION
                ? Math.max(GameConfig.EXPEDITION_MIN_DURATION_TICKS, Math.min(GameConfig.EXPEDITION_MAX_DURATION_TICKS, order.holdTicks))
                : Math.max(0, order.holdTicks);
        f.returnTick = f.arrivalTick + flight + f.holdTicks;
        state.fleets.add(f);

        if (!silent) state.addLog(Ids.LogCategory.FLEET, owner.name + " dispatched a " + order.mission
                + " fleet to " + order.targetGalaxy + ":" + order.targetSystem + ":" + order.targetPosition
                + " (arrives T+" + f.arrivalTick + "h)");
        return Result.ok(order.mission + " fleet dispatched (arrives in " + flight + "h)");
    }

    private double slowestSpeed(Map<Ids.ShipType, Integer> ships) {
        double slowest = Double.MAX_VALUE;
        for (Map.Entry<Ids.ShipType, Integer> e : ships.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            slowest = Math.min(slowest, catalog.ship(e.getKey()).speed);
        }
        return slowest == Double.MAX_VALUE ? 1000 : slowest;
    }

    @Override
    public Result recallFleet(String fleetId) {
        synchronized (lock) {
            for (FleetMovement f : state.fleets) {
                if (f.id.equals(fleetId)) {
                    if (f.returning) return Result.fail("Fleet is already returning");
                    f.returning = true;
                    f.resolvedAtTarget = true;
                    int elapsed = state.tick - f.departTick;
                    f.returnTick = state.tick + Math.max(1, elapsed);
                    state.addLog(Ids.LogCategory.FLEET, "Fleet recalled.");
                    return Result.ok("Fleet recalled");
                }
            }
            return Result.fail("Fleet not found");
        }
    }

    @Override
    public void selectPlanet(String planetId) {
        synchronized (lock) {
            if (state.findPlanet(planetId) != null) state.selectedPlanetId = planetId;
        }
    }

    // ---- requirements ----
    private String checkRequirements(Empire owner, Planet p, List<Requirement> reqs) {
        if (reqs == null) return null;
        for (Requirement r : reqs) {
            if (r.isBuilding()) {
                if (p.buildingLevelOf(r.building) < r.level) return r.label();
            } else {
                if (owner.techLevelOf(r.tech) < r.level) return r.label();
            }
        }
        return null;
    }

    // ==================================================================
    //  Persistence
    // ==================================================================

    @Override
    public Result save(File file) {
        synchronized (lock) {
            try {
                SaveLoad.save(file, state);
                return Result.ok("Saved to " + file.getName());
            } catch (Exception ex) {
                return Result.fail("Save failed: " + ex.getMessage());
            }
        }
    }

    @Override
    public Result load(File file) {
        synchronized (lock) {
            try {
                GameState loaded = SaveLoad.load(file);
                stopClockInternal();
                this.state = loaded;
                // rebuild transient AI controllers
                ais.clear();
                for (Empire e : loaded.empires) {
                    if (e.ai) ais.put(e.id, new AIController(e.difficulty));
                }
                recomputeAll();
                updateScoresAndSlots();
                return Result.ok("Loaded " + file.getName());
            } catch (Exception ex) {
                return Result.fail("Load failed: " + ex.getMessage());
            }
        }
    }

    // ==================================================================
    //  Listeners
    // ==================================================================

    @Override public void addListener(GameListener listener) {
        synchronized (listeners) { if (!listeners.contains(listener)) listeners.add(listener); }
    }
    @Override public void removeListener(GameListener listener) {
        synchronized (listeners) { listeners.remove(listener); }
    }

    private void fireTick(final List<Views.LogEntryView> events) {
        final List<GameListener> copy;
        synchronized (listeners) {
            if (listeners.isEmpty()) return;
            copy = new ArrayList<GameListener>(listeners);
        }
        final Views.GameStateView snap = state;
        Runnable r = new Runnable() {
            @Override public void run() {
                for (GameListener l : copy) {
                    l.onTick(snap);
                    for (Views.LogEntryView ev : events) l.onEvent(ev);
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    // ==================================================================
    //  AI access helpers (package + ai use). AI plays by the same rules.
    // ==================================================================

    /** Read-only handle passed to the AI. */
    public Catalog catalogForAI() { return catalog; }
    public GameState gameState() { return state; }
    public int currentTick() { return state.tick; }
    public int maxFleetSlotsFor(Empire e) { return maxFleetSlots(e); }
    public int activeFleetsFor(String ownerId) { return activeFleets(ownerId); }
}

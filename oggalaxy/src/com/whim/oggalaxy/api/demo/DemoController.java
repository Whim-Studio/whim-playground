package com.whim.oggalaxy.api.demo;

import com.whim.oggalaxy.api.Catalog;
import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.FleetOrder;
import com.whim.oggalaxy.api.GameConfig;
import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.NewGameSetup;
import com.whim.oggalaxy.api.Result;
import com.whim.oggalaxy.api.Views;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A lightweight, dependency-free {@link GameController} that serves a rich canned world
 * so the UI can be built and run before the real engine exists. It is NOT the game
 * simulation: {@code advance()} just trickles resources and burns down queue timers, and
 * commands mostly append a log line. The real {@code GameEngine} (simulation task)
 * replaces this at wiring time in {@code app.Main}.
 *
 * This class also documents, by example, exactly how the {@code Views} interfaces are
 * meant to be populated.
 */
public final class DemoController implements GameController {

    private final Catalog catalog = Catalog.standard();
    private final List<GameListener> listeners = new ArrayList<GameListener>();
    private final List<PojoLog> log = new ArrayList<PojoLog>();
    private State state;
    private boolean clockRunning;
    private int speed = 1;

    public DemoController() {
        newGame(new NewGameSetup("Commander", Ids.PlayerClass.GENERAL, new ArrayList<NewGameSetup.AIConfig>(), 42L));
    }

    @Override public Catalog catalog() { return catalog; }
    @Override public Views.GameStateView state() { return state; }

    @Override
    public void newGame(NewGameSetup setup) {
        this.state = buildDemoWorld(setup);
        addLog(Ids.LogCategory.SYSTEM, "New game started for " + setup.commanderName);
    }

    @Override public void startClock() { clockRunning = true; }
    @Override public void stopClock() { clockRunning = false; }
    @Override public boolean isClockRunning() { return clockRunning; }
    @Override public void setSpeed(int ticksPerSecond) { speed = Math.max(1, ticksPerSecond); }
    @Override public int getSpeed() { return speed; }

    @Override
    public void advance(int ticks) {
        for (int i = 0; i < ticks; i++) {
            state.tick++;
            for (PojoPlanet p : state.planetList) {
                p.res.metal = Math.min(p.res.cap, p.res.metal + p.res.prodMetal);
                p.res.crystal = Math.min(p.res.cap, p.res.crystal + p.res.prodCrystal);
                p.res.deut = Math.min(p.res.cap, p.res.deut + p.res.prodDeut);
                if (p.construction != null && --p.construction.remaining <= 0) {
                    addLog(Ids.LogCategory.CONSTRUCTION, "Completed: " + p.construction.label + " on " + p.name);
                    p.construction = null;
                }
            }
        }
    }

    @Override
    public Result enqueueBuilding(String planetId, Ids.BuildingType type) {
        PojoPlanet p = find(planetId);
        if (p == null) return Result.fail("Unknown planet");
        int lvl = p.buildings.getOrDefault(type, 0);
        p.construction = new PojoQueue(catalog.building(type).name + " " + (lvl + 1), 5);
        addLog(Ids.LogCategory.CONSTRUCTION, "Queued " + catalog.building(type).name + " on " + p.name);
        return Result.ok();
    }

    @Override
    public Result cancelConstruction(String planetId) {
        PojoPlanet p = find(planetId);
        if (p != null) p.construction = null;
        return Result.ok();
    }

    @Override
    public Result enqueueResearch(Ids.TechType type, String labPlanetId) {
        state.player.research = new PojoQueue(catalog.tech(type).name, 8);
        addLog(Ids.LogCategory.RESEARCH, "Researching " + catalog.tech(type).name);
        return Result.ok();
    }

    @Override
    public Result enqueueShip(String planetId, Ids.ShipType type, int count) {
        PojoPlanet p = find(planetId);
        if (p == null) return Result.fail("Unknown planet");
        p.shipyard.add(new PojoQueue(count + "x " + catalog.ship(type).name, 6));
        p.ships.put(type, p.ships.getOrDefault(type, 0) + count);
        addLog(Ids.LogCategory.CONSTRUCTION, "Building " + count + "x " + catalog.ship(type).name);
        return Result.ok();
    }

    @Override
    public Result enqueueDefense(String planetId, Ids.DefenseType type, int count) {
        PojoPlanet p = find(planetId);
        if (p == null) return Result.fail("Unknown planet");
        p.shipyard.add(new PojoQueue(count + "x " + catalog.defense(type).name, 6));
        p.defenses.put(type, p.defenses.getOrDefault(type, 0) + count);
        return Result.ok();
    }

    @Override
    public Result dispatchFleet(FleetOrder order) {
        PojoFleet f = new PojoFleet();
        f.id = "f" + (state.fleetList.size() + 1);
        f.ownerName = state.player.name;
        f.player = true;
        f.mission = order.mission;
        f.origin = new int[]{1, 1, 1};
        PojoPlanet o = find(order.originPlanetId);
        if (o != null) f.origin = new int[]{o.g, o.s, o.p};
        f.target = new int[]{order.targetGalaxy, order.targetSystem, order.targetPosition};
        f.ships = order.ships;
        f.cargo = order.cargo;
        f.arrival = state.tick + 3;
        f.ret = state.tick + 6;
        state.fleetList.add(f);
        addLog(Ids.LogCategory.FLEET, order.mission + " fleet dispatched");
        return Result.ok();
    }

    @Override
    public Result recallFleet(String fleetId) {
        for (PojoFleet f : state.fleetList) {
            if (f.id.equals(fleetId)) { f.returning = true; return Result.ok(); }
        }
        return Result.fail("Fleet not found");
    }

    @Override
    public void selectPlanet(String planetId) {
        if (find(planetId) != null) state.selectedId = planetId;
    }

    @Override public Result save(File file) { return Result.ok("(demo) not persisted"); }
    @Override public Result load(File file) { return Result.fail("(demo) nothing to load"); }
    @Override public void addListener(GameListener l) { listeners.add(l); }
    @Override public void removeListener(GameListener l) { listeners.remove(l); }

    // ------------------------------------------------------------------
    private PojoPlanet find(String id) {
        for (PojoPlanet p : state.planetList) if (p.id.equals(id)) return p;
        return null;
    }

    private void addLog(Ids.LogCategory cat, String msg) {
        PojoLog e = new PojoLog();
        e.tick = state == null ? 0 : state.tick;
        e.cat = cat;
        e.msg = msg;
        log.add(e);
        if (log.size() > 200) log.remove(0);
    }

    private State buildDemoWorld(NewGameSetup setup) {
        State st = new State();
        st.tick = 0;

        PojoEmpire player = new PojoEmpire();
        player.id = "p0";
        player.name = setup.commanderName == null ? "Commander" : setup.commanderName;
        player.player = true;
        player.pc = setup.playerClass == null ? Ids.PlayerClass.GENERAL : setup.playerClass;
        player.score = 1234;
        player.tech.put(Ids.TechType.ENERGY_TECHNOLOGY, 3);
        player.tech.put(Ids.TechType.COMBUSTION_DRIVE, 4);
        st.player = player;

        PojoPlanet home = new PojoPlanet();
        home.id = "p0-1";
        home.name = "Homeworld";
        home.g = 1; home.s = 42; home.p = 7;
        home.maxTemp = 35; home.minTemp = -5;
        home.fieldsMax = GameConfig.HOME_FIELDS_BASE; home.fieldsUsed = 9;
        home.buildings.put(Ids.BuildingType.METAL_MINE, 8);
        home.buildings.put(Ids.BuildingType.CRYSTAL_MINE, 6);
        home.buildings.put(Ids.BuildingType.DEUTERIUM_SYNTHESIZER, 4);
        home.buildings.put(Ids.BuildingType.SOLAR_PLANT, 9);
        home.buildings.put(Ids.BuildingType.ROBOTICS_FACTORY, 2);
        home.buildings.put(Ids.BuildingType.SHIPYARD, 4);
        home.buildings.put(Ids.BuildingType.RESEARCH_LAB, 3);
        home.ships.put(Ids.ShipType.LIGHT_FIGHTER, 24);
        home.ships.put(Ids.ShipType.SMALL_CARGO, 12);
        home.ships.put(Ids.ShipType.CRUISER, 3);
        home.defenses.put(Ids.DefenseType.ROCKET_LAUNCHER, 30);
        home.defenses.put(Ids.DefenseType.LIGHT_LASER, 12);
        home.res.metal = 15400; home.res.crystal = 8200; home.res.deut = 3100;
        home.res.cap = 100000;
        home.res.prodMetal = 1800; home.res.prodCrystal = 900; home.res.prodDeut = 320;
        home.res.eProd = 4200; home.res.eCons = 3600;
        home.construction = new PojoQueue("Crystal Mine 7", 4);
        player.planets.add(home);
        st.planetList.add(home);
        st.selectedId = home.id;

        PojoEmpire ai1 = new PojoEmpire();
        ai1.id = "a1"; ai1.name = "Zarkon Hegemony"; ai1.ai = true;
        ai1.difficulty = Ids.Difficulty.HARD; ai1.pc = Ids.PlayerClass.GENERAL; ai1.score = 2210;
        PojoPlanet ai1p = new PojoPlanet();
        ai1p.id = "a1-1"; ai1p.name = "Zarkon Prime"; ai1p.g = 1; ai1p.s = 44; ai1p.p = 4;
        ai1.planets.add(ai1p); st.planetList.add(ai1p);
        st.empires.add(ai1);

        PojoEmpire ai2 = new PojoEmpire();
        ai2.id = "a2"; ai2.name = "Vega Collective"; ai2.ai = true;
        ai2.difficulty = Ids.Difficulty.EASY; ai2.pc = Ids.PlayerClass.MINER; ai2.score = 640;
        PojoPlanet ai2p = new PojoPlanet();
        ai2p.id = "a2-1"; ai2p.name = "Vega I"; ai2p.g = 1; ai2p.s = 42; ai2p.p = 11;
        ai2.planets.add(ai2p); st.planetList.add(ai2p);
        st.empires.add(ai2);

        st.empires.add(0, player);

        PojoFleet f = new PojoFleet();
        f.id = "f1"; f.ownerName = player.name; f.player = true;
        f.mission = Ids.MissionType.EXPEDITION;
        f.origin = new int[]{1, 42, 7}; f.target = new int[]{1, 42, 16};
        f.ships = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        f.ships.put(Ids.ShipType.LIGHT_FIGHTER, 10);
        f.cargo = Cost.ZERO; f.arrival = 2; f.ret = 5;
        st.fleetList.add(f);

        PojoCombat cr = new PojoCombat();
        cr.id = "c1"; cr.tick = 0; cr.attacker = "Zarkon Hegemony"; cr.defender = player.name;
        cr.loc = new int[]{1, 42, 7};
        cr.rounds.add("Round 1: attacker fires 40,000 dmg, defender fires 12,000 dmg");
        cr.rounds.add("Round 2: defender shields collapse");
        cr.outcome = "Defender holds — attacker retreats";
        cr.debris = new Cost(12000, 6000, 0);
        st.combats.add(cr);

        PojoExped ex = new PojoExped();
        ex.id = "e1"; ex.tick = 0; ex.outcome = "Resources found";
        ex.detail = "Your fleet discovered a derelict freighter convoy.";
        ex.gains = new Cost(8000, 4000, 1000); ex.dm = 150;
        st.expeds.add(ex);

        st.logRef = log;
        addLog(Ids.LogCategory.SYSTEM, "Welcome to OG Galaxy.");
        return st;
    }

    // ================= POJO view implementations =================

    final class State implements Views.GameStateView {
        int tick;
        String selectedId;
        PojoEmpire player;
        final List<PojoEmpire> empires = new ArrayList<PojoEmpire>();
        final List<PojoPlanet> planetList = new ArrayList<PojoPlanet>();
        final List<PojoFleet> fleetList = new ArrayList<PojoFleet>();
        final List<PojoCombat> combats = new ArrayList<PojoCombat>();
        final List<PojoExped> expeds = new ArrayList<PojoExped>();
        List<PojoLog> logRef = new ArrayList<PojoLog>();

        @Override public int currentTick() { return tick; }
        @Override public String formattedTime() { return "T+" + tick + "h"; }
        @Override public Ids.Phase phase() { return Ids.Phase.RUNNING; }
        @Override public Views.EmpireView player() { return player; }
        @Override public List<Views.EmpireView> empires() { return new ArrayList<Views.EmpireView>(empires); }
        @Override public List<Views.FleetMovementView> fleets() { return new ArrayList<Views.FleetMovementView>(fleetList); }
        @Override public String selectedPlanetId() { return selectedId; }
        @Override public Views.PlanetView selectedPlanet() { return find(selectedId); }
        @Override public List<Views.LogEntryView> log() { return new ArrayList<Views.LogEntryView>(logRef); }
        @Override public List<Views.GalaxyCellView> galaxyRow(int galaxy, int system) {
            List<Views.GalaxyCellView> row = new ArrayList<Views.GalaxyCellView>();
            for (int pos = 1; pos <= GameConfig.POSITIONS_PER_SYSTEM; pos++) {
                PojoCell c = new PojoCell(); c.g = galaxy; c.s = system; c.p = pos;
                for (PojoPlanet pl : planetList) {
                    if (pl.g == galaxy && pl.s == system && pl.p == pos) {
                        c.name = pl.name; c.owner = ownerOf(pl); c.player = c.owner.equals(player.name);
                    }
                }
                row.add(c);
            }
            return row;
        }
        @Override public List<Views.CombatReportView> combatReports() { return new ArrayList<Views.CombatReportView>(combats); }
        @Override public List<Views.ExpeditionReportView> expeditionReports() { return new ArrayList<Views.ExpeditionReportView>(expeds); }
        @Override public int usedFleetSlots() { return fleetList.size(); }
        @Override public int maxFleetSlots() { return 1 + player.tech.getOrDefault(Ids.TechType.COMPUTER_TECHNOLOGY, 0); }
        @Override public String winnerName() { return null; }

        private String ownerOf(PojoPlanet pl) {
            for (PojoEmpire e : empires) if (e.planets.contains(pl)) return e.name;
            return "";
        }
    }

    static final class PojoRes implements Views.ResourceView {
        double metal, crystal, deut, cap;
        double prodMetal, prodCrystal, prodDeut, eProd, eCons;
        @Override public double amount(Ids.ResourceType t) {
            switch (t) { case METAL: return metal; case CRYSTAL: return crystal;
                case DEUTERIUM: return deut; case ENERGY: return eProd - eCons; default: return 0; }
        }
        @Override public double capacity(Ids.ResourceType t) {
            return t == Ids.ResourceType.ENERGY || t == Ids.ResourceType.DARK_MATTER ? Double.POSITIVE_INFINITY : cap;
        }
        @Override public double productionPerTick(Ids.ResourceType t) {
            switch (t) { case METAL: return prodMetal; case CRYSTAL: return prodCrystal;
                case DEUTERIUM: return prodDeut; default: return 0; }
        }
        @Override public double energyProduced() { return eProd; }
        @Override public double energyConsumed() { return eCons; }
        @Override public double energyRatio() { return eCons <= 0 ? 1 : Math.min(1, eProd / eCons); }
    }

    static final class PojoQueue implements Views.QueueItemView {
        String label; int total; int remaining;
        PojoQueue(String label, int total) { this.label = label; this.total = total; this.remaining = total; }
        @Override public String label() { return label; }
        @Override public int totalTicks() { return total; }
        @Override public int remainingTicks() { return remaining; }
        @Override public double progressFraction() { return total <= 0 ? 1 : 1.0 - (double) remaining / total; }
    }

    static final class PojoPlanet implements Views.PlanetView {
        String id, name; int g, s, p; boolean moon;
        int maxTemp, minTemp, fieldsUsed, fieldsMax;
        final Map<Ids.BuildingType, Integer> buildings = new EnumMap<Ids.BuildingType, Integer>(Ids.BuildingType.class);
        final Map<Ids.ShipType, Integer> ships = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        final Map<Ids.DefenseType, Integer> defenses = new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class);
        final PojoRes res = new PojoRes();
        PojoQueue construction;
        final List<PojoQueue> shipyard = new ArrayList<PojoQueue>();
        @Override public String id() { return id; }
        @Override public String name() { return name; }
        @Override public int galaxy() { return g; }
        @Override public int system() { return s; }
        @Override public int position() { return p; }
        @Override public boolean isMoon() { return moon; }
        @Override public boolean hasMoon() { return false; }
        @Override public int minTemp() { return minTemp; }
        @Override public int maxTemp() { return maxTemp; }
        @Override public int fieldsUsed() { return fieldsUsed; }
        @Override public int fieldsMax() { return fieldsMax; }
        @Override public int buildingLevel(Ids.BuildingType t) { return buildings.getOrDefault(t, 0); }
        @Override public int shipCount(Ids.ShipType t) { return ships.getOrDefault(t, 0); }
        @Override public int defenseCount(Ids.DefenseType t) { return defenses.getOrDefault(t, 0); }
        @Override public Views.ResourceView resources() { return res; }
        @Override public Views.QueueItemView currentConstruction() { return construction; }
        @Override public List<Views.QueueItemView> shipyardQueue() { return new ArrayList<Views.QueueItemView>(shipyard); }
    }

    static final class PojoEmpire implements Views.EmpireView {
        String id, name; boolean ai, player; Ids.PlayerClass pc; Ids.Difficulty difficulty;
        long score; final Map<Ids.TechType, Integer> tech = new EnumMap<Ids.TechType, Integer>(Ids.TechType.class);
        final List<PojoPlanet> planets = new ArrayList<PojoPlanet>();
        PojoQueue research;
        @Override public String id() { return id; }
        @Override public String name() { return name; }
        @Override public boolean isAI() { return ai; }
        @Override public boolean isPlayer() { return player; }
        @Override public Ids.PlayerClass playerClass() { return pc; }
        @Override public Ids.Difficulty difficulty() { return difficulty; }
        @Override public boolean alive() { return true; }
        @Override public long score() { return score; }
        @Override public int techLevel(Ids.TechType t) { return tech.getOrDefault(t, 0); }
        @Override public List<Views.PlanetView> planets() { return new ArrayList<Views.PlanetView>(planets); }
        @Override public Views.QueueItemView currentResearch() { return research; }
    }

    static final class PojoFleet implements Views.FleetMovementView {
        String id, ownerName; boolean player, returning;
        Ids.MissionType mission; int[] origin, target;
        Map<Ids.ShipType, Integer> ships; Cost cargo; int arrival, ret;
        @Override public String id() { return id; }
        @Override public String ownerName() { return ownerName; }
        @Override public boolean ownedByPlayer() { return player; }
        @Override public Ids.MissionType mission() { return mission; }
        @Override public int[] origin() { return origin; }
        @Override public int[] target() { return target; }
        @Override public Map<Ids.ShipType, Integer> ships() { return ships; }
        @Override public Cost cargo() { return cargo; }
        @Override public int departTick() { return 0; }
        @Override public int arrivalTick() { return arrival; }
        @Override public int returnTick() { return ret; }
        @Override public boolean returning() { return returning; }
        @Override public String statusText() { return returning ? "Returning" : "En route"; }
    }

    static final class PojoCell implements Views.GalaxyCellView {
        int g, s, p; String name = "", owner = ""; boolean player;
        @Override public int galaxy() { return g; }
        @Override public int system() { return s; }
        @Override public int position() { return p; }
        @Override public boolean empty() { return name.isEmpty(); }
        @Override public String ownerName() { return owner; }
        @Override public boolean ownedByPlayer() { return player; }
        @Override public boolean isAI() { return !owner.isEmpty() && !player; }
        @Override public String planetName() { return name; }
        @Override public boolean hasMoon() { return false; }
        @Override public boolean hasDebris() { return false; }
        @Override public Cost debris() { return Cost.ZERO; }
    }

    static final class PojoLog implements Views.LogEntryView {
        int tick; Ids.LogCategory cat; String msg;
        @Override public int tick() { return tick; }
        @Override public String timeText() { return "T+" + tick + "h"; }
        @Override public Ids.LogCategory category() { return cat; }
        @Override public String message() { return msg; }
    }

    static final class PojoCombat implements Views.CombatReportView {
        String id, attacker, defender, outcome; int tick; int[] loc;
        final List<String> rounds = new ArrayList<String>(); Cost debris = Cost.ZERO;
        @Override public String id() { return id; }
        @Override public int tick() { return tick; }
        @Override public String attackerName() { return attacker; }
        @Override public String defenderName() { return defender; }
        @Override public int[] location() { return loc; }
        @Override public List<String> roundSummaries() { return rounds; }
        @Override public Map<Ids.ShipType, Integer> attackerLosses() { return new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class); }
        @Override public Map<Ids.ShipType, Integer> defenderShipLosses() { return new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class); }
        @Override public Map<Ids.DefenseType, Integer> defenderDefenseLosses() { return new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class); }
        @Override public Cost debris() { return debris; }
        @Override public Cost plunder() { return Cost.ZERO; }
        @Override public boolean moonCreated() { return false; }
        @Override public String outcome() { return outcome; }
        @Override public String fullText() {
            StringBuilder sb = new StringBuilder(attacker + " vs " + defender + "\n");
            for (String r : rounds) sb.append(r).append("\n");
            sb.append(outcome);
            return sb.toString();
        }
    }

    static final class PojoExped implements Views.ExpeditionReportView {
        String id, outcome, detail; int tick, dm; Cost gains = Cost.ZERO;
        @Override public String id() { return id; }
        @Override public int tick() { return tick; }
        @Override public String outcome() { return outcome; }
        @Override public String detail() { return detail; }
        @Override public Cost gains() { return gains; }
        @Override public int darkMatter() { return dm; }
    }
}

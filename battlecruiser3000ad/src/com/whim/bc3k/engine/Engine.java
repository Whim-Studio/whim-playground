package com.whim.bc3k.engine;

import com.whim.bc3k.api.ActionResult;
import com.whim.bc3k.api.Enums;
import com.whim.bc3k.api.GameController;
import com.whim.bc3k.api.Views;
import com.whim.bc3k.sim.campaign.Campaign;
import com.whim.bc3k.sim.combat.CombatSim;
import com.whim.bc3k.sim.combat.GroundSkirmish;
import com.whim.bc3k.sim.crew.CrewMember;
import com.whim.bc3k.sim.crew.CrewRoster;
import com.whim.bc3k.sim.crew.ShipLocation;
import com.whim.bc3k.sim.galaxy.Galaxy;
import com.whim.bc3k.sim.galaxy.StarSystemNode;
import com.whim.bc3k.sim.ship.ShipSystems;
import com.whim.bc3k.save.SaveManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Phase 4 engine: binds the Phase 3 simulation modules (ship / crew / galaxy) and
 * the combat model behind the {@link GameController} seam, and drives the two
 * implemented game modes.
 *
 *  - Free Flight: no hostiles; the player explores (jump between systems) and
 *    manages power and crew. (Verified mode: exploration without attack.)
 *  - Xtreme Carnage: spawns an enemy raider; the player fights until one ship is
 *    destroyed. Loss routes to the end screen.
 *
 * Advanced Campaign Mode boots as Free Flight for now (its dynamic ticker is a
 * later phase); this is flagged, not silently dropped.
 */
public final class Engine implements GameController {

    private static final int MAX_FUEL   = 100;
    private static final int JUMP_FUEL  = 15;   // fuel consumed per jump
    private static final double FLASH_TTL = 4.0; // seconds a mission banner lingers

    private ShipSystems ship;
    private CrewRoster roster;
    private Galaxy galaxy;
    private CombatSim combat;          // null unless a space fight is active
    private GroundSkirmish ground;     // null unless a ground skirmish is active
    private Campaign campaign;         // null unless in Advanced Campaign mode

    private final GameViewImpl view = new GameViewImpl();
    private final List<String> log = new ArrayList<String>();
    private final SaveManager saves = new SaveManager("saves");
    private static final String AUTOSLOT = "auto";

    // logistics
    private int credits;
    private int fuel;
    private int spareParts;
    private int ordnance;
    private final Map<Enums.CraftType, int[]> craft =
            new EnumMap<Enums.CraftType, int[]>(Enums.CraftType.class);  // [total, launched]

    private Enums.Mode mode = Enums.Mode.MENU;
    private Enums.GameMode gameMode = Enums.GameMode.FREE_FLIGHT;
    private String shipName = "GCV Whimsy";
    private boolean started = false;
    private boolean paused = false;
    private String flash = "";
    private double flashTtl = 0;

    public Engine() {
        ship = new ShipSystems();
        roster = new CrewRoster();
        galaxy = Galaxy.defaultSector();
    }

    // ---- lifecycle ----

    @Override public Views.GameView view() { return view; }

    @Override public void newGame(Enums.GameMode gm, String name) {
        this.gameMode = gm;
        this.shipName = (name == null || name.isEmpty()) ? "GCV Whimsy" : name;
        this.started = true;
        this.paused = false;
        this.ship = new ShipSystems();
        this.galaxy = Galaxy.defaultSector();
        this.roster = new CrewRoster();
        seedCrew();
        seedLogistics();
        log.clear();

        combat = null;
        ground = null;
        campaign = null;
        if (gm == Enums.GameMode.XTREME_CARNAGE) {
            combat = new CombatSim(ship, "Gammulan Raider");
            say("Xtreme Carnage: a Gammulan raider closes. SPACE fires weapons.");
            setMode(Enums.Mode.TACTICAL);
        } else {
            if (gm == Enums.GameMode.CAMPAIGN) {
                campaign = new Campaign();
                say("Campaign: GALCOM orders — " + campaign.objective());
            } else {
                say(gm.title() + " engaged aboard " + shipName + ".");
            }
            setMode(Enums.Mode.NAV);
        }
        autosave();
    }

    private void seedCrew() {
        CrewMember cap = roster.hire("Cmdr. Reyes");
        cap.setSkill(CrewMember.Skill.COMMAND, 80);
        CrewMember eng = roster.hire("Okonkwo");
        eng.setSkill(CrewMember.Skill.ENGINEERING, 75);
        eng.orderTo(ShipLocation.ENGINEERING);
        CrewMember gun = roster.hire("Vasquez");
        gun.setSkill(CrewMember.Skill.GUNNERY, 78);
        gun.orderTo(ShipLocation.TACTICAL);
        roster.hire("Petrov");
    }

    private void seedLogistics() {
        credits = 5000;
        fuel = MAX_FUEL;
        spareParts = 12;
        ordnance = 40;
        craft.put(Enums.CraftType.FIGHTER, new int[]{ 4, 0 });
        craft.put(Enums.CraftType.SHUTTLE, new int[]{ 2, 0 });
        craft.put(Enums.CraftType.ATV,     new int[]{ 2, 0 });
    }

    @Override public void setMode(Enums.Mode m) { if (m != null) this.mode = m; }
    @Override public void togglePause() { paused = !paused; }

    // ---- save / load ----

    @Override public ActionResult save(String slot) {
        if (!started) return ActionResult.fail("Nothing to save.");
        if (saves.write(slot, snapshot())) { say("Game saved."); return ActionResult.ok("Saved."); }
        return ActionResult.fail("Save failed (I/O).");
    }

    @Override public ActionResult load(String slot) {
        Properties p = saves.read(slot);
        if (p == null) return ActionResult.fail("No save in slot '" + slot + "'.");
        restore(p);
        say("Game loaded (" + gameMode.title() + ").");
        return ActionResult.ok("Loaded.");
    }

    @Override public boolean hasSave(String slot) { return saves.exists(slot); }

    private void autosave() { if (started) saves.write(AUTOSLOT, snapshot()); }

    /** Serialize the full game state to a flat Properties map (public for round-trip testing). */
    public Properties snapshot() {
        Properties p = new Properties();
        p.setProperty("version", "1");
        p.setProperty("gameMode", gameMode.name());
        p.setProperty("shipName", shipName);
        p.setProperty("mode", mode.name());
        p.setProperty("paused", Boolean.toString(paused));
        p.setProperty("credits", Integer.toString(credits));
        p.setProperty("fuel", Integer.toString(fuel));
        p.setProperty("spareParts", Integer.toString(spareParts));
        p.setProperty("ordnance", Integer.toString(ordnance));
        p.setProperty("ship.hull", Integer.toString(ship.hull()));
        p.setProperty("ship.shields", Integer.toString(ship.shields()));
        p.setProperty("ship.reactorOnline", Boolean.toString(ship.reactorOnline()));
        for (Enums.PowerSystem s : Enums.PowerSystem.values())
            p.setProperty("ship.power." + s.name(), Integer.toString(ship.system(s).power()));
        for (Enums.CraftType t : Enums.CraftType.values()) {
            int[] c = craft.get(t);
            p.setProperty("craft." + t.name() + ".total", Integer.toString(c[0]));
            p.setProperty("craft." + t.name() + ".launched", Integer.toString(c[1]));
        }
        p.setProperty("galaxy.current", Integer.toString(galaxy.currentId()));
        StringBuilder vis = new StringBuilder();
        for (StarSystemNode n : galaxy.systems())
            if (n.visited()) { if (vis.length() > 0) vis.append(','); vis.append(n.id()); }
        p.setProperty("galaxy.visited", vis.toString());
        p.setProperty("campaign.present", campaign == null ? "0" : "1");
        if (campaign != null) {
            p.setProperty("campaign.threat", Integer.toString(campaign.threat()));
            p.setProperty("campaign.resolved", Integer.toString(campaign.resolvedCount()));
        }
        List<CrewMember> ms = roster.members();
        p.setProperty("crew.count", Integer.toString(ms.size()));
        for (int i = 0; i < ms.size(); i++) {
            CrewMember m = ms.get(i);
            p.setProperty("crew." + i + ".name", m.name());
            p.setProperty("crew." + i + ".health", Integer.toString(m.health()));
            p.setProperty("crew." + i + ".fatigue", Integer.toString(m.fatigue()));
            p.setProperty("crew." + i + ".hunger", Integer.toString(m.hunger()));
            p.setProperty("crew." + i + ".loc", m.location().name());
        }
        return p;
    }

    /** Rebuild the full game state from a snapshot (public for round-trip testing). */
    public void restore(Properties p) {
        gameMode = parseEnum(p.getProperty("gameMode"), Enums.GameMode.FREE_FLIGHT);
        shipName = p.getProperty("shipName", "GCV Whimsy");
        paused = Boolean.parseBoolean(p.getProperty("paused", "false"));
        started = true;

        credits = pi(p, "credits", 5000);
        fuel = pi(p, "fuel", MAX_FUEL);
        spareParts = pi(p, "spareParts", 0);
        ordnance = pi(p, "ordnance", 0);

        ship = new ShipSystems();
        ship.setHull(pi(p, "ship.hull", ship.maxHull()));
        ship.setShields(pi(p, "ship.shields", ship.maxShields()));
        ship.setReactorOnline(Boolean.parseBoolean(p.getProperty("ship.reactorOnline", "true")));
        for (Enums.PowerSystem s : Enums.PowerSystem.values())
            ship.setPowerAbsolute(s, pi(p, "ship.power." + s.name(), ship.system(s).power()));

        for (Enums.CraftType t : Enums.CraftType.values()) {
            int[] c = craft.get(t);
            if (c == null) { c = new int[2]; craft.put(t, c); }
            c[0] = pi(p, "craft." + t.name() + ".total", c[0]);
            c[1] = pi(p, "craft." + t.name() + ".launched", 0);
        }

        galaxy = Galaxy.defaultSector();
        for (String id : p.getProperty("galaxy.visited", "").split(",")) {
            if (!id.isEmpty()) galaxy.markVisited(Integer.parseInt(id.trim()));
        }
        galaxy.setCurrentUnchecked(pi(p, "galaxy.current", 0));

        roster = new CrewRoster();
        int n = pi(p, "crew.count", 0);
        for (int i = 0; i < n; i++) {
            roster.hireLoaded(
                    p.getProperty("crew." + i + ".name", "Crew " + i),
                    pi(p, "crew." + i + ".health", 100),
                    pi(p, "crew." + i + ".fatigue", 0),
                    pi(p, "crew." + i + ".hunger", 0),
                    parseEnum(p.getProperty("crew." + i + ".loc"), ShipLocation.BRIDGE));
        }

        if ("1".equals(p.getProperty("campaign.present"))) {
            campaign = new Campaign();
            int res = pi(p, "campaign.resolved", 0);
            campaign.loadState(pi(p, "campaign.threat", 20), res, res);
        } else {
            campaign = null;
        }
        // In-progress engagements aren't persisted; loading Xtreme Carnage re-arms a fresh raider.
        combat = gameMode == Enums.GameMode.XTREME_CARNAGE ? new CombatSim(ship, "Gammulan Raider") : null;
        ground = null;

        mode = parseEnum(p.getProperty("mode"), Enums.Mode.NAV);
        flash = "";
        flashTtl = 0;
    }

    private static int pi(Properties p, String key, int dflt) {
        try { return Integer.parseInt(p.getProperty(key, Integer.toString(dflt)).trim()); }
        catch (NumberFormatException e) { return dflt; }
    }
    private static <E extends Enum<E>> E parseEnum(String name, E dflt) {
        if (name == null) return dflt;
        try { return Enum.valueOf(dflt.getDeclaringClass(), name.trim()); }
        catch (IllegalArgumentException e) { return dflt; }
    }

    // ---- simulation tick ----

    @Override public void tick(double dt) {
        if (!started) return;
        ship.tick(dt);
        roster.tick(dt);
        // Critical hull damage scrams the reactor — the player must restart it (Shift+R).
        if (ship.reactorOnline() && ship.hull() < ship.maxHull() * 0.15) {
            ship.shutdownReactor();
            say("Reactor breach! Core scrammed — restart with Shift+R.");
        }
        if (flashTtl > 0) { flashTtl -= dt; if (flashTtl <= 0) flash = ""; }
        if (campaign != null) campaign.tick(dt);
        if (ground != null && !ground.over()) {
            ground.tick(dt);
            if (ground.over()) say(ground.playerWon()
                    ? "Ground forces cleared the LZ. ATVs returning."
                    : "Ground detachment overrun. Survivors extracting.");
        }
        if (combat != null && !combat.over()) {
            combat.tick(dt);
            if (combat.over()) {
                if (combat.playerWon()) {
                    say("Enemy destroyed. Xtreme Carnage cleared.");
                } else {
                    say("Battlecruiser lost. Mission end.");
                    setMode(Enums.Mode.GAME_OVER);
                }
            }
        }
    }

    // ---- power / engineering ----

    @Override public ActionResult setPower(Enums.PowerSystem s, int delta) {
        if (!started) return ActionResult.fail("No active game.");
        if (!ship.reactorOnline()) return ActionResult.fail("Reactor offline — restart it (Shift+R).");
        if (ship.allocate(s, delta)) return ActionResult.ok(s.label() + " power set to " + ship.system(s).power() + ".");
        return ActionResult.fail("Cannot change " + s.label() + " power (cap or budget).");
    }

    @Override public ActionResult restartReactor() {
        if (!started) return ActionResult.fail("No active game.");
        if (ship.reactorOnline()) return ActionResult.ok("Reactor already online.");
        ship.restartReactor();
        say("Reactor restarted. Power available: " + ship.reactorOutput() + " units.");
        return ActionResult.ok("Reactor online.");
    }

    @Override public ActionResult requestTow() {
        if (!started) return ActionResult.fail("No active game.");
        say("Tow request broadcast to nearest GALCOM station.");
        return ActionResult.ok("Tow requested.");
    }

    @Override public ActionResult repair(Enums.PowerSystem s) {
        if (!started) return ActionResult.fail("No active game.");
        if (ship.repairSystem(s, 20.0)) {
            say(s.label() + " integrity now " + ship.system(s).integrity() + "%.");
            return ActionResult.ok("Repairing " + s.label() + ".");
        }
        return ActionResult.fail(s.label() + " needs no repair.");
    }

    // ---- navigation ----

    @Override public ActionResult jumpTo(int systemId) {
        if (!started) return ActionResult.fail("No active game.");
        if (combat != null && !combat.over()) return ActionResult.fail("Cannot jump during combat.");
        if (!galaxy.canJumpTo(systemId)) return ActionResult.fail("No jump link to that system.");
        if (fuel < JUMP_FUEL) return ActionResult.fail("Insufficient fuel — refuel at a starstation (CARGO).");
        fuel -= JUMP_FUEL;
        galaxy.jumpTo(systemId);
        StarSystemNode n = galaxy.current();
        say("Jumped to " + n.name() + (n.hasStation() ? " (starstation in range)." : "."));
        autosave();
        return ActionResult.ok("Arrived at " + n.name() + ".");
    }

    // ---- personnel ----

    @Override public ActionResult orderCrew(int crewId, Enums.CrewOrder order) {
        if (!started) return ActionResult.fail("No active game.");
        CrewMember m = roster.byId(crewId);
        if (m == null || !m.alive()) return ActionResult.fail("No such living crew member.");
        m.orderTo(toLocation(order));
        say(m.name() + ": " + order.label() + ".");
        return ActionResult.ok(m.name() + " " + order.label() + ".");
    }

    @Override public ActionResult cloneCrew() {
        if (!started) return ActionResult.fail("No active game.");
        CrewMember m = roster.cloneFromDna();
        if (m == null) return ActionResult.fail("No DNA samples stored.");
        say("Cloned " + m.name() + " from stored DNA.");
        return ActionResult.ok("Cloned " + m.name() + ".");
    }

    private static ShipLocation toLocation(Enums.CrewOrder o) {
        switch (o) {
            case REST:           return ShipLocation.QUARTERS;
            case EAT:            return ShipLocation.GALLEY;
            case TO_ENGINEERING: return ShipLocation.ENGINEERING;
            case TO_TACTICAL:    return ShipLocation.TACTICAL;
            case TO_BRIDGE:
            default:             return ShipLocation.BRIDGE;
        }
    }

    // ---- combat ----

    @Override public ActionResult fireWeapons() {
        if (!started) return ActionResult.fail("No active game.");
        if (combat == null || combat.over()) return ActionResult.fail("No target.");
        double dmg = combat.playerVolley();
        if (dmg <= 0) return ActionResult.fail("Weapons unpowered — divert power on the PWR console.");
        return ActionResult.ok("Volley away (" + (int) dmg + " dmg).");
    }

    // ---- logistics / comms / flight deck ----

    @Override public ActionResult refuel() {
        if (!started) return ActionResult.fail("No active game.");
        if (!galaxy.current().hasStation()) return ActionResult.fail("No starstation here to refuel from.");
        if (fuel >= MAX_FUEL) return ActionResult.fail("Fuel tanks already full.");
        fuel = MAX_FUEL;
        say("Refuelled to " + MAX_FUEL + " units at " + galaxy.current().name() + ".");
        return ActionResult.ok("Refuelled.");
    }

    @Override public ActionResult hail() {
        if (!started) return ActionResult.fail("No active game.");
        StarSystemNode here = galaxy.current();
        if (here.hasStation()) say("Hailing " + here.name() + " control: \"GALCOM channel open, Commander.\"");
        else say("Hailing on open channels... no response in " + here.name() + ".");
        return ActionResult.ok("Channel opened.");
    }

    @Override public ActionResult resolveObjective() {
        if (!started) return ActionResult.fail("No active game.");
        if (campaign == null) return ActionResult.fail("Objectives only exist in Advanced Campaign mode.");
        campaign.resolveObjective();
        say("Objective complete. New orders: " + campaign.objective());
        autosave();
        return ActionResult.ok("Objective resolved.");
    }

    @Override public ActionResult launchCraft(Enums.CraftType type) {
        if (!started) return ActionResult.fail("No active game.");
        int[] c = craft.get(type);
        if (c[1] >= c[0]) return ActionResult.fail("No " + type.name().toLowerCase() + "s docked to launch.");
        c[1]++;
        // A fighter launched mid-battle joins the dogfight.
        if (type == Enums.CraftType.FIGHTER && combat != null && !combat.over()) combat.addPlayerFighters(1);
        say(type.name() + " launched (" + c[1] + "/" + c[0] + " out).");
        return ActionResult.ok(type.name() + " launched.");
    }

    @Override public ActionResult recallCraft(Enums.CraftType type) {
        if (!started) return ActionResult.fail("No active game.");
        int[] c = craft.get(type);
        if (c[1] <= 0) return ActionResult.fail("No " + type.name().toLowerCase() + "s are out.");
        c[1]--;
        if (type == Enums.CraftType.FIGHTER && combat != null && !combat.over()) combat.removePlayerFighters(1);
        say(type.name() + " recalled (" + c[1] + "/" + c[0] + " out).");
        return ActionResult.ok(type.name() + " recalled.");
    }

    @Override public ActionResult deployAtv() {
        if (!started) return ActionResult.fail("No active game.");
        int[] atv = craft.get(Enums.CraftType.ATV);
        if (atv[0] <= 0) return ActionResult.fail("No ATVs aboard to deploy.");
        if (ground != null && !ground.over()) return ActionResult.fail("Ground skirmish already underway.");
        ground = new GroundSkirmish(atv[0]);
        say("ATV detachment deployed to the surface. SPACE to assault.");
        setMode(Enums.Mode.GROUND);
        return ActionResult.ok("Ground skirmish started.");
    }

    @Override public ActionResult assaultGround() {
        if (!started) return ActionResult.fail("No active game.");
        if (ground == null || ground.over()) return ActionResult.fail("No active ground engagement.");
        double dmg = ground.assault();
        return ActionResult.ok("Assault (" + (int) dmg + " dmg).");
    }

    // ---- helpers ----

    private void say(String line) {
        log.add(line);
        while (log.size() > 60) log.remove(0);
        flash = line;
        flashTtl = FLASH_TTL;
    }

    private Enums.Alert computeAlert() {
        double frac = ship.hull() / (double) ship.maxHull();
        boolean fighting = combat != null && !combat.over();
        if (!ship.reactorOnline() || frac < 0.3) return Enums.Alert.RED;
        if (fighting || frac < 0.6) return Enums.Alert.YELLOW;
        return Enums.Alert.GREEN;
    }

    // ---- view projections ----

    private final class ShipViewImpl implements Views.ShipView {
        @Override public String name() { return shipName; }
        @Override public int hull() { return ship.hull(); }
        @Override public int maxHull() { return ship.maxHull(); }
        @Override public int shields() { return ship.shields(); }
        @Override public int maxShields() { return ship.maxShields(); }
        @Override public boolean reactorOnline() { return ship.reactorOnline(); }
        @Override public int reactorOutput() { return ShipSystems.REACTOR_OUTPUT; }
        @Override public int reactorUsed() { return ship.reactorUsed(); }
        @Override public int power(Enums.PowerSystem s) { return ship.system(s).power(); }
        @Override public int integrity(Enums.PowerSystem s) { return ship.system(s).integrity(); }
        @Override public boolean breached(Enums.PowerSystem s) { return ship.system(s).breached(); }
        @Override public int maxPerSystem() { return ShipSystems.MAX_PER_SYSTEM; }
        @Override public Enums.Alert alert() { return computeAlert(); }
    }

    private final class CrewViewImpl implements Views.CrewView {
        private final CrewMember m;
        CrewViewImpl(CrewMember m) { this.m = m; }
        @Override public int id() { return m.id(); }
        @Override public String name() { return m.name(); }
        @Override public int health() { return m.health(); }
        @Override public int fatigue() { return m.fatigue(); }
        @Override public int hunger() { return m.hunger(); }
        @Override public String location() {
            return (m.destination() != null ? "→ " + m.destination().name() : m.location().name());
        }
        @Override public boolean alive() { return m.alive(); }
    }

    private final class SystemViewImpl implements Views.SystemView {
        private final StarSystemNode n;
        SystemViewImpl(StarSystemNode n) { this.n = n; }
        @Override public int id() { return n.id(); }
        @Override public String name() { return n.name(); }
        @Override public int x() { return n.x(); }
        @Override public int y() { return n.y(); }
        @Override public boolean visited() { return n.visited(); }
        @Override public boolean hasStation() { return n.hasStation(); }
        @Override public boolean current() { return n.id() == galaxy.currentId(); }
        @Override public boolean reachable() { return galaxy.canJumpTo(n.id()); }
        @Override public List<Integer> links() { return n.links(); }
    }

    private final class GalaxyViewImpl implements Views.GalaxyView {
        @Override public int currentId() { return galaxy.currentId(); }
        @Override public List<Views.SystemView> systems() {
            List<Views.SystemView> out = new ArrayList<Views.SystemView>();
            for (StarSystemNode n : galaxy.systems()) out.add(new SystemViewImpl(n));
            return out;
        }
    }

    private final class CombatViewImpl implements Views.CombatView {
        @Override public String enemyName() { return combat.enemyName(); }
        @Override public int enemyHull() { return combat.enemy().hull(); }
        @Override public int enemyMaxHull() { return combat.enemy().maxHull(); }
        @Override public int enemyShields() { return combat.enemy().shields(); }
        @Override public int enemyMaxShields() { return combat.enemy().maxShields(); }
        @Override public int playerFighters() { return combat.playerFighters(); }
        @Override public int enemyFighters() { return combat.enemyFighters(); }
        @Override public boolean over() { return combat.over(); }
        @Override public boolean playerWon() { return combat.playerWon(); }
    }

    private final class GroundViewImpl implements Views.GroundView {
        @Override public int playerHp() { return ground.playerHp(); }
        @Override public int playerMaxHp() { return ground.playerMaxHp(); }
        @Override public int enemyHp() { return ground.enemyHp(); }
        @Override public int enemyMaxHp() { return ground.enemyMaxHp(); }
        @Override public boolean over() { return ground.over(); }
        @Override public boolean playerWon() { return ground.playerWon(); }
    }

    private final class CargoViewImpl implements Views.CargoView {
        @Override public int credits() { return credits; }
        @Override public int fuel() { return fuel; }
        @Override public int maxFuel() { return MAX_FUEL; }
        @Override public int spareParts() { return spareParts; }
        @Override public int ordnance() { return ordnance; }
    }

    private final class CraftViewImpl implements Views.CraftView {
        private final Enums.CraftType type;
        CraftViewImpl(Enums.CraftType type) { this.type = type; }
        @Override public Enums.CraftType type() { return type; }
        @Override public int total() { return craft.get(type)[0]; }
        @Override public int launched() { return craft.get(type)[1]; }
        @Override public int docked() { return craft.get(type)[0] - craft.get(type)[1]; }
    }

    private final class CampaignViewImpl implements Views.CampaignView {
        @Override public int threat() { return campaign.threat(); }
        @Override public boolean critical() { return campaign.critical(); }
        @Override public int resolved() { return campaign.resolvedCount(); }
        @Override public String objective() { return campaign.objective(); }
    }

    private final class GameViewImpl implements Views.GameView {
        private final ShipViewImpl shipView = new ShipViewImpl();
        private final GalaxyViewImpl galaxyView = new GalaxyViewImpl();
        private final CargoViewImpl cargoView = new CargoViewImpl();
        @Override public Enums.Mode mode() { return mode; }
        @Override public Enums.GameMode gameMode() { return gameMode; }
        @Override public boolean paused() { return paused; }
        @Override public boolean started() { return started; }
        @Override public Views.ShipView ship() { return shipView; }
        @Override public Views.GalaxyView galaxy() { return galaxyView; }
        @Override public List<Views.CrewView> crew() {
            List<Views.CrewView> out = new ArrayList<Views.CrewView>();
            for (CrewMember m : roster.members()) out.add(new CrewViewImpl(m));
            return out;
        }
        @Override public Views.CargoView cargo() { return cargoView; }
        @Override public List<Views.CraftView> craft() {
            List<Views.CraftView> out = new ArrayList<Views.CraftView>();
            for (Enums.CraftType t : Enums.CraftType.values()) out.add(new CraftViewImpl(t));
            return out;
        }
        @Override public Views.CombatView combat() {
            return combat == null ? null : new CombatViewImpl();
        }
        @Override public Views.GroundView ground() {
            return ground == null ? null : new GroundViewImpl();
        }
        @Override public Views.CampaignView campaign() {
            return campaign == null ? null : new CampaignViewImpl();
        }
        @Override public List<String> log() { return log; }
        @Override public String flash() { return flash; }
    }
}

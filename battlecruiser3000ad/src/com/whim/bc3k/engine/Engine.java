package com.whim.bc3k.engine;

import com.whim.bc3k.api.ActionResult;
import com.whim.bc3k.api.Enums;
import com.whim.bc3k.api.GameController;
import com.whim.bc3k.api.Views;
import com.whim.bc3k.sim.campaign.Campaign;
import com.whim.bc3k.sim.combat.CombatSim;
import com.whim.bc3k.sim.crew.CrewMember;
import com.whim.bc3k.sim.crew.CrewRoster;
import com.whim.bc3k.sim.crew.ShipLocation;
import com.whim.bc3k.sim.galaxy.Galaxy;
import com.whim.bc3k.sim.galaxy.StarSystemNode;
import com.whim.bc3k.sim.ship.ShipSystems;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
    private CombatSim combat;          // null unless a fight is active
    private Campaign campaign;         // null unless in Advanced Campaign mode

    private final GameViewImpl view = new GameViewImpl();
    private final List<String> log = new ArrayList<String>();

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

    // ---- simulation tick ----

    @Override public void tick(double dt) {
        if (!started) return;
        ship.tick(dt);
        roster.tick(dt);
        if (flashTtl > 0) { flashTtl -= dt; if (flashTtl <= 0) flash = ""; }
        if (campaign != null) campaign.tick(dt);
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
        return ActionResult.ok("Objective resolved.");
    }

    @Override public ActionResult launchCraft(Enums.CraftType type) {
        if (!started) return ActionResult.fail("No active game.");
        int[] c = craft.get(type);
        if (c[1] >= c[0]) return ActionResult.fail("No " + type.name().toLowerCase() + "s docked to launch.");
        c[1]++;
        say(type.name() + " launched (" + c[1] + "/" + c[0] + " out).");
        return ActionResult.ok(type.name() + " launched.");
    }

    @Override public ActionResult recallCraft(Enums.CraftType type) {
        if (!started) return ActionResult.fail("No active game.");
        int[] c = craft.get(type);
        if (c[1] <= 0) return ActionResult.fail("No " + type.name().toLowerCase() + "s are out.");
        c[1]--;
        say(type.name() + " recalled (" + c[1] + "/" + c[0] + " out).");
        return ActionResult.ok(type.name() + " recalled.");
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
        @Override public boolean over() { return combat.over(); }
        @Override public boolean playerWon() { return combat.playerWon(); }
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
        @Override public Views.CampaignView campaign() {
            return campaign == null ? null : new CampaignViewImpl();
        }
        @Override public List<String> log() { return log; }
        @Override public String flash() { return flash; }
    }
}

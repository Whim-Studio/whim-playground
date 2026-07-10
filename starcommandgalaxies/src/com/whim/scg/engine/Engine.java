package com.whim.scg.engine;

import com.whim.scg.api.ActionResult;
import com.whim.scg.api.Defs;
import com.whim.scg.api.Enums;
import com.whim.scg.api.GameController;
import com.whim.scg.api.GridPos;
import com.whim.scg.api.Views;
import com.whim.scg.content.Content;
import com.whim.scg.model.BoardingModel;
import com.whim.scg.model.CombatModel;
import com.whim.scg.model.CrewModel;
import com.whim.scg.model.GameState;
import com.whim.scg.model.RoomModel;
import com.whim.scg.model.ShipModel;
import com.whim.scg.model.StarSystemModel;
import com.whim.scg.model.TechModel;
import com.whim.scg.model.WeaponModel;
import com.whim.scg.save.SaveManager;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The whole Star Command: Galaxies simulation. Implements the UI seam
 * {@link GameController}: intents in, {@link Views} out. Never throws from an
 * intent — illegal requests return {@link ActionResult#fail}.
 */
public final class Engine implements GameController {

    private GameState state = new GameState();
    private Content content;
    private Random rng = new Random(1);
    private Factory factory;
    private CombatSim combatSim;
    private BoardingSim boardingSim;
    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    /** Public no-arg constructor — the shell instantiates this reflectively. */
    public Engine() {
        try {
            content = Content.load();
        } catch (Exception e) {
            content = null; // degrade gracefully; newGame will report the failure
        }
        rebuildHelpers();
    }

    private void rebuildHelpers() {
        factory = content != null ? new Factory(content, rng) : null;
        combatSim = new CombatSim(rng);
        boardingSim = new BoardingSim(rng);
    }

    // ================================================================ lifecycle
    @Override public Views.GameView view() { return state; }

    @Override public void addChangeListener(ChangeListener l) {
        if (l != null) listeners.add(l);
    }

    private void fire() {
        ChangeEvent ev = new ChangeEvent(this);
        for (ChangeListener l : listeners) {
            try { l.stateChanged(ev); } catch (Exception ignored) {}
        }
    }

    @Override public void newGame(String captainName, String shipName) {
        try {
            if (content == null) content = Content.load();
            rng = new Random(newSeed());
            rebuildHelpers();
            Enums.Mode prev = state.mode; // "mode stays as the caller sets it"
            GameState st = new GameState();
            st.seed = seedOf(rng);
            st.mode = prev;
            st.captainName = (captainName == null || captainName.isEmpty()) ? "Captain" : captainName;
            st.credits = 150;
            st.day = 1;
            factory.initTech(st);
            Defs.ShipDef corvette = content.ship("corvette");
            st.playerShip = factory.buildShip(st, corvette, shipName);
            factory.addStartingCrew(st, st.playerShip, st.captainName);
            defaultPower(st.playerShip);
            st.galaxy = factory.buildGalaxy(st);
            st.logLine("Commissioned " + st.playerShip.name + " under " + st.captainName);
            st.setFlash("New voyage begins — reach The Maw");
            state = st;
            fire();
        } catch (Exception e) {
            state.logLine("newGame failed: " + e.getMessage());
        }
    }

    @Override public void setMode(Enums.Mode mode) {
        if (mode == null) return;
        state.mode = mode;
        // make navigated-to combat lively for the demo if nothing is staged
        if (mode == Enums.Mode.SPACE_COMBAT && state.combat == null && state.playerShip != null && factory != null) {
            startCombat(false);
        }
        fire();
    }

    @Override public void tick(double dt) {
        if (dt <= 0) return;
        if (state.flashTime > 0) {
            state.flashTime -= dt;
            if (state.flashTime <= 0) state.flash = "";
        }
        if (state.paused) return;
        try {
            if (state.mode == Enums.Mode.SPACE_COMBAT && state.combat != null) {
                combatSim.tick(state, dt);
                if (state.combat.over) resolveCombatEnd();
            } else if (state.mode == Enums.Mode.BOARDING && state.boarding != null) {
                boardingSim.tick(state, dt);
                if (state.boarding.over) resolveBoardingEnd();
            }
        } catch (Exception e) {
            state.logLine("tick error: " + e.getMessage());
        }
    }

    @Override public void togglePause() {
        state.paused = !state.paused;
        fire();
    }

    // ================================================================ save/load
    @Override public ActionResult save(final String slot) {
        return guard(new Op() { public ActionResult run() {
            return SaveManager.save(state, slot);
        }});
    }

    @Override public ActionResult load(final String slot) {
        return guard(new Op() { public ActionResult run() {
            GameState loaded = SaveManager.load(slot, content);
            if (loaded == null) return ActionResult.fail("no save in slot '" + slot + "'");
            state = loaded;
            rng = new Random(state.seed == 0 ? newSeed() : state.seed);
            rebuildHelpers();
            if (state.combat != null) state.combat.playerShip = state.playerShip;
            fire();
            return ActionResult.ok("loaded '" + slot + "'");
        }});
    }

    // ================================================================ crew
    @Override public ActionResult assignCrew(final int crewId, final int roomId) {
        return guard(new Op() { public ActionResult run() {
            ShipModel ship = state.playerShip;
            if (ship == null) return ActionResult.fail("no ship");
            CrewModel c = ship.crewById(crewId);
            if (c == null) return ActionResult.fail("no such crew");
            RoomModel r = ship.room(roomId);
            if (r == null) return ActionResult.fail("no such room");
            if (c.stationRoomId >= 0) {
                RoomModel old = ship.room(c.stationRoomId);
                if (old != null) old.crewIds.remove(Integer.valueOf(crewId));
            }
            c.stationRoomId = roomId;
            if (!r.crewIds.contains(crewId)) r.crewIds.add(crewId);
            fire();
            return ActionResult.ok(c.name + " → " + r.type);
        }});
    }

    @Override public ActionResult renameCrew(final int crewId, final String name) {
        return guard(new Op() { public ActionResult run() {
            ShipModel ship = state.playerShip;
            if (ship == null) return ActionResult.fail("no ship");
            CrewModel c = ship.crewById(crewId);
            if (c == null) return ActionResult.fail("no such crew");
            if (name == null || name.trim().isEmpty()) return ActionResult.fail("empty name");
            c.name = name.trim();
            fire();
            return ActionResult.ok();
        }});
    }

    @Override public ActionResult setRole(final int crewId, final Enums.CrewRole role) {
        return guard(new Op() { public ActionResult run() {
            ShipModel ship = state.playerShip;
            if (ship == null) return ActionResult.fail("no ship");
            CrewModel c = ship.crewById(crewId);
            if (c == null) return ActionResult.fail("no such crew");
            if (role == null) return ActionResult.fail("null role");
            c.role = role;
            fire();
            return ActionResult.ok(c.name + " is now " + role);
        }});
    }

    // ================================================================ power
    @Override public ActionResult setRoomPower(final int roomId, final int power) {
        return guard(new Op() { public ActionResult run() {
            ShipModel ship = state.playerShip;
            if (ship == null) return ActionResult.fail("no ship");
            RoomModel r = ship.room(roomId);
            if (r == null) return ActionResult.fail("no such room");
            if (!r.type.isPowered() || r.maxPower <= 0) return ActionResult.fail(r.type + " takes no power");
            int want = clamp(power, 0, r.maxPower);
            int delta = want - r.power;
            if (delta > ship.reactorFree()) want = r.power + Math.max(0, ship.reactorFree());
            r.power = want;
            fire();
            return ActionResult.ok();
        }});
    }

    @Override public ActionResult setWeaponPower(final int slot, final int power) {
        return guard(new Op() { public ActionResult run() {
            ShipModel ship = state.playerShip;
            if (ship == null) return ActionResult.fail("no ship");
            WeaponModel w = weapon(ship, slot);
            if (w == null) return ActionResult.fail("no such weapon");
            int want = clamp(power, 0, w.reqPower);
            int delta = want - w.powered;
            if (delta > ship.reactorFree()) want = w.powered + Math.max(0, ship.reactorFree());
            w.powered = want;
            if (w.powered == 0) w.charge = 0;
            fire();
            return ActionResult.ok();
        }});
    }

    // ================================================================ galaxy
    @Override public ActionResult jumpTo(final int systemId) {
        return guard(new Op() { public ActionResult run() {
            if (state.galaxy == null) return ActionResult.fail("no galaxy");
            if (state.mode == Enums.Mode.SPACE_COMBAT) return ActionResult.fail("cannot jump mid-battle");
            StarSystemModel cur = state.galaxy.current();
            if (cur == null) return ActionResult.fail("lost in space");
            if (systemId == cur.id) return ActionResult.fail("already here");
            if (!cur.links.contains(Integer.valueOf(systemId))) return ActionResult.fail("no jump route");
            StarSystemModel dest = state.galaxy.system(systemId);
            if (dest == null) return ActionResult.fail("no such system");
            state.galaxy.currentSystem = systemId;
            dest.visited = true;
            state.day++;
            state.mode = Enums.Mode.GALAXY_MAP;
            state.logLine("Jumped to " + dest.name + " (day " + state.day + ")");
            // arriving hazards
            if (dest.isBoss && !state.bossDefeated) {
                startBossCombat(dest);
                return ActionResult.ok("The Maw awaits...");
            }
            if (dest.pendingEvent == Enums.EventType.PIRATE_AMBUSH) {
                startCombat(false);
                dest.pendingEvent = Enums.EventType.NOTHING;
                return ActionResult.ok("Ambushed at " + dest.name + "!");
            }
            fire();
            return ActionResult.ok("Arrived at " + dest.name);
        }});
    }

    @Override public ActionResult scanSystem() {
        return guard(new Op() { public ActionResult run() {
            if (state.galaxy == null) return ActionResult.fail("no galaxy");
            StarSystemModel cur = state.galaxy.current();
            if (cur == null) return ActionResult.fail("lost in space");
            cur.scanned = true;
            int reveal = 1 + techLevel(Enums.TechType.SENSORS);
            int done = 0;
            for (Integer id : cur.links) {
                StarSystemModel n = state.galaxy.system(id);
                if (n != null && !n.scanned) { n.scanned = true; if (++done >= reveal) break; }
            }
            fire();
            return ActionResult.ok("Scanned " + cur.name + ": " + describeEvent(cur.pendingEvent));
        }});
    }

    @Override public ActionResult resolveEvent(final int choiceIndex) {
        return guard(new Op() { public ActionResult run() {
            if (state.galaxy == null) return ActionResult.fail("no galaxy");
            StarSystemModel cur = state.galaxy.current();
            if (cur == null) return ActionResult.fail("lost in space");
            Enums.EventType ev = cur.pendingEvent;
            if (ev == null || ev == Enums.EventType.NOTHING) return ActionResult.fail("nothing to resolve here");
            ActionResult res = applyEvent(cur, ev, choiceIndex);
            fire();
            return res;
        }});
    }

    @Override public ActionResult dock() {
        return guard(new Op() { public ActionResult run() {
            if (state.galaxy == null) return ActionResult.fail("no galaxy");
            StarSystemModel cur = state.galaxy.current();
            if (cur == null || !cur.hasStarport) return ActionResult.fail("no starport here");
            state.mode = Enums.Mode.STARPORT;
            state.logLine("Docked at " + cur.name + " starport");
            fire();
            return ActionResult.ok();
        }});
    }

    // ================================================================ economy
    @Override public ActionResult repairAll() {
        return guard(new Op() { public ActionResult run() {
            ShipModel ship = state.playerShip;
            if (ship == null) return ActionResult.fail("no ship");
            if (state.mode != Enums.Mode.STARPORT) return ActionResult.fail("must be docked");
            int missing = (ship.maxHull - ship.hull);
            for (RoomModel r : ship.rooms) missing += (r.maxHp - r.hp);
            int cost = Math.max(0, missing) * 3;
            if (cost == 0) return ActionResult.ok("already pristine");
            if (state.credits < cost) return ActionResult.fail("need " + cost + " credits");
            state.credits -= cost;
            ship.hull = ship.maxHull;
            for (RoomModel r : ship.rooms) { r.hp = r.maxHp; r.onFire = false; r.breached = false; r.fireTime = 0; }
            ship.oxygen = 100;
            for (CrewModel c : ship.crew) if (c.alive()) c.hp = c.maxHp;
            state.logLine("Repaired ship for " + cost + " credits");
            fire();
            return ActionResult.ok("Ship repaired");
        }});
    }

    @Override public ActionResult buyTech(final Enums.TechType type) {
        return guard(new Op() { public ActionResult run() {
            if (type == null) return ActionResult.fail("null tech");
            if (state.mode != Enums.Mode.STARPORT) return ActionResult.fail("must be docked");
            TechModel t = tech(type);
            if (t == null) return ActionResult.fail("no such tech");
            if (t.maxed()) return ActionResult.fail(t.name + " maxed");
            int cost = t.cost();
            if (state.credits < cost) return ActionResult.fail("need " + cost + " credits");
            state.credits -= cost;
            t.level++;
            applyTechEffect(type);
            state.logLine("Upgraded " + t.name + " to Lv" + t.level);
            fire();
            return ActionResult.ok(t.name + " Lv" + t.level);
        }});
    }

    @Override public ActionResult recruitCrew() {
        return guard(new Op() { public ActionResult run() {
            ShipModel ship = state.playerShip;
            if (ship == null) return ActionResult.fail("no ship");
            if (state.mode != Enums.Mode.STARPORT) return ActionResult.fail("must be docked");
            int cost = 90 + ship.crew.size() * 10;
            if (state.credits < cost) return ActionResult.fail("need " + cost + " credits");
            if (ship.crew.size() >= 8) return ActionResult.fail("crew quarters full");
            state.credits -= cost;
            Enums.CrewRole[] pool = {
                    Enums.CrewRole.SECURITY, Enums.CrewRole.SHIELD_TECH,
                    Enums.CrewRole.MEDIC, Enums.CrewRole.SCIENCE, Enums.CrewRole.ENGINEER };
            CrewModel c = factory.makeCrew(state, pool[rng.nextInt(pool.length)], Enums.Faction.FEDERATION, null, 30 + rng.nextInt(20));
            c.name = randomName();
            ship.crew.add(c);
            RoomModel q = ship.firstRoom(Enums.RoomType.QUARTERS);
            if (q != null) { c.stationRoomId = q.id; if (!q.crewIds.contains(c.id)) q.crewIds.add(c.id); }
            state.logLine("Recruited " + c.name + " (" + c.role + ")");
            fire();
            return ActionResult.ok("Recruited " + c.name);
        }});
    }

    @Override public ActionResult undock() {
        return guard(new Op() { public ActionResult run() {
            if (state.mode != Enums.Mode.STARPORT) return ActionResult.fail("not docked");
            state.mode = Enums.Mode.GALAXY_MAP;
            fire();
            return ActionResult.ok();
        }});
    }

    // ================================================================ combat
    @Override public ActionResult setWeaponTarget(final int slot, final int enemyRoomId) {
        return guard(new Op() { public ActionResult run() {
            if (state.combat == null) return ActionResult.fail("not in combat");
            WeaponModel w = weapon(state.playerShip, slot);
            if (w == null) return ActionResult.fail("no such weapon");
            if (enemyRoomId >= 0 && state.combat.enemyShip.room(enemyRoomId) == null)
                return ActionResult.fail("no such enemy room");
            w.targetRoomId = enemyRoomId;
            fire();
            return ActionResult.ok();
        }});
    }

    @Override public ActionResult fireWeapon(final int slot) {
        return guard(new Op() { public ActionResult run() {
            if (state.combat == null) return ActionResult.fail("not in combat");
            boolean ok = combatSim.manualFire(state.combat, slot);
            if (!ok) return ActionResult.fail("weapon not ready");
            fire();
            return ActionResult.ok("fired");
        }});
    }

    @Override public ActionResult beginBoarding() {
        return guard(new Op() { public ActionResult run() {
            CombatModel cb = state.combat;
            if (cb == null) return ActionResult.fail("not in combat");
            if (!cb.canBoard()) return ActionResult.fail("enemy shields still up");
            List<CrewModel> party = pickBoardingParty();
            if (party.isEmpty()) return ActionResult.fail("no crew to board with");
            BoardingModel b = factory.buildBoarding(state, cb.enemyShip, party);
            state.boarding = b;
            cb.boarded = true;
            state.mode = Enums.Mode.BOARDING;
            state.logLine("Boarding party teleported aboard " + cb.enemyShip.name);
            fire();
            return ActionResult.ok("Boarding!");
        }});
    }

    @Override public ActionResult fleeCombat() {
        return guard(new Op() { public ActionResult run() {
            CombatModel cb = state.combat;
            if (cb == null) return ActionResult.fail("not in combat");
            double chance = 0.45 + Rules.evasion(state.playerShip);
            if (rng.nextDouble() < chance) {
                state.combat = null;
                state.mode = Enums.Mode.GALAXY_MAP;
                state.logLine("Disengaged and jumped clear");
                state.setFlash("Escaped the battle");
                fire();
                return ActionResult.ok("Escaped");
            }
            // failed flee: take a hit
            state.playerShip.hull = Math.max(0, state.playerShip.hull - 2);
            state.logLine("Failed to disengage");
            if (state.playerShip.hull <= 0) { cb.over = true; cb.playerWon = false; resolveCombatEnd(); }
            fire();
            return ActionResult.fail("Could not disengage");
        }});
    }

    // ================================================================ boarding
    @Override public ActionResult selectBoarder(final int crewId) {
        return guard(new Op() { public ActionResult run() {
            BoardingModel b = state.boarding;
            if (b == null) return ActionResult.fail("not boarding");
            CrewModel c = b.friendlyById(crewId);
            if (c == null || !c.alive()) return ActionResult.fail("cannot select");
            b.selectedCrewId = crewId;
            fire();
            return ActionResult.ok();
        }});
    }

    @Override public ActionResult moveBoarder(final int crewId, final GridPos to) {
        return guard(new Op() { public ActionResult run() {
            BoardingModel b = state.boarding;
            if (b == null) return ActionResult.fail("not boarding");
            CrewModel c = b.friendlyById(crewId);
            if (c == null) return ActionResult.fail("no such boarder");
            if (!boardingSim.move(b, c, to)) return ActionResult.fail("cannot move there");
            boardingSim.checkEnd(state, b);
            if (b.over) resolveBoardingEnd();
            fire();
            return ActionResult.ok();
        }});
    }

    @Override public ActionResult boarderAttack(final int crewId, final int targetCrewId) {
        return guard(new Op() { public ActionResult run() {
            BoardingModel b = state.boarding;
            if (b == null) return ActionResult.fail("not boarding");
            CrewModel a = b.friendlyById(crewId);
            if (a == null) return ActionResult.fail("no such boarder");
            CrewModel t = null;
            for (CrewModel h : b.hostiles) if (h.id == targetCrewId) { t = h; break; }
            if (t == null) return ActionResult.fail("no such target");
            if (!boardingSim.attack(state, b, a, t)) return ActionResult.fail("target not adjacent");
            boardingSim.checkEnd(state, b);
            if (b.over) resolveBoardingEnd();
            fire();
            return ActionResult.ok();
        }});
    }

    @Override public ActionResult endBoarding() {
        return guard(new Op() { public ActionResult run() {
            BoardingModel b = state.boarding;
            if (b == null) return ActionResult.fail("not boarding");
            // recall survivors to the ship
            for (CrewModel c : b.friendlies) c.boardingPos = null;
            state.boarding = null;
            state.mode = state.combat != null ? Enums.Mode.SPACE_COMBAT : Enums.Mode.SHIP_INTERIOR;
            state.logLine("Boarding party recalled");
            fire();
            return ActionResult.ok("Recalled");
        }});
    }

    // ================================================================ internals
    private void startCombat(boolean boss) {
        CombatModel cb = new CombatModel();
        cb.playerShip = state.playerShip;
        cb.enemyShip = factory.buildEnemyShip(state, boss);
        cb.salvage = boss ? 500 : (60 + rng.nextInt(90));
        prepareForCombat(state.playerShip);
        state.combat = cb;
        state.boarding = null;
        state.mode = Enums.Mode.SPACE_COMBAT;
        state.paused = false;
        state.logLine("Engaged " + cb.enemyShip.name);
        fire();
    }

    private void startBossCombat(StarSystemModel bossSys) {
        startCombat(true);
        state.setFlash("Final battle: " + bossSys.name);
    }

    private void resolveCombatEnd() {
        CombatModel cb = state.combat;
        if (cb == null) return;
        boolean boss = cb.enemyShip != null && cb.enemyShip.faction == Enums.Faction.ALIEN_SWARM;
        if (cb.playerWon) {
            state.credits += cb.salvage;
            state.logLine("Victory! Salvaged " + cb.salvage + " credits");
            for (CrewModel c : state.playerShip.crew) if (c.alive()) c.gainXp(30);
            StarSystemModel cur = state.galaxy != null ? state.galaxy.current() : null;
            if (cur != null) cur.pendingEvent = Enums.EventType.NOTHING;
            state.combat = null;
            state.boarding = null;
            if (boss) {
                state.bossDefeated = true;
                state.mode = Enums.Mode.VICTORY;
                state.setFlash("The Maw is destroyed. You win!");
            } else {
                state.mode = Enums.Mode.GALAXY_MAP;
                state.setFlash("Enemy destroyed (+" + cb.salvage + ")");
            }
        } else {
            state.combat = null;
            state.boarding = null;
            state.mode = Enums.Mode.GAME_OVER;
            state.setFlash("Ship lost. Game over.");
            state.logLine("The " + state.playerShip.name + " was destroyed");
        }
        fire();
    }

    private void resolveBoardingEnd() {
        BoardingModel b = state.boarding;
        if (b == null) return;
        CombatModel cb = state.combat;
        if (b.playerWon) {
            state.logLine("Enemy ship seized!");
            for (CrewModel c : b.friendlies) { c.boardingPos = null; if (c.alive()) c.gainXp(40); }
            if (cb != null && cb.enemyShip != null) cb.enemyShip.hull = 0;
            state.boarding = null;
            if (cb != null) { cb.over = true; cb.playerWon = true; resolveCombatEnd(); }
            else state.mode = Enums.Mode.SHIP_INTERIOR;
        } else {
            state.logLine("Boarding party wiped out");
            for (CrewModel c : b.friendlies) c.boardingPos = null;
            state.boarding = null;
            state.mode = cb != null ? Enums.Mode.SPACE_COMBAT : Enums.Mode.SHIP_INTERIOR;
            state.setFlash("Away team lost");
        }
        fire();
    }

    private List<CrewModel> pickBoardingParty() {
        List<CrewModel> alive = new ArrayList<CrewModel>();
        for (CrewModel c : state.playerShip.crew) if (c.alive()) alive.add(c);
        Collections.sort(alive, new Comparator<CrewModel>() {
            public int compare(CrewModel a, CrewModel b) {
                return b.skill(Enums.StatType.COMBAT) - a.skill(Enums.StatType.COMBAT);
            }
        });
        int max = 2 + techLevel(Enums.TechType.TELEPORTER);
        List<CrewModel> party = new ArrayList<CrewModel>();
        for (int i = 0; i < alive.size() && party.size() < max; i++) party.add(alive.get(i));
        return party;
    }

    private ActionResult applyEvent(StarSystemModel sys, Enums.EventType ev, int choice) {
        boolean act = choice == 0; // 0 = engage/investigate, else = ignore
        switch (ev) {
            case DERELICT: {
                if (act) {
                    int loot = 40 + rng.nextInt(80);
                    state.credits += loot;
                    sys.pendingEvent = Enums.EventType.NOTHING;
                    if (rng.nextInt(100) < 25 && state.playerShip != null) {
                        state.playerShip.hull = Math.max(1, state.playerShip.hull - 2);
                        state.logLine("Salvaged derelict (+" + loot + ") but hit debris (-2 hull)");
                    } else {
                        state.logLine("Salvaged derelict (+" + loot + " credits)");
                    }
                    return ActionResult.ok("Salvaged +" + loot);
                }
                sys.pendingEvent = Enums.EventType.NOTHING;
                return ActionResult.ok("Left the derelict alone");
            }
            case DISTRESS: {
                if (act) {
                    int reward = 60 + rng.nextInt(70);
                    state.credits += reward;
                    sys.pendingEvent = Enums.EventType.NOTHING;
                    state.logLine("Answered distress call (+" + reward + ")");
                    return ActionResult.ok("Rescued survivors +" + reward);
                }
                sys.pendingEvent = Enums.EventType.NOTHING;
                return ActionResult.ok("Ignored the distress call");
            }
            case MERCHANT: {
                if (act) {
                    sys.hasStarport = true;
                    sys.pendingEvent = Enums.EventType.NOTHING;
                    state.logLine("Merchant convoy — trade post available, dock to trade");
                    return ActionResult.ok("Merchant will trade — dock here");
                }
                sys.pendingEvent = Enums.EventType.NOTHING;
                return ActionResult.ok("Waved the merchant on");
            }
            case HAZARD: {
                if (act) {
                    int dmg = 2 + rng.nextInt(4);
                    if (state.playerShip != null) state.playerShip.hull = Math.max(1, state.playerShip.hull - dmg);
                    sys.pendingEvent = Enums.EventType.NOTHING;
                    state.logLine("Pushed through the hazard (-" + dmg + " hull)");
                    return ActionResult.ok("Weathered hazard (-" + dmg + " hull)");
                }
                sys.pendingEvent = Enums.EventType.NOTHING;
                return ActionResult.ok("Detoured around the hazard");
            }
            case PIRATE_AMBUSH: {
                sys.pendingEvent = Enums.EventType.NOTHING;
                startCombat(false);
                return ActionResult.ok("Pirates attack!");
            }
            case STORY: {
                if (sys.isBoss && !state.bossDefeated) {
                    startBossCombat(sys);
                    return ActionResult.ok("The Maw stirs...");
                }
                sys.pendingEvent = Enums.EventType.NOTHING;
                state.credits += 50;
                return ActionResult.ok("A quiet mystery (+50)");
            }
            default:
                sys.pendingEvent = Enums.EventType.NOTHING;
                return ActionResult.ok();
        }
    }

    private void applyTechEffect(Enums.TechType type) {
        ShipModel s = state.playerShip;
        if (s == null) return;
        switch (type) {
            case SHIELDS:
                s.maxShields += 2; s.shields = Math.min(s.maxShields, s.shields + 2); break;
            case HULL:
                s.maxHull += 8; s.hull += 8; break;
            case ENGINES:
                s.reactor += 1; break;
            case WEAPONS:
                for (WeaponModel w : s.weapons) { w.chargeMax = Math.max(3, w.chargeMax - 1); } break;
            case MEDBAY:
                for (CrewModel c : s.crew) if (c.alive()) c.hp = c.maxHp; break;
            case TELEPORTER:
            case SENSORS:
            default:
                break; // read dynamically via techLevel()
        }
    }

    // default combat-ready power allocation for a fresh/quiet ship
    private void defaultPower(ShipModel ship) {
        for (RoomModel r : ship.rooms) r.power = 0;
        for (WeaponModel w : ship.weapons) w.powered = 0;
        setPowerIfPresent(ship, Enums.RoomType.SHIELDS, 2);
        setPowerIfPresent(ship, Enums.RoomType.ENGINES, 1);
        setPowerIfPresent(ship, Enums.RoomType.WEAPONS, 1);
        setPowerIfPresent(ship, Enums.RoomType.OXYGEN, 1);
        for (WeaponModel w : ship.weapons) {
            if (ship.reactorFree() >= w.reqPower) w.powered = w.reqPower;
        }
    }

    private void prepareForCombat(ShipModel ship) {
        if (ship == null) return;
        boolean anyPowered = false;
        for (WeaponModel w : ship.weapons) if (w.powered > 0) anyPowered = true;
        if (!anyPowered) defaultPower(ship);
        for (WeaponModel w : ship.weapons) { w.charge = 0; w.targetRoomId = -1; }
    }

    private void setPowerIfPresent(ShipModel ship, Enums.RoomType t, int p) {
        RoomModel r = ship.firstRoom(t);
        if (r != null && r.maxPower > 0) r.power = Math.min(r.maxPower, Math.min(p, Math.max(0, ship.reactorFree() + r.power)));
    }

    // ---- small helpers --------------------------------------------------
    private interface Op { ActionResult run() throws Exception; }

    private ActionResult guard(Op op) {
        try {
            return op.run();
        } catch (Exception e) {
            return ActionResult.fail("error: " + e.getMessage());
        }
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    private static WeaponModel weapon(ShipModel ship, int slot) {
        if (ship == null) return null;
        for (WeaponModel w : ship.weapons) if (w.slot == slot) return w;
        return null;
    }

    private TechModel tech(Enums.TechType type) {
        for (TechModel t : state.techTree) if (t.type == type) return t;
        return null;
    }

    private int techLevel(Enums.TechType type) {
        TechModel t = tech(type);
        return t == null ? 0 : t.level;
    }

    private static String describeEvent(Enums.EventType ev) {
        if (ev == null) return "clear";
        switch (ev) {
            case NOTHING: return "nothing of note";
            case DERELICT: return "a drifting derelict";
            case DISTRESS: return "a distress beacon";
            case MERCHANT: return "a merchant convoy";
            case HAZARD: return "a spatial hazard";
            case PIRATE_AMBUSH: return "pirate signatures!";
            case STORY: return "an ancient signal";
            default: return ev.name();
        }
    }

    private long newSeed() {
        return System.nanoTime() ^ (listeners.hashCode() * 0x9E3779B97F4A7C15L);
    }

    private static long seedOf(Random r) {
        // derive a stable seed snapshot for saving determinism
        return r.nextLong();
    }

    private String randomName() {
        String[] names = {"Vance", "Rell", "Okada", "Sol", "Kade", "Nera", "Dax", "Reyes", "Iko", "Marn"};
        return names[rng.nextInt(names.length)];
    }
}

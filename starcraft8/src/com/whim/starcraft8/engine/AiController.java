package com.whim.starcraft8.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.starcraft8.data.TechTree;
import com.whim.starcraft8.domain.AttackKind;
import com.whim.starcraft8.domain.BuildState;
import com.whim.starcraft8.domain.Building;
import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.GameState;
import com.whim.starcraft8.domain.Player;
import com.whim.starcraft8.domain.Race;
import com.whim.starcraft8.domain.Unit;
import com.whim.starcraft8.domain.UnitType;

/**
 * One state-machine AI per non-human player. Each tick window it: keeps workers mining,
 * builds supply before it caps, advances tech off {@link TechTree} (first production →
 * gas → second production), trains a mixed army, and sends attack-move waves at the
 * enemy town hall. Works for all three races without race-specific unit hardcoding.
 */
final class AiController {

    private static final int WORKER_CAP = 18;
    private static final int ATTACK_SUPPLY = 14;
    private static final int STRUCTURE_THROTTLE = 90; // ticks between structures

    private final SimulationImpl sim;
    private final int playerId;
    private long lastStructureTick = -1000;

    AiController(SimulationImpl sim, int playerId) {
        this.sim = sim;
        this.playerId = playerId;
    }

    int phase() {
        int p = SimulationImpl.AI_THINK_PERIOD;
        return ((playerId % p) + p) % p;
    }

    void think(long tick) {
        GameState gs = sim.gameState();
        Player p = gs.player(playerId);
        if (p == null || p.defeated()) return;
        Race r = p.race();

        List<Unit> myU = myUnits();
        List<Building> myB = myBuildings();
        Building hall = townHall(myB);
        if (hall == null) return; // nothing to drive production with this tick

        keepWorkersMining(myU);
        manageSupply(tick, p, r, hall, myB, myU);
        trainWorkers(p, r, hall, myB, myU);
        advanceTech(tick, p, r, hall, myB, myU);
        trainArmy(tick, p, r, myB);
        attackWaves(myU);
    }

    // -------- workers --------

    private void keepWorkersMining(List<Unit> myU) {
        for (Unit u : myU) {
            if (!u.type().isWorker()) continue;
            SimulationImpl.Order o = sim.order(u.id());
            if (o.kind == SimulationImpl.Order.NONE) sim.assignGather(u, -1);
        }
        staffGas(myU);
    }

    /** Put ~3 workers on each completed gas structure so gas-tier units can be trained. */
    private void staffGas(List<Unit> myU) {
        for (Building b : myBuildings()) {
            if (!b.type().isGas() || !b.alive() || b.state() == BuildState.UNDER_CONSTRUCTION) continue;
            int on = 0;
            for (Unit u : myU) {
                if (!u.type().isWorker()) continue;
                SimulationImpl.Order o = sim.order(u.id());
                if (o.kind == SimulationImpl.Order.GATHER && o.resX == b.tileX() && o.resY == b.tileY()) on++;
            }
            for (Unit u : myU) {
                if (on >= 3) break;
                if (!u.type().isWorker() || u.carriedResource() != 0) continue;
                SimulationImpl.Order o = sim.order(u.id());
                boolean onGas = o.kind == SimulationImpl.Order.GATHER && o.resX == b.tileX() && o.resY == b.tileY();
                if (o.kind == SimulationImpl.Order.GATHER && !onGas) { // pull a mineral worker
                    sim.assignGather(u, b.id());
                    on++;
                }
            }
        }
    }

    private void trainWorkers(Player p, Race r, Building hall, List<Building> myB, List<Unit> myU) {
        int workers = 0;
        for (Unit u : myU) if (u.type().isWorker()) workers++;
        int queued = queuedOf(myB, TechTree.worker(r));
        if (workers + queued >= WORKER_CAP) return;
        if (hall.productionQueue().size() < 2) sim.doTrain(hall, TechTree.worker(r));
    }

    // -------- supply --------

    private void manageSupply(long tick, Player p, Race r, Building hall,
                              List<Building> myB, List<Unit> myU) {
        int cap = p.supplyCap(), used = p.supplyUsed();
        if (cap >= SimulationImpl.SUPPLY_MAX || (cap - used) > 4) return;

        UnitType su = TechTree.supplyUnit(r);        // Overlord for Zerg, else null
        BuildingType sb = TechTree.supplyBuilding(r); // Depot/Pylon, null for Zerg
        if (su != null) {
            // Zerg: an Overlord (provides supply) trained from the hatchery.
            if (overlordsPending(myB, su) == 0 && p.minerals() >= su.mineralCost()) {
                sim.doTrain(hall, su);
            }
        } else if (sb != null) {
            // build successive depots/pylons as we approach the cap; only one at a time
            if (!underConstruction(myB, sb) && p.minerals() >= sb.mineralCost()
                    && tick - lastStructureTick >= STRUCTURE_THROTTLE) {
                if (placeStructure(p, sb, hall, myU)) lastStructureTick = tick;
            }
        }
    }

    private boolean underConstruction(List<Building> myB, BuildingType type) {
        for (Building b : myB) {
            if (b.type() == type && b.alive() && b.state() == BuildState.UNDER_CONSTRUCTION) return true;
        }
        return false;
    }

    private int overlordsPending(List<Building> myB, UnitType su) {
        int n = 0;
        for (Building b : myB) for (UnitType t : b.productionQueue()) if (t == su) n++;
        return n;
    }

    // -------- tech --------

    private void advanceTech(long tick, Player p, Race r, Building hall,
                             List<Building> myB, List<Unit> myU) {
        if (tick - lastStructureTick < STRUCTURE_THROTTLE) return;

        List<BuildingType> prod = productionBuildings(r);
        boolean haveProd = false;
        for (BuildingType b : prod) if (hasOrBuilding(myB, b)) { haveProd = true; break; }

        BuildingType want = null;
        if (!haveProd) {
            want = cheapestAffordable(prod, p, myB);
        } else {
            BuildingType gas = firstGas(r);
            if (gas != null && !hasOrBuilding(myB, gas) && p.gas() < 50
                    && p.minerals() >= gas.mineralCost()) {
                want = gas;
            } else if (countProduction(myB, prod) < 2 && p.minerals() >= 250) {
                want = cheapestAffordable(prod, p, myB); // a second/different production building
            }
        }
        if (want != null && placeStructure(p, want, hall, myU)) lastStructureTick = tick;
    }

    // -------- army --------

    private void trainArmy(long tick, Player p, Race r, List<Building> myB) {
        for (Building b : myB) {
            if (!b.alive() || b.state() == BuildState.UNDER_CONSTRUCTION) continue;
            if (b.type().isTownHall()) continue;
            List<UnitType> menu = combatUnits(b.type());
            if (menu.isEmpty() || b.productionQueue().size() >= 2) continue;
            // rotate choice for a mixed army, deterministically (no Math.random in engine)
            int start = (int) ((tick / 7 + b.id()) % menu.size());
            for (int k = 0; k < menu.size(); k++) {
                UnitType t = menu.get((start + k) % menu.size());
                if (p.minerals() >= t.mineralCost() && p.gas() >= t.gasCost()) {
                    if (sim.doTrain(b, t)) break;
                }
            }
        }
    }

    private void attackWaves(List<Unit> myU) {
        int armySupply = 0;
        List<Long> army = new ArrayList<Long>();
        for (Unit u : myU) {
            if (u.type().isWorker() || u.type().attackKind() == AttackKind.NONE) continue;
            if (u.type().supplyProvided() > 0) continue; // overlords aren't fighters
            armySupply += u.type().supplyCost();
            army.add(u.id());
        }
        if (armySupply < ATTACK_SUPPLY || army.isEmpty()) return;

        double[] tgt = enemyTarget();
        if (tgt == null) return;
        // attack-move the whole army at the enemy; units already in combat keep their
        // in-sight target, so re-issuing only redirects stragglers (no fight interrupt).
        sim.cmdMove(army, tgt[0], tgt[1], true);
    }

    private double[] enemyTarget() {
        GameState gs = sim.gameState();
        Building hall = townHall(myBuildings());
        double hx = hall == null ? gs.map().width() / 2.0 : hall.tileX();
        double hy = hall == null ? gs.map().height() / 2.0 : hall.tileY();
        Building bestHall = null, bestAny = null;
        double bd = Double.MAX_VALUE, ba = Double.MAX_VALUE;
        for (Building b : gs.buildings()) {
            if (!b.alive() || b.ownerId() == playerId) continue;
            Player op = gs.player(b.ownerId());
            if (op != null && op.defeated()) continue;
            double dx = b.tileX() - hx, dy = b.tileY() - hy, d = dx * dx + dy * dy;
            if (b.type().isTownHall() && d < bd) { bd = d; bestHall = b; }
            if (d < ba) { ba = d; bestAny = b; }
        }
        Building t = bestHall != null ? bestHall : bestAny;
        if (t != null) {
            return new double[]{t.tileX() + t.type().widthTiles() / 2.0,
                    t.tileY() + t.type().heightTiles() / 2.0};
        }
        // no enemy buildings: chase nearest enemy unit
        Unit best = null; double bu = Double.MAX_VALUE;
        for (Unit u : gs.units()) {
            if (!u.alive() || u.ownerId() == playerId) continue;
            double dx = u.x() - hx, dy = u.y() - hy, d = dx * dx + dy * dy;
            if (d < bu) { bu = d; best = u; }
        }
        return best == null ? null : new double[]{best.x(), best.y()};
    }

    // -------- placement helpers --------

    private boolean placeStructure(Player p, BuildingType type, Building hall, List<Unit> myU) {
        Unit worker = freeWorker(myU, p.race());
        if (worker == null) return false;
        int[] tile = findBuildTile(type, hall);
        if (tile == null) return false;
        return sim.doBuild(playerId, worker, type, tile[0], tile[1]);
    }

    private Unit freeWorker(List<Unit> myU, Race r) {
        int workers = 0;
        Unit pick = null;
        for (Unit u : myU) {
            if (!u.type().isWorker()) continue;
            workers++;
            SimulationImpl.Order o = sim.order(u.id());
            if (pick == null && u.carriedResource() == 0
                    && (o.kind == SimulationImpl.Order.GATHER || o.kind == SimulationImpl.Order.NONE)) {
                pick = u;
            }
        }
        // don't cannibalise the whole economy (Zerg consumes the drone)
        if (workers <= 3) return null;
        return pick;
    }

    private int[] findBuildTile(BuildingType type, Building hall) {
        int hx = hall.tileX(), hy = hall.tileY();
        for (int rad = 2; rad <= 16; rad++) {
            for (int dy = -rad; dy <= rad; dy++) {
                for (int dx = -rad; dx <= rad; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != rad) continue; // ring only
                    int tx = hx + dx, ty = hy + dy;
                    if (sim.canPlace(type, tx, ty)) return new int[]{tx, ty};
                }
            }
        }
        return null;
    }

    // -------- tech-tree views --------

    private List<BuildingType> productionBuildings(Race r) {
        List<BuildingType> out = new ArrayList<BuildingType>();
        for (BuildingType b : TechTree.buildableBy(r)) {
            if (b.isTownHall() || b.isSupply() || b.isGas()) continue;
            if (!combatUnits(b).isEmpty()) out.add(b);
        }
        return out;
    }

    private BuildingType firstGas(Race r) {
        for (BuildingType b : TechTree.buildableBy(r)) if (b.isGas()) return b;
        return null;
    }

    private List<UnitType> combatUnits(BuildingType b) {
        List<UnitType> out = new ArrayList<UnitType>();
        for (UnitType t : TechTree.producedBy(b)) {
            if (!t.isWorker() && t.attackKind() != AttackKind.NONE && t.supplyProvided() == 0) out.add(t);
        }
        return out;
    }

    private BuildingType cheapestAffordable(List<BuildingType> list, Player p, List<Building> myB) {
        BuildingType best = null; int bc = Integer.MAX_VALUE;
        for (BuildingType b : list) {
            if (hasOrBuilding(myB, b)) continue;
            if (!sim.prereqsMet(playerId, b)) continue;
            if (p.minerals() < b.mineralCost() || p.gas() < b.gasCost()) continue;
            if (b.mineralCost() < bc) { bc = b.mineralCost(); best = b; }
        }
        return best;
    }

    private int countProduction(List<Building> myB, List<BuildingType> prod) {
        int n = 0;
        for (Building b : myB) if (prod.contains(b.type())) n++;
        return n;
    }

    // -------- small state queries --------

    private boolean hasOrBuilding(List<Building> myB, BuildingType type) {
        for (Building b : myB) if (b.type() == type && b.alive()) return true;
        return false;
    }

    private int queuedOf(List<Building> myB, UnitType type) {
        int n = 0;
        for (Building b : myB) for (UnitType t : b.productionQueue()) if (t == type) n++;
        return n;
    }

    private Building townHall(List<Building> myB) {
        for (Building b : myB) {
            if (b.type().isTownHall() && b.alive() && b.state() != BuildState.UNDER_CONSTRUCTION) return b;
        }
        for (Building b : myB) if (b.type().isTownHall() && b.alive()) return b;
        return null;
    }

    private List<Unit> myUnits() {
        List<Unit> out = new ArrayList<Unit>();
        for (Unit u : sim.gameState().units()) if (u.ownerId() == playerId && u.alive()) out.add(u);
        return out;
    }

    private List<Building> myBuildings() {
        List<Building> out = new ArrayList<Building>();
        for (Building b : sim.gameState().buildings()) if (b.ownerId() == playerId && b.alive()) out.add(b);
        return out;
    }
}

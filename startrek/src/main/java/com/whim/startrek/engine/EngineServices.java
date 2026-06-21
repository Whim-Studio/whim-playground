package com.whim.startrek.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.whim.startrek.domain.Empire;
import com.whim.startrek.domain.FacilityType;
import com.whim.startrek.domain.Fleet;
import com.whim.startrek.domain.GalaxyMap;
import com.whim.startrek.domain.GameServices;
import com.whim.startrek.domain.GameState;
import com.whim.startrek.domain.GridCell;
import com.whim.startrek.domain.MapObjectType;
import com.whim.startrek.domain.Race;
import com.whim.startrek.domain.ResourceType;
import com.whim.startrek.domain.Ship;
import com.whim.startrek.domain.StarSystem;
import com.whim.startrek.domain.TechType;

/**
 * Concrete {@link GameServices} implementation wiring the engine subsystems into the domain's turn
 * loop. {@code TurnManager} calls these in phase order (INCOME, RESEARCH, MOVEMENT, COMBAT, BORG).
 */
public class EngineServices implements GameServices {

    /** Turn at which the dormant Borg threat first awakens. */
    private static final int BORG_AWAKEN_TURN = 8;

    private final EconomyEngine economy;
    private final FleetAI fleetAI;
    private final BorgEngine borg;
    private final Random rng;

    public EngineServices() {
        this(new EconomyEngine(), new FleetAI(), new BorgEngine(), new Random(20240101L));
    }

    public EngineServices(EconomyEngine economy, FleetAI fleetAI, BorgEngine borg, Random rng) {
        this.economy = economy;
        this.fleetAI = fleetAI;
        this.borg = borg;
        this.rng = rng;
    }

    public EconomyEngine getEconomy() {
        return economy;
    }

    public FleetAI getFleetAI() {
        return fleetAI;
    }

    public BorgEngine getBorgEngine() {
        return borg;
    }

    // ------------------------------------------------------------------ INCOME

    @Override
    public void applyIncome(GameState s) {
        if (s == null) {
            return;
        }
        for (Empire e : s.getEmpires()) {
            for (StarSystem sys : e.getSystems()) {
                if (sys.isBorgControlled()) {
                    continue; // assimilated systems produce nothing for their old owner
                }
                for (ResourceType r : ResourceType.values()) {
                    long prod = sys.getProduction(r);
                    if (prod != 0L) {
                        e.addTreasury(r, prod);
                    }
                }
                // Modest organic population growth.
                long pop = sys.getPopulation();
                if (pop > 0L) {
                    sys.setPopulation(pop + Math.max(1L, pop / 50L));
                }
            }
        }
    }

    // ---------------------------------------------------------------- RESEARCH

    @Override
    public void applyResearch(GameState s) {
        if (s == null) {
            return;
        }
        for (Empire e : s.getEmpires()) {
            int points = 0;
            for (StarSystem sys : e.getSystems()) {
                points += Math.max(0, sys.getFacility(FacilityType.RESEARCH_FACILITY));
            }
            if (points <= 0) {
                continue;
            }
            // Invest in the tree that is currently furthest below its racial cap.
            for (int i = 0; i < points; i++) {
                TechType target = mostBehindTree(e);
                if (target == null) {
                    break; // everything maxed out
                }
                e.setTechLevel(target, e.getTechLevel(target) + 1);
            }
        }
    }

    private TechType mostBehindTree(Empire e) {
        TechType best = null;
        int bestGap = 0;
        for (TechType t : TechType.values()) {
            int cap = e.getRace().getCap(t);
            int gap = cap - e.getTechLevel(t);
            if (gap > bestGap) {
                bestGap = gap;
                best = t;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- MOVEMENT

    @Override
    public void resolveMovement(GameState s) {
        if (s == null) {
            return;
        }
        for (Empire e : s.getEmpires()) {
            fleetAI.stepCloaking(e, s);
        }

        GalaxyMap map = s.getMap();
        // Snapshot fleets first: moving them mutates cell fleet lists.
        List<Fleet> fleets = new ArrayList<Fleet>(map.allFleets());
        for (Fleet f : fleets) {
            if (f.isEmpty()) {
                continue;
            }
            int dr = f.getDestRow();
            int dc = f.getDestCol();
            if (dr < 0 || dc < 0 || (dr == f.getRow() && dc == f.getCol())) {
                continue; // no destination set, or already there
            }
            int[] next = fleetAI.nextStepToward(f, dr, dc, s);
            moveFleet(map, f, next[0], next[1]);
            if (!f.isEmpty()) {
                applyHazards(map, f);
            }
        }
    }

    private void moveFleet(GalaxyMap map, Fleet f, int nr, int nc) {
        if (nr == f.getRow() && nc == f.getCol()) {
            return;
        }
        GridCell from = map.getCell(f.getRow(), f.getCol());
        GridCell to = map.getCell(nr, nc);
        if (to == null) {
            return;
        }
        if (from != null) {
            from.getFleets().remove(f);
        }
        if (!to.getFleets().contains(f)) {
            to.getFleets().add(f);
        }
        f.setCell(nr, nc);
    }

    private void applyHazards(GalaxyMap map, Fleet f) {
        GridCell cell = map.getCell(f.getRow(), f.getCol());
        if (cell == null) {
            return;
        }
        MapObjectType type = cell.getType();

        if (type.destroysAssets()) {
            destroyFleet(map, f);
            return;
        }

        int minPct = type.getHullDamageMinPct();
        int maxPct = type.getHullDamageMaxPct();
        if (maxPct > 0) {
            int pct = minPct + (maxPct > minPct ? rng.nextInt(maxPct - minPct + 1) : 0);
            for (Ship ship : f.getShips()) {
                int dmg = (int) Math.round(ship.getMaxHull() * (pct / 100.0));
                ship.setHull(Math.max(0, ship.getHull() - dmg));
            }
            removeDestroyedShips(f);
        }

        // Wormhole teleport (after any unstable-wormhole damage above).
        if ((type == MapObjectType.STABLE_WORMHOLE || type == MapObjectType.UNSTABLE_WORMHOLE)
                && cell.getWormholeLinkRow() >= 0 && cell.getWormholeLinkCol() >= 0 && !f.isEmpty()) {
            moveFleet(map, f, cell.getWormholeLinkRow(), cell.getWormholeLinkCol());
        }
    }

    private void destroyFleet(GalaxyMap map, Fleet f) {
        for (Ship ship : f.getShips()) {
            ship.setHull(0);
        }
        f.getShips().clear();
        GridCell cell = map.getCell(f.getRow(), f.getCol());
        if (cell != null) {
            cell.getFleets().remove(f);
        }
    }

    private void removeDestroyedShips(Fleet f) {
        Iterator<Ship> it = f.getShips().iterator();
        while (it.hasNext()) {
            if (it.next().isDestroyed()) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ COMBAT

    @Override
    public void resolveCombat(GameState s) {
        if (s == null || s.isBattleActive()) {
            return; // a live (UI-driven) battle is in progress; don't auto-resolve over it
        }
        Map<Race, Empire> byRace = empiresByRace(s);
        GalaxyMap map = s.getMap();
        for (int r = 0; r < map.getRows(); r++) {
            for (int c = 0; c < map.getCols(); c++) {
                GridCell cell = map.getCell(r, c);
                if (cell == null || cell.getFleets().size() < 2) {
                    continue;
                }
                autoResolveCell(cell, byRace);
            }
        }
    }

    private void autoResolveCell(GridCell cell, Map<Race, Empire> byRace) {
        // Group fleets by owner; need at least two distinct owners to fight.
        Map<Race, List<Fleet>> byOwner = new HashMap<Race, List<Fleet>>();
        for (Fleet f : new ArrayList<Fleet>(cell.getFleets())) {
            if (f.isEmpty()) {
                continue;
            }
            List<Fleet> list = byOwner.get(f.getOwner());
            if (list == null) {
                list = new ArrayList<Fleet>();
                byOwner.put(f.getOwner(), list);
            }
            list.add(f);
        }
        if (byOwner.size() < 2) {
            return;
        }

        Race winner = null;
        long bestStrength = -1L;
        for (Map.Entry<Race, List<Fleet>> entry : byOwner.entrySet()) {
            long str = strength(entry.getValue());
            if (str > bestStrength) {
                bestStrength = str;
                winner = entry.getKey();
            }
        }

        // Losers are wiped; the victor takes attrition scaled to the toughest opponent it faced.
        long enemyStrength = 0L;
        for (Map.Entry<Race, List<Fleet>> entry : byOwner.entrySet()) {
            if (entry.getKey() == winner) {
                continue;
            }
            enemyStrength = Math.max(enemyStrength, strength(entry.getValue()));
            for (Fleet loser : entry.getValue()) {
                wipeFleet(cell, loser, byRace.get(entry.getKey()));
            }
        }
        applyAttrition(byOwner.get(winner), enemyStrength, bestStrength);
        cleanupEmptyFleets(cell, byRace);
    }

    private void applyAttrition(List<Fleet> winners, long enemyStrength, long winnerStrength) {
        if (winners == null || winnerStrength <= 0) {
            return;
        }
        double ratio = Math.min(0.9, (double) enemyStrength / (double) winnerStrength);
        for (Fleet f : winners) {
            for (Ship ship : f.getShips()) {
                int dmg = (int) Math.round((ship.getShields() + ship.getHull()) * ratio * 0.5);
                int shields = ship.getShields();
                if (dmg <= shields) {
                    ship.setShields(shields - dmg);
                } else {
                    ship.setShields(0);
                    ship.setHull(Math.max(1, ship.getHull() - (dmg - shields))); // victor survives
                }
            }
        }
    }

    private long strength(List<Fleet> fleets) {
        long total = 0L;
        for (Fleet f : fleets) {
            for (Ship ship : f.getShips()) {
                total += ship.getHull() + ship.getShields() + (long) ship.getWeaponDamage();
            }
        }
        return total;
    }

    private void wipeFleet(GridCell cell, Fleet loser, Empire owner) {
        for (Ship ship : loser.getShips()) {
            ship.setHull(0);
        }
        loser.getShips().clear();
        cell.getFleets().remove(loser);
        if (owner != null) {
            owner.getFleets().remove(loser);
        }
    }

    private void cleanupEmptyFleets(GridCell cell, Map<Race, Empire> byRace) {
        Iterator<Fleet> it = cell.getFleets().iterator();
        while (it.hasNext()) {
            Fleet f = it.next();
            if (f.isEmpty()) {
                it.remove();
                Empire owner = byRace.get(f.getOwner());
                if (owner != null) {
                    owner.getFleets().remove(f);
                }
            }
        }
    }

    private Map<Race, Empire> empiresByRace(GameState s) {
        Map<Race, Empire> map = new HashMap<Race, Empire>();
        for (Empire e : s.getEmpires()) {
            map.put(e.getRace(), e);
        }
        return map;
    }

    // -------------------------------------------------------------------- BORG

    @Override
    public void stepBorg(GameState s) {
        if (s == null) {
            return;
        }
        // Awaken the dormant collective once the galaxy has had time to settle.
        if (!borg.isEradicated(s) && !s.getBorgState().isActive()
                && s.getTurnNumber() >= BORG_AWAKEN_TURN) {
            s.getBorgState().setActive(true);
        }
        borg.step(s);
    }
}

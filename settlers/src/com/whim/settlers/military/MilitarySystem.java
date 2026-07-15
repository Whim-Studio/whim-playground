package com.whim.settlers.military;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingManager;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.economy.Economy;
import com.whim.settlers.economy.Good;
import com.whim.settlers.map.TileMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knights, garrisons, morale, territory, and the attack/defend flow.
 *
 * <p>Territory is the union of radii around every finished fort (military
 * building or Castle) that has at least one knight garrisoned; each tile is owned
 * by the nearest claiming fort's player. Building a garrisoned fort near the
 * border is therefore the only way to expand — exactly as the design states.
 * Capturing an enemy fort flips its owner and recomputes territory, which can
 * orphan (neutralise) the loser's buildings left outside their remaining borders.
 */
public final class MilitarySystem {

    private final TileMap map;
    private final BuildingManager buildings;
    private final Economy economy;

    private final Map<Building, Garrison> garrisons = new HashMap<Building, Garrison>();
    private final List<Attack> attacks = new ArrayList<Attack>();

    /** Per-tile owner (player id) or -1 for neutral. */
    private int[] territory;

    // Player-tunable knobs (human).
    private int knightTarget = 6;
    private int defaultRank = 1;

    private float territoryTimer, knightTimer, moraleTimer;

    public MilitarySystem(TileMap map, BuildingManager buildings, Economy economy) {
        this.map = map;
        this.buildings = buildings;
        this.economy = economy;
        this.territory = new int[map.width() * map.height()];
        java.util.Arrays.fill(territory, -1);
    }

    // ------------------------------------------------------------- fort config

    public static boolean isFort(BuildingType t) {
        return t.isMilitary() || t == BuildingType.CASTLE;
    }

    /** Territory radius (Chebyshev tiles) claimed by a garrisoned fort. */
    public static int radius(BuildingType t) {
        switch (t) {
            case CASTLE:      return 8;
            case GARRISON:    return 7;
            case GUARD_TOWER: return 6;
            case GUARD_HUT:   return 4;
            default:          return 0;
        }
    }

    /** Maximum knights a fort can garrison. */
    public static int capacity(BuildingType t) {
        switch (t) {
            case CASTLE:      return 8;
            case GARRISON:    return 6;
            case GUARD_TOWER: return 4;
            case GUARD_HUT:   return 2;
            default:          return 0;
        }
    }

    // ------------------------------------------------------------------ update

    public void update(float dt) {
        produceKnights(dt);
        trainKnights(dt);
        raiseMorale(dt);
        updateAttacks(dt);
        territoryTimer += dt;
        if (territoryTimer >= 0.5f) { territoryTimer = 0f; recomputeTerritory(); }
    }

    /** Convert settlers (+ a sword and shield) into knights for the human player. */
    private void produceKnights(float dt) {
        knightTimer += dt;
        if (knightTimer < 1.5f) return;
        knightTimer = 0f;
        if (knightCount(Players.HUMAN) >= knightTarget) return;
        Building dest = null;
        for (Building b : buildings.all()) {
            if (b.ownerId() == Players.HUMAN && b.isFinished() && isFort(b.type())
                    && garrisonOf(b).knights.size() < capacity(b.type())) {
                dest = b; break;
            }
        }
        if (dest == null) return;
        if (economy.stock().has(Good.SWORD, 1) && economy.stock().has(Good.SHIELD, 1)
                && economy.takeSettler()) {
            economy.stock().take(Good.SWORD, 1);
            economy.stock().take(Good.SHIELD, 1);
            garrisonOf(dest).knights.add(new Knight(defaultRank));
        }
    }

    private void trainKnights(float dt) {
        for (Map.Entry<Building, Garrison> e : garrisons.entrySet()) {
            boolean inCastle = e.getKey().type() == BuildingType.CASTLE;
            for (Knight k : e.getValue().knights) k.train(dt, inCastle);
        }
    }

    /** Deliver gold coins to raise a human fort's morale (combat multiplier). */
    private void raiseMorale(float dt) {
        moraleTimer += dt;
        if (moraleTimer < 2f) return;
        moraleTimer = 0f;
        for (Building b : buildings.all()) {
            if (b.ownerId() != Players.HUMAN || !isFort(b.type()) || !b.isFinished()) continue;
            Garrison g = garrisonOf(b);
            if (g.morale < 2.0f && economy.stock().take(Good.GOLD, 1)) {
                g.morale = Math.min(2.0f, g.morale + 0.15f);
            }
        }
    }

    // --------------------------------------------------------------- territory

    private void recomputeTerritory() {
        int w = map.width(), h = map.height();
        int[] owner = new int[w * h];
        int[] best = new int[w * h];
        java.util.Arrays.fill(owner, -1);
        java.util.Arrays.fill(best, Integer.MAX_VALUE);
        for (Building b : buildings.all()) {
            if (!b.isFinished() || !isFort(b.type())) continue;
            if (garrisonOf(b).knights.isEmpty()) continue; // must be garrisoned
            int r = radius(b.type());
            int cx = b.x() + b.type().footprintW() / 2;
            int cy = b.y() + b.type().footprintH() / 2;
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int tx = cx + dx, ty = cy + dy;
                    if (!map.inBounds(tx, ty)) continue;
                    int d = Math.max(Math.abs(dx), Math.abs(dy));
                    int idx = ty * w + tx;
                    if (d < best[idx]) { best[idx] = d; owner[idx] = b.ownerId(); }
                }
            }
        }
        this.territory = owner;
    }

    public boolean ownsTile(int player, int x, int y) {
        return map.inBounds(x, y) && territory[y * map.width() + x] == player;
    }

    public int ownerAt(int x, int y) {
        return map.inBounds(x, y) ? territory[y * map.width() + x] : -1;
    }

    /** Whole footprint must be inside the player's territory (non-military build). */
    public boolean withinTerritory(int player, BuildingType type, int ax, int ay) {
        for (int dy = 0; dy < type.footprintH(); dy++)
            for (int dx = 0; dx < type.footprintW(); dx++)
                if (!ownsTile(player, ax + dx, ay + dy)) return false;
        return true;
    }

    /** A military building may sit on or one tile beyond the border, to expand it. */
    public boolean adjacentToTerritory(int player, BuildingType type, int ax, int ay) {
        for (int dy = -1; dy <= type.footprintH(); dy++) {
            for (int dx = -1; dx <= type.footprintW(); dx++) {
                if (ownsTile(player, ax + dx, ay + dy)) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ combat

    /** Send up to {@code count} knights from the human's forts against a target. */
    public boolean launchAttack(Building target, int count) {
        if (target == null || !isFort(target.type()) || target.ownerId() == Players.HUMAN) return false;
        List<Building> srcs = new ArrayList<Building>();
        for (Building b : buildings.all()) {
            if (b.ownerId() == Players.HUMAN && isFort(b.type()) && !garrisonOf(b).knights.isEmpty()) {
                srcs.add(b);
            }
        }
        final int tcx = target.x(), tcy = target.y();
        Collections.sort(srcs, new Comparator<Building>() {
            @Override public int compare(Building a, Building b) {
                return dist(a, tcx, tcy) - dist(b, tcx, tcy);
            }
        });
        List<Knight> force = new ArrayList<Knight>();
        for (Building src : srcs) {
            List<Knight> g = garrisonOf(src).knights;
            while (!g.isEmpty() && force.size() < count) force.add(g.remove(g.size() - 1));
            if (force.size() >= count) break;
        }
        if (force.isEmpty()) return false;
        attacks.add(new Attack(target, Players.HUMAN, force, 4.0f)); // // approximate march time
        return true;
    }

    private void updateAttacks(float dt) {
        for (int i = attacks.size() - 1; i >= 0; i--) {
            Attack a = attacks.get(i);
            a.timer -= dt;
            if (a.timer <= 0f) { resolve(a); attacks.remove(i); }
        }
    }

    private void resolve(Attack a) {
        Building target = a.target;
        if (target.ownerId() == a.attacker) { // already ours — reinforce
            garrisonOf(target).knights.addAll(a.force);
            return;
        }
        Garrison def = garrisonOf(target);
        float atkStr = strengthOf(a.force, 1.0f);
        float defStr = strengthOf(def.knights, def.morale);
        if (atkStr >= defStr) {
            int loser = target.ownerId();
            def.knights.clear();
            def.morale = 1.0f;
            target.setOwner(a.attacker);
            int cap = capacity(target.type());
            int survivors = Math.max(1, Math.round(a.force.size()
                    * (defStr <= 0 ? 1f : (atkStr - defStr) / atkStr)));
            for (int i = 0; i < a.force.size() && def.knights.size() < Math.min(cap, survivors); i++) {
                def.knights.add(a.force.get(i));
            }
            recomputeTerritory();
            neutraliseOrphans(loser);
        } else {
            // Defender holds; attackers are lost, defenders take casualties.
            int killed = Math.min(def.knights.size() - 1, a.force.size());
            for (int i = 0; i < killed && def.knights.size() > 1; i++) {
                def.knights.remove(def.knights.size() - 1);
            }
        }
    }

    /** Buildings of the loser left outside their remaining territory go neutral. */
    private void neutraliseOrphans(int loser) {
        for (Building b : buildings.all()) {
            if (b.ownerId() != loser || isFort(b.type())) continue;
            if (!ownsTile(loser, b.x(), b.y())) {
                b.setOwner(-1); // inert / neutral
            }
        }
    }

    private static float strengthOf(List<Knight> ks, float morale) {
        int s = 0;
        for (Knight k : ks) s += k.strength();
        return s * morale;
    }

    private static int dist(Building b, int x, int y) {
        return Math.abs(b.x() - x) + Math.abs(b.y() - y);
    }

    // --------------------------------------------------------------- accessors

    public Garrison garrisonOf(Building b) {
        Garrison g = garrisons.get(b);
        if (g == null) { g = new Garrison(); garrisons.put(b, g); }
        return g;
    }

    /** Seed a garrison directly (used to set up the enemy at game start). */
    public void seedGarrison(Building b, int count, int rank) {
        Garrison g = garrisonOf(b);
        for (int i = 0; i < count; i++) g.knights.add(new Knight(rank));
        recomputeTerritory();
    }

    public int knightCount(int player) {
        int n = 0;
        for (Map.Entry<Building, Garrison> e : garrisons.entrySet()) {
            if (e.getKey().ownerId() == player) n += e.getValue().knights.size();
        }
        return n;
    }

    public int garrisonSize(Building b) { return garrisonOf(b).knights.size(); }
    public float morale(Building b)     { return garrisonOf(b).morale; }
    public int knightTarget()           { return knightTarget; }
    public void bumpKnightTarget(int d) { knightTarget = Math.max(0, Math.min(30, knightTarget + d)); }
    public int defaultRank()            { return defaultRank; }
    public void bumpDefaultRank(int d)  { defaultRank = Math.max(1, Math.min(Knight.MAX_RANK, defaultRank + d)); }
    public int activeAttacks()          { return attacks.size(); }

    /** Garrison of a fort: its knights and morale. */
    public static final class Garrison {
        final List<Knight> knights = new ArrayList<Knight>();
        float morale = 1.0f;
    }

    /** An in-flight assault. */
    private static final class Attack {
        final Building target;
        final int attacker;
        final List<Knight> force;
        float timer;
        Attack(Building target, int attacker, List<Knight> force, float timer) {
            this.target = target; this.attacker = attacker; this.force = force; this.timer = timer;
        }
    }
}

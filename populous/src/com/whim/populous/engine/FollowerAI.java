package com.whim.populous.engine;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.api.Enums.TerrainType;
import com.whim.populous.domain.Follower;
import com.whim.populous.domain.GameStateManager;
import com.whim.populous.domain.MapGrid;
import com.whim.populous.domain.PapalMagnet;
import com.whim.populous.domain.Settlement;

/**
 * Per-follower autonomous behaviour, evaluated every tick on the sim thread.
 * A walker seeks the largest nearby flat plateau of own/neutral land, walks to
 * it, founds or upgrades a settlement, breeds under the population cap, migrates
 * when overcrowded, converges on its side's active Papal Magnet, drowns on
 * water/swamp, and loses health on lava. Stamina drains while walking and
 * recovers while resting on a settlement.
 *
 * Engine-owned scratch state per walker is kept in an {@link IdentityHashMap}
 * here, so the domain {@link Follower} stays a clean data object with no AI
 * bookkeeping fields.
 */
final class FollowerAI {

    private static final double SPEED = 0.18;          // tiles per tick
    private static final double ARRIVE_DIST = 0.4;     // "close enough" radius
    private static final int SCAN_RADIUS = 6;          // plateau search radius
    private static final int BREED_INTERVAL = 90;      // ticks between births
    private static final int CROWD_LIMIT = 6;          // walkers near a site => migrate

    /** Engine-private per-walker memory. */
    private static final class AiState {
        boolean hasTarget = false;
        int targetCol = 0;
        int targetRow = 0;
        long lastBreedTick = 0;
        long lastRetarget = -9999;
    }

    private final Random rng;
    private final Map<Follower, AiState> memory = new IdentityHashMap<Follower, AiState>();

    FollowerAI(Random rng) {
        this.rng = rng;
    }

    /** Drop scratch for walkers that have died, to avoid unbounded growth. */
    void forget(Follower f) {
        memory.remove(f);
    }

    private AiState stateFor(Follower f) {
        AiState s = memory.get(f);
        if (s == null) {
            s = new AiState();
            memory.put(f, s);
        }
        return s;
    }

    /**
     * Advance every walker one step. New walkers born this tick are appended to
     * the manager's follower list; we iterate by index up to the original size
     * so newborns are processed next tick, not this one.
     */
    void update(GameStateManager mgr) {
        MapGrid map = mgr.map();
        List<Follower> followers = mgr.followers();
        int originalSize = followers.size();
        long tick = mgr.getTick();

        for (int i = 0; i < originalSize; i++) {
            Follower f = followers.get(i);
            if (!f.alive()) {
                continue;
            }
            stepWalker(mgr, map, f, tick);
        }
    }

    private void stepWalker(GameStateManager mgr, MapGrid map, Follower f, long tick) {
        AiState st = stateFor(f);

        // 1. Papal Magnet overrides all other goals while active.
        PapalMagnet magnet = mgr.magnet(f.allegiance());
        boolean herding = magnet != null && magnet.active();
        if (herding) {
            st.hasTarget = true;
            st.targetCol = magnet.col();
            st.targetRow = magnet.row();
        }

        int col = (int) Math.floor(f.x());
        int row = (int) Math.floor(f.y());

        // 2. Pick a new goal if we have none / have arrived / are overdue.
        boolean arrived = st.hasTarget && dist(f.x(), f.y(), st.targetCol + 0.5, st.targetRow + 0.5) < ARRIVE_DIST;
        if (!herding && (!st.hasTarget || arrived || tick - st.lastRetarget > 240)) {
            chooseTarget(map, f, st, col, row, tick);
        }

        // 3. Move toward the target; drain stamina while walking.
        boolean moved = false;
        if (st.hasTarget) {
            moved = walkToward(f, st.targetCol + 0.5, st.targetRow + 0.5);
        }
        if (moved) {
            f.setStamina(Math.max(0, f.stamina() - 1));
        } else {
            f.setStamina(Math.min(100, f.stamina() + 2)); // resting recovers faster
        }

        // Recompute the tile we now stand on.
        col = clamp((int) Math.floor(f.x()), map.cols());
        row = clamp((int) Math.floor(f.y()), map.rows());

        // 4. If we reached a build goal, found/upgrade a settlement + maybe breed.
        if (!herding && st.hasTarget
                && dist(f.x(), f.y(), st.targetCol + 0.5, st.targetRow + 0.5) < ARRIVE_DIST) {
            tryBuildAndBreed(mgr, map, f, st, col, row, tick);
            st.hasTarget = false; // look for the next job next tick
        }

        // 5. Environmental hazards, evaluated on the tile we END the tick on.
        applyHazards(f, map, col, row);
    }

    /** Seek the largest flat, own/neutral plateau within scan radius. */
    private void chooseTarget(MapGrid map, Follower f, AiState st, int col, int row, long tick) {
        int bestCol = -1;
        int bestRow = -1;
        int bestFlat = 0;
        int roughCol = -1;
        int roughRow = -1;

        for (int dr = -SCAN_RADIUS; dr <= SCAN_RADIUS; dr++) {
            for (int dc = -SCAN_RADIUS; dc <= SCAN_RADIUS; dc++) {
                int cc = col + dc;
                int rr = row + dr;
                if (!map.inBounds(cc, rr)) {
                    continue;
                }
                if (map.elevationAt(cc, rr) < map.seaLevel()) {
                    continue; // never head into the sea
                }
                Allegiance owner = map.tileAt(cc, rr).owner();
                boolean mine = owner == Allegiance.NEUTRAL || owner == f.allegiance();
                if (!mine) {
                    continue;
                }
                int flat = map.flatAreaAt(cc, rr);
                if (flat > bestFlat) {
                    bestFlat = flat;
                    bestCol = cc;
                    bestRow = rr;
                }
                if (roughCol < 0 && flat <= 1) {
                    roughCol = cc;
                    roughRow = rr;
                }
            }
        }

        if (bestFlat >= 1 && bestCol >= 0) {
            st.hasTarget = true;
            st.targetCol = bestCol;
            st.targetRow = bestRow;
        } else if (roughCol >= 0) {
            // No plateau: wander to rough land (helps future flattening).
            st.hasTarget = true;
            st.targetCol = roughCol;
            st.targetRow = roughRow;
        } else {
            // Fully boxed in: pick a random nearby land tile.
            st.hasTarget = true;
            st.targetCol = clamp(col + rng.nextInt(5) - 2, map.cols());
            st.targetRow = clamp(row + rng.nextInt(5) - 2, map.rows());
        }
        st.lastRetarget = tick;
    }

    private void tryBuildAndBreed(GameStateManager mgr, MapGrid map, Follower f,
                                  AiState st, int col, int row, long tick) {
        if (map.elevationAt(col, row) < map.seaLevel()) {
            return; // can't build in water
        }
        Allegiance owner = map.tileAt(col, row).owner();
        if (owner != Allegiance.NEUTRAL && owner != f.allegiance()) {
            return; // enemy ground
        }

        int flat = map.flatAreaAt(col, row);
        if (flat < 1) {
            return;
        }
        SettlementType tier = typeForFlat(flat);

        // Overcrowding check -> migrate instead of piling up.
        int nearby = countNearbyAllies(mgr, f, col, row);
        if (nearby > CROWD_LIMIT) {
            st.hasTarget = false;
            return;
        }

        Settlement existing = settlementAt(mgr, col, row);
        if (existing == null) {
            Settlement s = new Settlement(f.allegiance(), col, row, tier);
            mgr.settlements().add(s);
            map.setOwner(col, row, f.allegiance());
            map.setSettlement(col, row, tier, tierLevel(tier));
        } else if (existing.owner() == f.allegiance()
                && tier.ordinal() > existing.type().ordinal()) {
            existing.setType(tier);
            map.setSettlement(col, row, tier, tierLevel(tier));
        }

        // Rest here: recover.
        f.setStamina(Math.min(100, f.stamina() + 5));
        f.setHealth(Math.min(100, f.health() + 1));

        // Breed under the population cap, spaced out in time.
        boolean underCap = mgr.population(f.allegiance()) < mgr.populationCap();
        if (underCap && tick - st.lastBreedTick > BREED_INTERVAL) {
            double nx = col + 0.5 + (rng.nextDouble() - 0.5);
            double ny = row + 0.5 + (rng.nextDouble() - 0.5);
            Follower child = new Follower(f.allegiance(), nx, ny);
            mgr.followers().add(child);
            st.lastBreedTick = tick;
        }
    }

    private void applyHazards(Follower f, MapGrid map, int col, int row) {
        TerrainType terrain = map.tileAt(col, row).terrain();
        if (terrain == TerrainType.WATER || terrain == TerrainType.SHALLOW
                || terrain == TerrainType.SWAMP) {
            f.setAlive(false); // drowned / swallowed
            f.setHealth(0);
            return;
        }
        if (terrain == TerrainType.LAVA) {
            f.setHealth(f.health() - 15);
            if (f.health() <= 0) {
                f.setAlive(false);
            }
        }
        // Slow attrition when exhausted and far from home.
        if (f.stamina() <= 0) {
            f.setHealth(f.health() - 1);
            if (f.health() <= 0) {
                f.setAlive(false);
            }
        }
    }

    // ------------------------------------------------------------------
    // Small helpers.
    // ------------------------------------------------------------------

    private boolean walkToward(Follower f, double tx, double ty) {
        double dx = tx - f.x();
        double dy = ty - f.y();
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < 1e-6) {
            return false;
        }
        double step = Math.min(SPEED, d);
        f.setX(f.x() + dx / d * step);
        f.setY(f.y() + dy / d * step);
        return true;
    }

    private int countNearbyAllies(GameStateManager mgr, Follower self, int col, int row) {
        List<Follower> all = mgr.followers();
        int count = 0;
        for (int i = 0; i < all.size(); i++) {
            Follower o = all.get(i);
            if (o == self || !o.alive() || o.allegiance() != self.allegiance()) {
                continue;
            }
            if (Math.abs(o.x() - (col + 0.5)) <= 1.5 && Math.abs(o.y() - (row + 0.5)) <= 1.5) {
                count++;
            }
        }
        return count;
    }

    private Settlement settlementAt(GameStateManager mgr, int col, int row) {
        List<Settlement> settlements = mgr.settlements();
        for (int i = 0; i < settlements.size(); i++) {
            Settlement s = settlements.get(i);
            if (s.col() == col && s.row() == row) {
                return s;
            }
        }
        return null;
    }

    static SettlementType typeForFlat(int flat) {
        if (flat >= 16) return SettlementType.CASTLE;
        if (flat >= 9)  return SettlementType.TOWER;
        if (flat >= 4)  return SettlementType.HOUSE;
        if (flat >= 2)  return SettlementType.HUT;
        return SettlementType.TENT;
    }

    private static int tierLevel(SettlementType t) {
        return t.ordinal(); // 1..5 small index within the tier for the renderer
    }

    private static double dist(double ax, double ay, double bx, double by) {
        double dx = ax - bx;
        double dy = ay - by;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static int clamp(int v, int max) {
        if (v < 0) return 0;
        if (v >= max) return max - 1;
        return v;
    }
}

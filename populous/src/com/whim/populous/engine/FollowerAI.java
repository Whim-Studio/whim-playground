package com.whim.populous.engine;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.api.Enums.TerrainType;
import com.whim.populous.domain.Follower;
import com.whim.populous.domain.GameState;
import com.whim.populous.domain.MapGrid;
import com.whim.populous.domain.PapalMagnet;
import com.whim.populous.domain.Settlement;
import com.whim.populous.domain.SettlementRules;
import com.whim.populous.domain.Tile;

/**
 * Per-follower autonomous behaviour, evaluated every tick on the sim thread.
 * A walker seeks the largest nearby flat plateau of own/neutral land, walks to
 * it, founds or upgrades a settlement (tiered by {@link SettlementRules}),
 * breeds under the population cap, migrates when overcrowded, converges on its
 * side's active Papal Magnet, drowns on water/swamp, and loses health on lava.
 * Stamina drains while walking and recovers while resting on a settlement.
 *
 * Engine-owned scratch state per walker lives in an {@link IdentityHashMap}
 * here, keeping the domain {@link Follower} a clean data object.
 */
final class FollowerAI {

    private static final double SPEED = 0.18;
    private static final double ARRIVE_DIST = 0.4;
    private static final int SCAN_RADIUS = 6;
    private static final int BREED_INTERVAL = 90;
    private static final int CROWD_LIMIT = 6;

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
     * Advance every walker one step. Newborns are appended to the live list; we
     * only iterate up to the original size so they are processed next tick.
     */
    void update(GameState gs) {
        MapGrid map = gs.grid();
        List<Follower> followers = gs.followerList();
        int originalSize = followers.size();
        long tick = gs.tick();

        for (int i = 0; i < originalSize; i++) {
            Follower f = followers.get(i);
            if (!f.alive()) {
                continue;
            }
            stepWalker(gs, map, f, tick);
        }
    }

    private void stepWalker(GameState gs, MapGrid map, Follower f, long tick) {
        AiState st = stateFor(f);

        // 1. Papal Magnet overrides other goals while active.
        PapalMagnet magnet = gs.magnetFor(f.allegiance());
        boolean herding = magnet != null && magnet.active();
        if (herding) {
            st.hasTarget = true;
            st.targetCol = magnet.col();
            st.targetRow = magnet.row();
        }

        int col = f.tileCol();
        int row = f.tileRow();

        // 2. Pick a new goal if we have none / have arrived / are overdue.
        boolean arrived = st.hasTarget
                && dist(f.x(), f.y(), st.targetCol + 0.5, st.targetRow + 0.5) < ARRIVE_DIST;
        if (!herding && (!st.hasTarget || arrived || tick - st.lastRetarget > 240)) {
            chooseTarget(map, f, st, col, row, tick);
        }

        // 3. Move toward the target; drain/recover stamina.
        boolean moved = false;
        if (st.hasTarget) {
            moved = walkToward(f, st.targetCol + 0.5, st.targetRow + 0.5);
        }
        if (moved) {
            f.drainStamina(1);
        } else {
            f.recoverStamina(2);
        }

        col = clamp(f.tileCol(), map.cols());
        row = clamp(f.tileRow(), map.rows());

        // 4. If we reached a build goal, found/upgrade a settlement + maybe breed.
        if (!herding && st.hasTarget
                && dist(f.x(), f.y(), st.targetCol + 0.5, st.targetRow + 0.5) < ARRIVE_DIST) {
            tryBuildAndBreed(gs, map, f, st, col, row, tick);
            st.hasTarget = false;
        }

        // 5. Environmental hazards on the tile we END the tick on.
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
                if (EngineSupport.elevationAt(map, cc, rr) < map.seaLevel()) {
                    continue;
                }
                Allegiance owner = EngineSupport.ownerAt(map, cc, rr);
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
            st.hasTarget = true;
            st.targetCol = roughCol;
            st.targetRow = roughRow;
        } else {
            st.hasTarget = true;
            st.targetCol = clamp(col + rng.nextInt(5) - 2, map.cols());
            st.targetRow = clamp(row + rng.nextInt(5) - 2, map.rows());
        }
        st.lastRetarget = tick;
    }

    private void tryBuildAndBreed(GameState gs, MapGrid map, Follower f,
                                  AiState st, int col, int row, long tick) {
        Tile home = map.tile(col, row);
        if (home == null || home.elevation() < map.seaLevel()) {
            return;
        }
        Allegiance owner = home.owner();
        if (owner != Allegiance.NEUTRAL && owner != f.allegiance()) {
            return;
        }

        int flat = map.flatAreaAt(col, row);
        if (flat < 1) {
            return;
        }
        SettlementType tier = SettlementRules.tierFor(flat);

        int nearby = countNearbyAllies(gs, f, col, row);
        if (nearby > CROWD_LIMIT) {
            st.hasTarget = false; // overcrowded -> migrate elsewhere
            return;
        }

        Settlement existing = home.settlementRef();
        if (existing == null) {
            Settlement s = new Settlement(home, f.allegiance(), tier,
                    SettlementRules.levelWithinTier(flat));
            home.setSettlement(s); // also claims territory ownership
        } else if (existing.owner() == f.allegiance()
                && tier.ordinal() > existing.type().ordinal()) {
            existing.retier(flat); // upgrade tier + level from the plateau size
        }

        // Rest here: recover.
        f.recoverStamina(5);
        f.heal(1);

        // Breed under the population cap, spaced out in time.
        boolean underCap = EngineSupport.livePopulation(gs, f.allegiance()) < gs.populationCap();
        if (underCap && tick - st.lastBreedTick > BREED_INTERVAL) {
            double nx = col + 0.5 + (rng.nextDouble() - 0.5);
            double ny = row + 0.5 + (rng.nextDouble() - 0.5);
            gs.followerList().add(new Follower(f.allegiance(), nx, ny));
            st.lastBreedTick = tick;
        }
    }

    private void applyHazards(Follower f, MapGrid map, int col, int row) {
        TerrainType terrain = EngineSupport.terrainAt(map, col, row);
        if (terrain == TerrainType.WATER || terrain == TerrainType.SHALLOW
                || terrain == TerrainType.SWAMP) {
            f.kill();
            return;
        }
        if (terrain == TerrainType.LAVA) {
            f.damage(15);
        }
        if (f.stamina() <= 0) {
            f.damage(1);
        }
    }

    // ------------------------------------------------------------------
    // Helpers.
    // ------------------------------------------------------------------

    private boolean walkToward(Follower f, double tx, double ty) {
        double dx = tx - f.x();
        double dy = ty - f.y();
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < 1e-6) {
            return false;
        }
        double step = Math.min(SPEED, d);
        f.setPosition(f.x() + dx / d * step, f.y() + dy / d * step);
        return true;
    }

    private int countNearbyAllies(GameState gs, Follower self, int col, int row) {
        List<Follower> all = gs.followerList();
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

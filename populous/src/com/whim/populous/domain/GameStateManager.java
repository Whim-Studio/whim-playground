package com.whim.populous.domain;

import java.util.List;
import java.util.Random;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.SettlementType;

/**
 * Factory and orchestration surface for the whole game state. This is the object
 * the engine (Task 2) OWNS: it builds a procedural island with a starting colony
 * for each side, and exposes the derived-quantity helpers (population recount,
 * mana accrual) the engine calls each tick. All AI, the sim loop and rendering
 * live OUTSIDE this class — here we keep pure state + deterministic rules.
 *
 * <h3>Populous mana model</h3>
 * Each side's mana climbs each tick by the sum of its settlements' mana weights
 * (see {@link SettlementRules}) — more and bigger dwellings mean more worshippers
 * and therefore more belief/manna. Mana is clamped to {@link #DEFAULT_MAX_MANA},
 * which is sized above the costliest power (ARMAGEDDON, 5000) so a large empire
 * can eventually afford anything. Population (for the victory check and the HUD)
 * is the live count of a side's walkers.
 */
public final class GameStateManager {

    public static final int MAP_COLS = 64;
    public static final int MAP_ROWS = 64;
    public static final int SEA_LEVEL = 0;

    /** Full mana-bar reference; above ARMAGEDDON's 5000 cost. */
    public static final int DEFAULT_MAX_MANA = 6000;
    /** Per-side soft population cap. */
    public static final int DEFAULT_POP_CAP = 250;

    /** Starting walkers per side. */
    public static final int START_FOLLOWERS = 6;

    private GameState state;
    private long seed;

    public GameStateManager() { }

    /** Build a brand-new game on a procedural island for the given seed. */
    public GameState newGame(long seed) {
        this.seed = seed;
        MapGrid map = new MapGrid(MAP_COLS, MAP_ROWS, SEA_LEVEL);
        Random rnd = new Random(seed);
        generateIsland(map, rnd);

        GameState gs = new GameState(map, DEFAULT_MAX_MANA, DEFAULT_POP_CAP);

        // Two opposing homelands on the mid-latitude, east vs west.
        int gy = MAP_ROWS / 2;
        int gx = MAP_COLS / 4;          // GOOD in the west
        int ex = (MAP_COLS * 3) / 4;    // EVIL in the east

        foundHomeland(gs, map, Allegiance.GOOD, gx, gy);
        foundHomeland(gs, map, Allegiance.EVIL, ex, gy);

        recomputePopulations(gs);
        this.state = gs;
        return gs;
    }

    public GameState state() { return state; }
    public long seed() { return seed; }

    // ---- procedural island --------------------------------------------------

    private void generateIsland(MapGrid map, Random rnd) {
        int cols = map.cols();
        int rows = map.rows();
        double cx = cols / 2.0;
        double cy = rows / 2.0;
        double maxR = Math.min(cols, rows) / 2.0;

        double[] h = new double[cols * rows];

        // Radial base: a broad dome that falls below sea level near the edges,
        // guaranteeing the landmass is a surrounded island.
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double dx = c - cx;
                double dy = r - cy;
                double d = Math.sqrt(dx * dx + dy * dy) / maxR;
                h[r * cols + c] = (0.82 - d) * 8.0;
            }
        }

        // Scatter random hills and hollows to break the symmetry into bays,
        // ridges and inland plains.
        int features = 28;
        for (int k = 0; k < features; k++) {
            int fc = rnd.nextInt(cols);
            int fr = rnd.nextInt(rows);
            boolean pit = rnd.nextInt(100) < 30;
            double amp = (pit ? -1 : 1) * (2.0 + rnd.nextDouble() * 4.0);
            double sigma = 3.0 + rnd.nextDouble() * 6.0;
            double twoSigmaSq = 2.0 * sigma * sigma;
            int reach = (int) Math.ceil(sigma * 2.5);
            for (int dr = -reach; dr <= reach; dr++) {
                for (int dc = -reach; dc <= reach; dc++) {
                    int c = fc + dc;
                    int r = fr + dr;
                    if (c < 0 || c >= cols || r < 0 || r >= rows) {
                        continue;
                    }
                    double distSq = dc * dc + dr * dr;
                    h[r * cols + c] += amp * Math.exp(-distSq / twoSigmaSq);
                }
            }
        }

        // Commit to integer elevations, biased so there is plenty of low grass.
        for (int i = 0; i < h.length; i++) {
            int e = (int) Math.round(h[i]);
            if (e > 9) {
                e = 9;    // cap peaks
            }
            if (e < -4) {
                e = -4;   // cap trenches
            }
            map.tile(i % cols, i / cols).setElevation(e);
        }
    }

    /**
     * Flatten a buildable plateau, seed one starter settlement and spawn the
     * side's opening walkers around it.
     */
    private void foundHomeland(GameState gs, MapGrid map, Allegiance side, int cx, int cy) {
        int plateau = 1; // grass elevation
        // Flatten a 5x5 grass plateau, ringed one step lower so the buildable
        // flat area is well-defined for the follower AI.
        for (int dr = -3; dr <= 3; dr++) {
            for (int dc = -3; dc <= 3; dc++) {
                int c = cx + dc;
                int r = cy + dr;
                if (!map.inBounds(c, r)) {
                    continue;
                }
                boolean edge = Math.abs(dr) == 3 || Math.abs(dc) == 3;
                map.tile(c, r).setElevation(edge ? plateau - 1 : plateau);
            }
        }

        // Starter dwelling: a HOUSE on the plateau centre.
        Tile home = map.tile(cx, cy);
        Settlement s = new Settlement(home, side, SettlementType.HOUSE,
                SettlementRules.levelWithinTier(4));
        home.setSettlement(s);

        // Claim the plateau tiles as this side's territory.
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                Tile t = map.tile(cx + dc, cy + dr);
                if (t != null) {
                    t.setOwner(side);
                }
            }
        }

        // Opening walkers clustered around home.
        List<Follower> list = gs.followerList();
        double[][] offs = new double[][] {
            { 0.5, 0.5 }, { -1.2, 0.3 }, { 1.3, -0.4 },
            { 0.2, 1.4 }, { -0.9, -1.1 }, { 1.1, 1.2 }
        };
        for (int i = 0; i < START_FOLLOWERS && i < offs.length; i++) {
            list.add(new Follower(side, cx + offs[i][0], cy + offs[i][1]));
        }
    }

    // ---- per-tick derived quantities (engine calls these) -------------------

    /** Recount living walkers per side into the state's population fields. */
    public void recomputePopulations(GameState gs) {
        int good = 0;
        int evil = 0;
        List<Follower> list = gs.followerList();
        for (int i = 0; i < list.size(); i++) {
            Follower f = list.get(i);
            if (!f.alive()) {
                continue;
            }
            if (f.allegiance() == Allegiance.GOOD) {
                good++;
            } else if (f.allegiance() == Allegiance.EVIL) {
                evil++;
            }
        }
        gs.setGoodPopulation(good);
        gs.setEvilPopulation(evil);
    }

    /**
     * Total mana weight a side earns per tick: the sum of its settlements' mana
     * weights. Scans every tile's settlement once (authoritative over the map).
     */
    public int manaWeightFor(GameState gs, Allegiance side) {
        MapGrid map = gs.grid();
        int sum = 0;
        for (int r = 0; r < map.rows(); r++) {
            for (int c = 0; c < map.cols(); c++) {
                Tile t = map.tile(c, r);
                Settlement s = t.settlementRef();
                if (s != null && s.owner() == side) {
                    sum += s.manaWeight();
                }
            }
        }
        return sum;
    }

    /** Accrue one tick of mana for both sides from their settlement weights. */
    public void accrueMana(GameState gs) {
        gs.addGoodMana(manaWeightFor(gs, Allegiance.GOOD));
        gs.addEvilMana(manaWeightFor(gs, Allegiance.EVIL));
    }
}

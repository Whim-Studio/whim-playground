package com.whim.populous.engine;

import java.util.List;
import java.util.Random;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.domain.Follower;
import com.whim.populous.domain.GameStateManager;
import com.whim.populous.domain.MapGrid;
import com.whim.populous.domain.PapalMagnet;

/**
 * The opposing (EVIL) deity, driven by a four-state machine:
 *
 *   EXPAND -> flatten land around its followers so they can settle, and herd
 *             them with its Papal Magnet.
 *   GROW   -> stand back and let settlements mature while mana banks.
 *   ATTACK -> once mana clears a threshold, hurl a disaster at the player's
 *             densest settlement cluster.
 *   (then back to EXPAND)
 *
 * Runs on the sim thread; delegates all terraforming/disasters to
 * {@link DivinePowers} so mana accounting stays in one place.
 */
final class RivalAI {

    private enum Phase { EXPAND, GROW, ATTACK }

    private static final int EXPAND_TICKS = 240;
    private static final int GROW_TICKS = 300;
    private static final int ATTACK_MANA_THRESHOLD = 900;

    private final Random rng;
    private Phase phase = Phase.EXPAND;
    private int phaseTicks = 0;

    RivalAI(Random rng) {
        this.rng = rng;
    }

    void update(GameStateManager mgr, DivinePowers powers) {
        phaseTicks++;
        maintainMagnet(mgr);

        switch (phase) {
            case EXPAND:
                expand(mgr);
                if (phaseTicks > EXPAND_TICKS) {
                    transition(Phase.GROW);
                }
                break;
            case GROW:
                if (phaseTicks > GROW_TICKS
                        || mgr.getMana(Allegiance.EVIL) >= ATTACK_MANA_THRESHOLD) {
                    transition(Phase.ATTACK);
                }
                break;
            case ATTACK:
                attack(mgr, powers);
                transition(Phase.EXPAND);
                break;
            default:
                transition(Phase.EXPAND);
                break;
        }
    }

    private void transition(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    /** Flatten a few tiles around evil followers so plateaus can form. */
    private void expand(GameStateManager mgr) {
        MapGrid map = mgr.map();
        List<Follower> all = mgr.followers();
        int flattened = 0;
        for (int i = 0; i < all.size() && flattened < 4; i++) {
            Follower f = all.get(i);
            if (!f.alive() || f.allegiance() != Allegiance.EVIL) {
                continue;
            }
            int c = (int) Math.floor(f.x());
            int r = (int) Math.floor(f.y());
            // Raising toward a common level smooths the ground into a plateau.
            if (map.inBounds(c, r)) {
                if (map.elevationAt(c, r) <= map.seaLevel()) {
                    map.raise(c, r, 1);
                } else if (rng.nextBoolean()) {
                    map.raise(c, r, 2);
                } else {
                    map.lower(c, r, 1);
                }
                flattened++;
            }
        }
    }

    /** Cast the strongest affordable disaster at the player's densest cluster. */
    private void attack(GameStateManager mgr, DivinePowers powers) {
        int[] target = densestGoodCluster(mgr);
        if (target == null) {
            return;
        }
        int col = target[0];
        int row = target[1];
        int mana = mgr.getMana(Allegiance.EVIL);

        if (mana >= GodPower.VOLCANO.manaCost()) {
            powers.volcano(mgr, Allegiance.EVIL, col, row);
        } else if (mana >= GodPower.EARTHQUAKE.manaCost()) {
            powers.earthquake(mgr, Allegiance.EVIL, col, row);
        } else if (mana >= GodPower.SWAMP.manaCost()) {
            powers.swamp(mgr, Allegiance.EVIL, col, row);
        }
        // else: not enough for a disaster yet; keep banking.
    }

    /** Keep the evil magnet roughly at the centroid of evil followers. */
    private void maintainMagnet(GameStateManager mgr) {
        PapalMagnet pm = mgr.magnet(Allegiance.EVIL);
        if (pm == null) {
            return;
        }
        List<Follower> all = mgr.followers();
        double sx = 0;
        double sy = 0;
        int n = 0;
        for (int i = 0; i < all.size(); i++) {
            Follower f = all.get(i);
            if (f.alive() && f.allegiance() == Allegiance.EVIL) {
                sx += f.x();
                sy += f.y();
                n++;
            }
        }
        // Re-anchor occasionally so followers regroup without constant churn.
        if (n > 0 && phase == Phase.GROW && phaseTicks % 120 == 0) {
            pm.activate((int) (sx / n), (int) (sy / n));
        }
    }

    /** Grid-bucket the player's followers and return the densest 8x8 cell centre. */
    private int[] densestGoodCluster(GameStateManager mgr) {
        MapGrid map = mgr.map();
        int cols = map.cols();
        int rows = map.rows();
        int bucket = 8;
        int bc = (cols + bucket - 1) / bucket;
        int br = (rows + bucket - 1) / bucket;
        int[][] counts = new int[br][bc];

        List<Follower> all = mgr.followers();
        int best = 0;
        int bestBc = -1;
        int bestBr = -1;
        for (int i = 0; i < all.size(); i++) {
            Follower f = all.get(i);
            if (!f.alive() || f.allegiance() != Allegiance.GOOD) {
                continue;
            }
            int gc = clamp((int) (f.x() / bucket), bc);
            int gr = clamp((int) (f.y() / bucket), br);
            counts[gr][gc]++;
            if (counts[gr][gc] > best) {
                best = counts[gr][gc];
                bestBc = gc;
                bestBr = gr;
            }
        }
        if (bestBc < 0) {
            return null;
        }
        int[] out = new int[2];
        out[0] = clamp(bestBc * bucket + bucket / 2, cols);
        out[1] = clamp(bestBr * bucket + bucket / 2, rows);
        return out;
    }

    private static int clamp(int v, int max) {
        if (v < 0) return 0;
        if (v >= max) return max - 1;
        return v;
    }
}

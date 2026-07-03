package com.whim.populous.engine;

import java.util.List;
import java.util.Random;

import com.whim.populous.api.ActionResult;
import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.api.Enums.TerrainType;
import com.whim.populous.domain.Follower;
import com.whim.populous.domain.GameStateManager;
import com.whim.populous.domain.MapGrid;
import com.whim.populous.domain.PapalMagnet;
import com.whim.populous.domain.Settlement;

/**
 * The algorithms behind every {@link GodPower}. Each method verifies the caster
 * can afford {@link GodPower#manaCost()} and deducts it on success. Terraforming
 * (RAISE/LOWER) deducts only a tiny trickle. All mutation happens on the sim
 * thread under the engine lock (the engine holds the lock before delegating).
 *
 * Global powers (FLOOD, ARMAGEDDON) install transient state that
 * {@link #tickGlobals} advances every tick.
 */
final class DivinePowers {

    /** Mana trickle charged per raise/lower brush click. */
    private static final int TERRAFORM_TRICKLE = 1;

    /** How many elevation steps FLOOD lifts the sea, and for how long. */
    private static final int FLOOD_STEPS = 2;
    private static final int FLOOD_DURATION_TICKS = 300; // ~10s at 30 tps

    private final Random rng;

    // --- transient global-effect state, only touched on the sim thread ---
    private int floodTicksRemaining = 0;
    private int floodSavedSeaLevel = 0;
    private boolean floodActive = false;

    private int armageddonTicksRemaining = 0;
    private int battleCol = 0;
    private int battleRow = 0;

    DivinePowers(Random rng) {
        this.rng = rng;
    }

    boolean floodActive() { return floodActive; }
    boolean armageddonActive() { return armageddonTicksRemaining > 0; }

    // ------------------------------------------------------------------
    // Terraforming (RAISE_LAND / LOWER_LAND) — a small smoothing brush.
    // ------------------------------------------------------------------

    ActionResult raise(GameStateManager mgr, Allegiance caster, int col, int row) {
        MapGrid map = mgr.map();
        if (!map.inBounds(col, row)) {
            return ActionResult.fail("Out of bounds");
        }
        map.raise(col, row, 1);
        trickle(mgr, caster);
        return ActionResult.ok("Land raised");
    }

    ActionResult lower(GameStateManager mgr, Allegiance caster, int col, int row) {
        MapGrid map = mgr.map();
        if (!map.inBounds(col, row)) {
            return ActionResult.fail("Out of bounds");
        }
        map.lower(col, row, 1);
        trickle(mgr, caster);
        return ActionResult.ok("Land lowered");
    }

    private void trickle(GameStateManager mgr, Allegiance caster) {
        int cur = mgr.getMana(caster);
        mgr.setMana(caster, Math.max(0, cur - TERRAFORM_TRICKLE));
    }

    // ------------------------------------------------------------------
    // Targeted powers.
    // ------------------------------------------------------------------

    ActionResult papalMagnet(GameStateManager mgr, Allegiance caster, int col, int row) {
        if (!afford(mgr, caster, GodPower.PAPAL_MAGNET)) {
            return ActionResult.fail("Not enough mana for Papal Magnet");
        }
        MapGrid map = mgr.map();
        if (!map.inBounds(col, row)) {
            return ActionResult.fail("Out of bounds");
        }
        PapalMagnet pm = mgr.magnet(caster);
        pm.activate(col, row);
        deduct(mgr, caster, GodPower.PAPAL_MAGNET);
        return ActionResult.ok("Papal Magnet placed");
    }

    /** A jagged fault line that lowers a streak of tiles and topples settlements on it. */
    ActionResult earthquake(GameStateManager mgr, Allegiance caster, int col, int row) {
        if (!afford(mgr, caster, GodPower.EARTHQUAKE)) {
            return ActionResult.fail("Not enough mana for Earthquake");
        }
        MapGrid map = mgr.map();
        int c = col;
        int r = row;
        int len = 10 + rng.nextInt(8);
        for (int step = 0; step < len; step++) {
            if (map.inBounds(c, r)) {
                map.lower(c, r, 1);
                toppleSettlementAt(mgr, c, r);
            }
            // wander the fault in a jagged diagonal
            c += rng.nextInt(3) - 1;
            r += (rng.nextBoolean() ? 1 : 0) + (rng.nextInt(3) - 1);
        }
        deduct(mgr, caster, GodPower.EARTHQUAKE);
        return ActionResult.ok("Earthquake!");
    }

    /** Marks a blob of tiles as SWAMP — walkers that end a tick on them drown. */
    ActionResult swamp(GameStateManager mgr, Allegiance caster, int col, int row) {
        if (!afford(mgr, caster, GodPower.SWAMP)) {
            return ActionResult.fail("Not enough mana for Swamp");
        }
        MapGrid map = mgr.map();
        int radius = 2;
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                if (dc * dc + dr * dr > radius * radius) {
                    continue;
                }
                int cc = col + dc;
                int rr = row + dr;
                if (map.inBounds(cc, rr) && map.elevationAt(cc, rr) >= map.seaLevel()) {
                    map.setTerrainOverride(cc, rr, TerrainType.SWAMP);
                }
            }
        }
        deduct(mgr, caster, GodPower.SWAMP);
        return ActionResult.ok("Swamp spreads");
    }

    /** Raises a tall cone at the target and scatters ROCK/LAVA rings around it. */
    ActionResult volcano(GameStateManager mgr, Allegiance caster, int col, int row) {
        if (!afford(mgr, caster, GodPower.VOLCANO)) {
            return ActionResult.fail("Not enough mana for Volcano");
        }
        MapGrid map = mgr.map();
        int peak = 8;
        int radius = 5;
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                int cc = col + dc;
                int rr = row + dr;
                if (!map.inBounds(cc, rr)) {
                    continue;
                }
                double dist = Math.sqrt(dc * dc + dr * dr);
                if (dist > radius) {
                    continue;
                }
                int lift = (int) Math.round(peak * (1.0 - dist / (radius + 1.0)));
                if (lift > 0) {
                    map.setElevation(cc, rr, map.elevationAt(cc, rr) + lift);
                    toppleSettlementAt(mgr, cc, rr);
                }
                // Inner ring: molten lava. Outer ring: scattered rock.
                if (dist <= 1.5) {
                    map.setTerrainOverride(cc, rr, TerrainType.LAVA);
                } else if (dist >= radius - 1 && rng.nextInt(3) == 0) {
                    map.setTerrainOverride(cc, rr, TerrainType.ROCK);
                }
            }
        }
        deduct(mgr, caster, GodPower.VOLCANO);
        return ActionResult.ok("Volcano erupts!");
    }

    // ------------------------------------------------------------------
    // Global powers.
    // ------------------------------------------------------------------

    ActionResult flood(GameStateManager mgr, Allegiance caster) {
        if (!afford(mgr, caster, GodPower.FLOOD)) {
            return ActionResult.fail("Not enough mana for Flood");
        }
        MapGrid map = mgr.map();
        if (!floodActive) {
            floodSavedSeaLevel = map.seaLevel();
        }
        floodActive = true;
        floodTicksRemaining = FLOOD_DURATION_TICKS;
        map.setSeaLevel(floodSavedSeaLevel + FLOOD_STEPS);
        deduct(mgr, caster, GodPower.FLOOD);
        return ActionResult.ok("The waters rise!");
    }

    ActionResult armageddon(GameStateManager mgr, Allegiance caster) {
        if (!afford(mgr, caster, GodPower.ARMAGEDDON)) {
            return ActionResult.fail("Not enough mana for Armageddon");
        }
        MapGrid map = mgr.map();
        battleCol = map.cols() / 2;
        battleRow = map.rows() / 2;
        // Flatten a battlefield so both hosts can stand.
        for (int dr = -4; dr <= 4; dr++) {
            for (int dc = -4; dc <= 4; dc++) {
                int cc = battleCol + dc;
                int rr = battleRow + dr;
                if (map.inBounds(cc, rr)) {
                    map.setElevation(cc, rr, Math.max(map.seaLevel() + 1, map.elevationAt(cc, rr)));
                    map.setTerrainOverride(cc, rr, null);
                }
            }
        }
        // Summon every walker to the field.
        List<Follower> all = mgr.followers();
        for (int i = 0; i < all.size(); i++) {
            Follower f = all.get(i);
            if (f.alive()) {
                f.setX(battleCol + (rng.nextDouble() - 0.5) * 6.0);
                f.setY(battleRow + (rng.nextDouble() - 0.5) * 6.0);
            }
        }
        armageddonTicksRemaining = 600; // fight for up to ~20s
        deduct(mgr, caster, GodPower.ARMAGEDDON);
        return ActionResult.ok("ARMAGEDDON — the final battle begins!");
    }

    // ------------------------------------------------------------------
    // Per-tick advancement of transient global effects.
    // ------------------------------------------------------------------

    void tickGlobals(GameStateManager mgr) {
        MapGrid map = mgr.map();
        if (floodActive) {
            floodTicksRemaining--;
            if (floodTicksRemaining <= 0) {
                map.setSeaLevel(floodSavedSeaLevel);
                floodActive = false;
            }
        }
        if (armageddonTicksRemaining > 0) {
            armageddonTicksRemaining--;
            resolveBattle(mgr);
        }
    }

    /** During Armageddon, opposing walkers standing close trade blows. */
    private void resolveBattle(GameStateManager mgr) {
        List<Follower> all = mgr.followers();
        for (int i = 0; i < all.size(); i++) {
            Follower a = all.get(i);
            if (!a.alive()) {
                continue;
            }
            for (int j = i + 1; j < all.size(); j++) {
                Follower b = all.get(j);
                if (!b.alive() || b.allegiance() == a.allegiance()) {
                    continue;
                }
                double dx = a.x() - b.x();
                double dy = a.y() - b.y();
                if (dx * dx + dy * dy <= 1.5 * 1.5) {
                    a.setHealth(a.health() - 4);
                    b.setHealth(b.health() - 4);
                    if (a.health() <= 0) {
                        a.setAlive(false);
                    }
                    if (b.health() <= 0) {
                        b.setAlive(false);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers.
    // ------------------------------------------------------------------

    private void toppleSettlementAt(GameStateManager mgr, int col, int row) {
        List<Settlement> settlements = mgr.settlements();
        for (int i = settlements.size() - 1; i >= 0; i--) {
            Settlement s = settlements.get(i);
            if (s.col() == col && s.row() == row) {
                settlements.remove(i);
                mgr.map().setSettlement(col, row, SettlementType.NONE, 0);
                mgr.map().setOwner(col, row, Allegiance.NEUTRAL);
            }
        }
    }

    private boolean afford(GameStateManager mgr, Allegiance caster, GodPower power) {
        return mgr.getMana(caster) >= power.manaCost();
    }

    private void deduct(GameStateManager mgr, Allegiance caster, GodPower power) {
        mgr.setMana(caster, Math.max(0, mgr.getMana(caster) - power.manaCost()));
    }
}

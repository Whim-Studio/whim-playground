package com.whim.populous.engine;

import java.util.List;
import java.util.Random;

import com.whim.populous.api.ActionResult;
import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.Enums.TerrainType;
import com.whim.populous.domain.Follower;
import com.whim.populous.domain.GameState;
import com.whim.populous.domain.MapGrid;
import com.whim.populous.domain.PapalMagnet;
import com.whim.populous.domain.Tile;

/**
 * The algorithms behind every {@link GodPower}. Each verifies the caster can
 * afford {@link GodPower#manaCost()} and deducts it on success; terraforming
 * (RAISE/LOWER) deducts only a tiny trickle. All mutation runs on the sim
 * thread under the engine lock.
 *
 * SWAMP/LAVA/ROCK are applied as timed {@link Tile#setTransient} overrides and
 * aged by {@link MapGrid#ageTransients()} each tick. Global powers (FLOOD,
 * ARMAGEDDON) install transient state that {@link #tickGlobals} advances.
 */
final class DivinePowers {

    private static final int TERRAFORM_TRICKLE = 1;

    private static final int SWAMP_TTL = 450;   // ticks a swamp lingers
    private static final int LAVA_TTL = 300;    // ticks lava stays molten
    private static final int ROCK_TTL = 100000; // rocks are effectively permanent

    private static final int FLOOD_STEPS = 2;
    private static final int FLOOD_DURATION_TICKS = 300; // ~10s at 30 tps

    private final Random rng;

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

    ActionResult raise(GameState gs, Allegiance caster, int col, int row) {
        MapGrid map = gs.grid();
        if (!map.inBounds(col, row)) {
            return ActionResult.fail("Out of bounds");
        }
        map.raiseBrush(col, row, 1);
        trickle(gs, caster);
        return ActionResult.ok("Land raised");
    }

    ActionResult lower(GameState gs, Allegiance caster, int col, int row) {
        MapGrid map = gs.grid();
        if (!map.inBounds(col, row)) {
            return ActionResult.fail("Out of bounds");
        }
        map.lowerBrush(col, row, 1);
        trickle(gs, caster);
        return ActionResult.ok("Land lowered");
    }

    private void trickle(GameState gs, Allegiance caster) {
        gs.addMana(caster, -TERRAFORM_TRICKLE);
    }

    // ------------------------------------------------------------------
    // Targeted powers.
    // ------------------------------------------------------------------

    ActionResult papalMagnet(GameState gs, Allegiance caster, int col, int row) {
        if (!afford(gs, caster, GodPower.PAPAL_MAGNET)) {
            return ActionResult.fail("Not enough mana for Papal Magnet");
        }
        if (!gs.grid().inBounds(col, row)) {
            return ActionResult.fail("Out of bounds");
        }
        PapalMagnet pm = gs.magnetFor(caster);
        pm.placeAt(col, row);
        deduct(gs, caster, GodPower.PAPAL_MAGNET);
        return ActionResult.ok("Papal Magnet placed");
    }

    /** A jagged fault line that lowers a streak of tiles and topples settlements on it. */
    ActionResult earthquake(GameState gs, Allegiance caster, int col, int row) {
        if (!afford(gs, caster, GodPower.EARTHQUAKE)) {
            return ActionResult.fail("Not enough mana for Earthquake");
        }
        MapGrid map = gs.grid();
        int c = col;
        int r = row;
        int len = 10 + rng.nextInt(8);
        for (int step = 0; step < len; step++) {
            if (map.inBounds(c, r)) {
                map.lowerBrush(c, r, 1);
                toppleSettlementAt(map, c, r);
            }
            c += rng.nextInt(3) - 1;
            r += (rng.nextBoolean() ? 1 : 0) + (rng.nextInt(3) - 1);
        }
        deduct(gs, caster, GodPower.EARTHQUAKE);
        return ActionResult.ok("Earthquake!");
    }

    /** Marks a blob of tiles as SWAMP — walkers that end a tick on them drown. */
    ActionResult swamp(GameState gs, Allegiance caster, int col, int row) {
        if (!afford(gs, caster, GodPower.SWAMP)) {
            return ActionResult.fail("Not enough mana for Swamp");
        }
        MapGrid map = gs.grid();
        int radius = 2;
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                if (dc * dc + dr * dr > radius * radius) {
                    continue;
                }
                int cc = col + dc;
                int rr = row + dr;
                Tile t = map.tile(cc, rr);
                if (t != null && t.elevation() >= map.seaLevel()) {
                    t.setTransient(TerrainType.SWAMP, SWAMP_TTL);
                }
            }
        }
        deduct(gs, caster, GodPower.SWAMP);
        return ActionResult.ok("Swamp spreads");
    }

    /** Raises a tall cone at the target and scatters ROCK/LAVA rings around it. */
    ActionResult volcano(GameState gs, Allegiance caster, int col, int row) {
        if (!afford(gs, caster, GodPower.VOLCANO)) {
            return ActionResult.fail("Not enough mana for Volcano");
        }
        MapGrid map = gs.grid();
        int peak = 8;
        int radius = 5;
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                int cc = col + dc;
                int rr = row + dr;
                Tile t = map.tile(cc, rr);
                if (t == null) {
                    continue;
                }
                double dist = Math.sqrt(dc * dc + dr * dr);
                if (dist > radius) {
                    continue;
                }
                int lift = (int) Math.round(peak * (1.0 - dist / (radius + 1.0)));
                if (lift > 0) {
                    t.addElevation(lift);
                    toppleSettlementAt(map, cc, rr);
                }
                if (dist <= 1.5) {
                    t.setTransient(TerrainType.LAVA, LAVA_TTL);
                } else if (dist >= radius - 1 && rng.nextInt(3) == 0) {
                    t.setTransient(TerrainType.ROCK, ROCK_TTL);
                }
            }
        }
        deduct(gs, caster, GodPower.VOLCANO);
        return ActionResult.ok("Volcano erupts!");
    }

    // ------------------------------------------------------------------
    // Global powers.
    // ------------------------------------------------------------------

    ActionResult flood(GameState gs, Allegiance caster) {
        if (!afford(gs, caster, GodPower.FLOOD)) {
            return ActionResult.fail("Not enough mana for Flood");
        }
        MapGrid map = gs.grid();
        if (!floodActive) {
            floodSavedSeaLevel = map.seaLevel();
        }
        floodActive = true;
        floodTicksRemaining = FLOOD_DURATION_TICKS;
        map.setSeaLevel(floodSavedSeaLevel + FLOOD_STEPS);
        deduct(gs, caster, GodPower.FLOOD);
        return ActionResult.ok("The waters rise!");
    }

    ActionResult armageddon(GameState gs, Allegiance caster) {
        if (!afford(gs, caster, GodPower.ARMAGEDDON)) {
            return ActionResult.fail("Not enough mana for Armageddon");
        }
        MapGrid map = gs.grid();
        battleCol = map.cols() / 2;
        battleRow = map.rows() / 2;
        int floor = map.seaLevel() + 1;
        for (int dr = -4; dr <= 4; dr++) {
            for (int dc = -4; dc <= 4; dc++) {
                Tile t = map.tile(battleCol + dc, battleRow + dr);
                if (t != null) {
                    if (t.elevation() < floor) {
                        t.setElevation(floor);
                    }
                    t.setTransient(null, 0); // clear any swamp/lava on the field
                }
            }
        }
        List<Follower> all = gs.followerList();
        for (int i = 0; i < all.size(); i++) {
            Follower f = all.get(i);
            if (f.alive()) {
                f.setPosition(battleCol + (rng.nextDouble() - 0.5) * 6.0,
                        battleRow + (rng.nextDouble() - 0.5) * 6.0);
            }
        }
        armageddonTicksRemaining = 600;
        deduct(gs, caster, GodPower.ARMAGEDDON);
        return ActionResult.ok("ARMAGEDDON — the final battle begins!");
    }

    // ------------------------------------------------------------------
    // Per-tick advancement of transient global effects.
    // ------------------------------------------------------------------

    void tickGlobals(GameState gs) {
        MapGrid map = gs.grid();
        if (floodActive) {
            floodTicksRemaining--;
            if (floodTicksRemaining <= 0) {
                map.setSeaLevel(floodSavedSeaLevel);
                floodActive = false;
            }
        }
        if (armageddonTicksRemaining > 0) {
            armageddonTicksRemaining--;
            resolveBattle(gs);
        }
    }

    /** During Armageddon, opposing walkers standing close trade blows. */
    private void resolveBattle(GameState gs) {
        List<Follower> all = gs.followerList();
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
                    a.damage(4);
                    b.damage(4);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers.
    // ------------------------------------------------------------------

    private void toppleSettlementAt(MapGrid map, int col, int row) {
        Tile t = map.tile(col, row);
        if (t != null && t.settlementRef() != null) {
            t.clearSettlement();
            t.setOwner(Allegiance.NEUTRAL);
        }
    }

    private boolean afford(GameState gs, Allegiance caster, GodPower power) {
        return gs.manaFor(caster) >= power.manaCost();
    }

    private void deduct(GameState gs, Allegiance caster, GodPower power) {
        gs.addMana(caster, -power.manaCost());
    }
}

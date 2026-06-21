package com.midnight.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.midnight.core.Character;
import com.midnight.core.Direction;
import com.midnight.core.GameState;
import com.midnight.core.Location;
import com.midnight.core.Map;
import com.midnight.core.Side;
import com.midnight.core.Stronghold;

/**
 * Doomdark's night brain.
 *
 * <p>Each NIGHT every Doomdark army picks an objective and advances one tile
 * toward it. Objectives are weighted so the horde prefers, in order: the
 * Citadel of the Moon (Xajorkith, the wargame loss condition for the Free),
 * the player's recruited lords (hunting them down), and any other Free-held
 * stronghold. Where a Doomdark army ends the night sharing a tile with Free
 * forces or a Free stronghold, the {@link CombatResolver} is invoked.
 *
 * <p>This resolver is pure logic: it mutates the live {@link GameState} through
 * core setters only, never moves FREE lords (frozen at night), never advances
 * the day ({@code GameState.endDay} does that), and emits no Swing or console
 * output &mdash; all text goes into the returned {@link NightReport}.
 */
public class DoomdarkAI implements NightResolver {

    /** Pull weight for Xajorkith (smaller = stronger pull). */
    private static final double XAJORKITH_WEIGHT = 0.5;
    /** Pull weight for hunting a recruited Free lord. */
    private static final double LORD_WEIGHT = 1.0;
    /** Pull weight for any other Free-held stronghold. */
    private static final double STRONGHOLD_WEIGHT = 1.5;
    /** Default seed so the no-arg / single-arg AI is reproducible. */
    private static final long DEFAULT_SEED = 0xD00DA72L;

    private final CombatResolver resolver;
    private final Random rng;

    public DoomdarkAI() {
        this(new StandardCombatResolver());
    }

    public DoomdarkAI(CombatResolver resolver) {
        this(resolver, DEFAULT_SEED);
    }

    public DoomdarkAI(CombatResolver resolver, long seed) {
        this.resolver = (resolver != null) ? resolver : new StandardCombatResolver();
        this.rng = new Random(seed);
    }

    @Override
    public NightReport resolveNight(GameState state) {
        List<String> movements = new ArrayList<String>();
        List<BattleResult> battles = new ArrayList<BattleResult>();

        Map map = state.map();

        // Snapshot of objectives, taken before any movement this night.
        List<Character> playerLords = new ArrayList<Character>(state.playerLords());
        List<Stronghold> freeStrongholds = new ArrayList<Stronghold>();
        for (Stronghold sh : map.strongholds()) {
            if (sh.owner() == Side.FREE) {
                freeStrongholds.add(sh);
            }
        }

        // Advance each Doomdark army one tile toward its objective.
        for (Character d : aliveDoom(state)) {
            Location from = d.location();
            Location target = chooseTarget(from, playerLords, freeStrongholds);
            if (target == null || target.equals(from)) {
                continue;
            }
            Direction step = bestStep(map, from, target);
            if (step == null) {
                continue;
            }
            Location dest = from.neighbor(step);
            if (!map.isPassable(dest)) {
                continue;
            }
            d.setLocation(dest);
            d.setFacing(step);
            movements.add("The army of " + d.name() + " marches " + step
                    + " from " + from + " to " + dest + ".");
        }

        // Resolve a battle once at each location where a Doomdark army now meets
        // Free forces or a Free-held stronghold.
        Set<Location> resolved = new HashSet<Location>();
        for (Character d : aliveDoom(state)) {
            Location loc = d.location();
            if (resolved.contains(loc)) {
                continue;
            }
            if (!freePresent(state, loc)) {
                continue;
            }
            resolved.add(loc);
            BattleResult br = resolver.resolveBattle(state, loc);
            if (br != null) {
                battles.add(br);
            }
        }

        return new NightReport(battles, movements);
    }

    /** Alive DOOMDARK characters (fresh list each call so mid-loop moves are seen). */
    private static List<Character> aliveDoom(GameState state) {
        List<Character> out = new ArrayList<Character>();
        for (Character c : state.charactersOf(Side.DOOMDARK)) {
            if (c != null && c.isAlive()) {
                out.add(c);
            }
        }
        return out;
    }

    /** True if any alive FREE character or a FREE stronghold sits at {@code loc}. */
    private static boolean freePresent(GameState state, Location loc) {
        for (Character c : state.charactersAt(loc)) {
            if (c != null && c.isAlive() && c.side() == Side.FREE) {
                return true;
            }
        }
        Stronghold sh = state.map().strongholdAt(loc);
        return sh != null && sh.owner() == Side.FREE;
    }

    /** Pick the most attractive objective for an army at {@code from}. */
    private Location chooseTarget(Location from, List<Character> lords, List<Stronghold> strongholds) {
        Location best = null;
        double bestScore = Double.MAX_VALUE;

        for (Character lord : lords) {
            if (lord == null || !lord.isAlive()) {
                continue;
            }
            double score = from.chebyshevDistanceTo(lord.location()) * LORD_WEIGHT;
            if (score < bestScore) {
                bestScore = score;
                best = lord.location();
            }
        }

        for (Stronghold sh : strongholds) {
            double weight = sh.isXajorkith() ? XAJORKITH_WEIGHT : STRONGHOLD_WEIGHT;
            double score = from.chebyshevDistanceTo(sh.location()) * weight;
            if (score < bestScore) {
                bestScore = score;
                best = sh.location();
            }
        }

        return best;
    }

    /**
     * Choose the passable neighbour that moves closest to {@code target}.
     * Ties are broken with the seeded RNG so converging armies spread out
     * rather than stacking on a single track.
     */
    private Direction bestStep(Map map, Location from, Location target) {
        Direction best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Direction d : Direction.values()) {
            Location n = from.neighbor(d);
            if (!map.isPassable(n)) {
                continue;
            }
            int dist = n.chebyshevDistanceTo(target);
            if (dist < bestDist || (dist == bestDist && rng.nextBoolean())) {
                bestDist = dist;
                best = d;
            }
        }
        // Only advance if it actually closes the gap (or holds level toward a flank).
        if (best != null && bestDist <= from.chebyshevDistanceTo(target)) {
            return best;
        }
        return null;
    }
}

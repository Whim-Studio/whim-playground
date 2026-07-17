package com.whim.xcom.battle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import com.whim.xcom.model.DamageType;
import com.whim.xcom.model.FireMode;
import com.whim.xcom.rng.Rng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.model.DamageModel;
import com.whim.xcom.rules.model.ShotContext;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * The tactical engine: turn order, movement (with reaction fire), shooting (all
 * math delegated to the {@link Ruleset} strategies), a small alien AI, and the
 * win/lose check. Pure — no Swing. A {@link Listener} lets a view observe events
 * and repaint; headless callers can ignore it and drive the game directly.
 */
public final class BattleGame {

    /** Observer hook for the view (event log lines + "something changed"). */
    public interface Listener {
        void onEvent(String message);
        void onChanged();
        /** A blast occurred over these tiles (for a transient view effect). */
        default void onExplosion(java.util.List<int[]> tiles) { }
    }

    private static final int GRENADE_POWER = 50;
    private static final int GRENADE_RADIUS = 2;
    private static final int GRENADE_TU_PERCENT = 25;

    private static final int SIGHT_DAY = 20;
    private static final int SIGHT_NIGHT = 9;

    private final Ruleset ruleset;
    private final Rng rng;
    private final BattleMap map;
    private final List<BattleUnit> units = new ArrayList<BattleUnit>();
    private final boolean night;

    private final boolean[][] visible;
    private final boolean[][] discovered;

    private Side currentSide = Side.XCOM;
    private int turn = 1;
    private Side winner;
    private int aliensKilled;

    private Listener listener = new Listener() {
        @Override public void onEvent(String message) { }
        @Override public void onChanged() { }
    };

    public BattleGame(Ruleset ruleset, Rng rng, BattleMap map, boolean night) {
        this.ruleset = ruleset;
        this.rng = rng;
        this.map = map;
        this.night = night;
        this.visible = new boolean[map.width()][map.height()];
        this.discovered = new boolean[map.width()][map.height()];
    }

    public void setListener(Listener listener) {
        this.listener = listener == null ? this.listener : listener;
    }

    // ---- accessors ----------------------------------------------------------

    public BattleMap map() { return map; }
    public List<BattleUnit> units() { return units; }
    public Side currentSide() { return currentSide; }
    public int turn() { return turn; }
    public boolean night() { return night; }
    public int sightRange() { return night ? SIGHT_NIGHT : SIGHT_DAY; }
    public boolean visible(int x, int y) { return map.inBounds(x, y) && visible[x][y]; }
    public boolean discovered(int x, int y) { return map.inBounds(x, y) && discovered[x][y]; }

    void addUnit(BattleUnit u) {
        units.add(u);
        recomputeVisibility();
    }

    public BattleUnit unitAt(int x, int y) {
        for (BattleUnit u : units) {
            if (u.alive() && u.x() == x && u.y() == y) {
                return u;
            }
        }
        return null;
    }

    public List<BattleUnit> living(Side side) {
        List<BattleUnit> out = new ArrayList<BattleUnit>();
        for (BattleUnit u : units) {
            if (u.alive() && u.side() == side) {
                out.add(u);
            }
        }
        return out;
    }

    // ---- visibility / line of sight ----------------------------------------

    public boolean canSee(BattleUnit from, BattleUnit to) {
        if (from == null || to == null || !from.alive() || !to.alive()) {
            return false;
        }
        return BattleMap.distance(from.x(), from.y(), to.x(), to.y()) <= sightRange()
                && map.hasLineOfSight(from.x(), from.y(), to.x(), to.y());
    }

    /** Recompute which tiles X-COM currently sees (fog of war) and reveal them. */
    public void recomputeVisibility() {
        for (int x = 0; x < map.width(); x++) {
            for (int y = 0; y < map.height(); y++) {
                visible[x][y] = false;
            }
        }
        int range = sightRange();
        for (BattleUnit u : units) {
            if (!u.alive() || u.side() != Side.XCOM) {
                continue;
            }
            int minX = Math.max(0, u.x() - range);
            int maxX = Math.min(map.width() - 1, u.x() + range);
            int minY = Math.max(0, u.y() - range);
            int maxY = Math.min(map.height() - 1, u.y() + range);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (BattleMap.distance(u.x(), u.y(), x, y) <= range
                            && map.hasLineOfSight(u.x(), u.y(), x, y)) {
                        visible[x][y] = true;
                        discovered[x][y] = true;
                    }
                }
            }
        }
    }

    /** An enemy the player may click on: an alive alien on a currently-visible tile. */
    public boolean isEnemyVisible(BattleUnit alien) {
        return alien != null && alien.alive() && alien.alien() && visible(alien.x(), alien.y());
    }

    // ---- geometry -----------------------------------------------------------

    /** Octant 0=N,1=NE,2=E,3=SE,4=S,5=SW,6=W,7=NW for a delta. */
    public static int directionTo(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return 4;
        }
        double ang = Math.atan2(dx, -dy); // 0 = north, clockwise
        int oct = (int) Math.round(ang / (Math.PI / 4.0));
        return ((oct % 8) + 8) % 8;
    }

    private DamageModel.Facing facingHit(BattleUnit target, BattleUnit shooter) {
        int dirFromTarget = directionTo(shooter.x() - target.x(), shooter.y() - target.y());
        int diff = ((dirFromTarget - target.facing()) % 8 + 8) % 8;
        if (diff == 0 || diff == 1 || diff == 7) {
            return DamageModel.Facing.FRONT;
        }
        if (diff == 3 || diff == 4 || diff == 5) {
            return DamageModel.Facing.REAR;
        }
        return DamageModel.Facing.SIDE;
    }

    // ---- movement -----------------------------------------------------------

    /** Dijkstra path (excluding the start) to a walkable, unoccupied target, or empty. */
    public List<int[]> pathTo(BattleUnit u, int tx, int ty) {
        List<int[]> empty = new ArrayList<int[]>();
        if (!map.walkable(tx, ty) || unitAt(tx, ty) != null) {
            return empty;
        }
        int w = map.width();
        int h = map.height();
        int[][] dist = new int[w][h];
        int[][] prev = new int[w][h];
        for (int[] row : dist) {
            java.util.Arrays.fill(row, Integer.MAX_VALUE);
        }
        for (int[] row : prev) {
            java.util.Arrays.fill(row, -1);
        }
        final int[][] fd = dist;
        PriorityQueue<int[]> pq = new PriorityQueue<int[]>(new Comparator<int[]>() {
            @Override public int compare(int[] a, int[] b) {
                return Integer.compare(fd[a[0]][a[1]], fd[b[0]][b[1]]);
            }
        });
        dist[u.x()][u.y()] = 0;
        pq.add(new int[] {u.x(), u.y()});
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int cx = cur[0];
            int cy = cur[1];
            if (cx == tx && cy == ty) {
                break;
            }
            for (int d = 0; d < 8; d++) {
                int nx = cx + dx[d];
                int ny = cy + dy[d];
                if (!map.walkable(nx, ny)) {
                    continue;
                }
                if (!(nx == tx && ny == ty) && unitAt(nx, ny) != null) {
                    continue; // can't path through occupied tiles
                }
                boolean diag = (d % 2) == 1;
                int base = map.tile(nx, ny).moveCost();
                int step = diag ? ruleset.timeUnits().walkDiagonalCost(base)
                                : ruleset.timeUnits().walkCost(base);
                int nd = dist[cx][cy] + step;
                if (nd < dist[nx][ny]) {
                    dist[nx][ny] = nd;
                    prev[nx][ny] = cx * h + cy;
                    pq.add(new int[] {nx, ny});
                }
            }
        }
        if (dist[tx][ty] == Integer.MAX_VALUE) {
            return empty;
        }
        List<int[]> rev = new ArrayList<int[]>();
        int cx = tx;
        int cy = ty;
        while (!(cx == u.x() && cy == u.y())) {
            rev.add(new int[] {cx, cy});
            int p = prev[cx][cy];
            if (p < 0) {
                return empty;
            }
            cx = p / h;
            cy = p % h;
        }
        List<int[]> path = new ArrayList<int[]>();
        for (int i = rev.size() - 1; i >= 0; i--) {
            path.add(rev.get(i));
        }
        return path;
    }

    /**
     * Walk {@code u} toward {@code (tx,ty)} as far as its TU allow, stepping tile
     * by tile. Each step may draw enemy reaction fire; movement stops early if the
     * unit is killed. Returns the number of tiles actually moved.
     */
    public int moveUnit(BattleUnit u, int tx, int ty) {
        if (u == null || !u.alive()) {
            return 0;
        }
        List<int[]> path = pathTo(u, tx, ty);
        int moved = 0;
        for (int[] step : path) {
            boolean diag = Math.abs(step[0] - u.x()) == 1 && Math.abs(step[1] - u.y()) == 1;
            int base = map.tile(step[0], step[1]).moveCost();
            int cost = diag ? ruleset.timeUnits().walkDiagonalCost(base)
                            : ruleset.timeUnits().walkCost(base);
            if (!u.hasTU(cost)) {
                break;
            }
            u.setFacing(directionTo(step[0] - u.x(), step[1] - u.y()));
            u.spendTU(cost);
            u.setPos(step[0], step[1]);
            moved++;
            recomputeVisibility();
            reactionFireAgainst(u);
            if (!u.alive()) {
                break;
            }
        }
        listener.onChanged();
        checkVictory();
        return moved;
    }

    // ---- shooting -----------------------------------------------------------

    /** To-hit percent (clamped 0..100) for a HUD preview; 0 if the mode is illegal. */
    public int hitChance(BattleUnit shooter, BattleUnit target, FireMode mode) {
        WeaponDef w = shooter.weapon();
        if (w == null || !w.supports(mode)) {
            return 0;
        }
        int dist = BattleMap.distance(shooter.x(), shooter.y(), target.x(), target.y());
        ShotContext ctx = new ShotContext(shooter.firingAccuracy(), shooter.kneeling(),
                w.twoHanded(), true, 0, dist, false);
        int pct = ruleset.accuracy().hitChancePercent(w, mode, ctx);
        return Math.max(0, Math.min(100, pct));
    }

    public int fireCost(BattleUnit shooter, FireMode mode) {
        WeaponDef w = shooter.weapon();
        if (w == null || !w.supports(mode)) {
            return Integer.MAX_VALUE;
        }
        return ruleset.timeUnits().fireCost(w, mode, shooter.maxTU());
    }

    /**
     * {@code shooter} fires {@code mode} at {@code target}. Costs TU, consumes
     * ammo, rolls each shot through the ruleset accuracy + damage models. Returns
     * true if the shot was taken.
     */
    public boolean fire(BattleUnit shooter, BattleUnit target, FireMode mode) {
        if (shooter == null || target == null || !shooter.alive() || !target.alive()) {
            return false;
        }
        WeaponDef w = shooter.weapon();
        if (w == null || !w.supports(mode)) {
            return false;
        }
        int cost = fireCost(shooter, mode);
        if (!shooter.hasTU(cost)) {
            return false;
        }
        if (!map.hasLineOfSight(shooter.x(), shooter.y(), target.x(), target.y())) {
            return false;
        }
        shooter.spendTU(cost);
        shooter.setFacing(directionTo(target.x() - shooter.x(), target.y() - shooter.y()));
        int shots = Math.max(1, w.shots(mode));
        int hits = 0;
        for (int i = 0; i < shots && target.alive(); i++) {
            if (!shooter.consumeAmmo()) {
                break;
            }
            int chance = hitChance(shooter, target, mode);
            if (rng.chance(chance / 100.0)) {
                hits++;
                DamageModel.Facing facing = facingHit(target, shooter);
                int dmg = ruleset.damage().rollDamage(rng, w.power(), w.damageType(),
                        target.armor(), facing);
                boolean dead = target.applyDamage(dmg);
                listener.onEvent(shooter.name() + " hits " + target.name() + " for " + dmg
                        + (dead ? " — KILLED!" : ""));
                if (dead) {
                    onDeath(target);
                }
            } else {
                listener.onEvent(shooter.name() + " misses " + target.name());
            }
        }
        if (hits == 0) {
            listener.onEvent(shooter.name() + " fires at " + target.name() + " (no hits)");
        }
        recomputeVisibility();
        listener.onChanged();
        checkVictory();
        return true;
    }

    /** Max tiles a unit can throw a grenade (strength-scaled). */
    public int throwRange(BattleUnit u) {
        return 5 + u.strength() / 8;
    }

    public int grenadeCost(BattleUnit u) {
        return (int) Math.round(u.maxTU() * GRENADE_TU_PERCENT / 100.0);
    }

    /**
     * {@code unit} lobs a grenade onto {@code (tx,ty)}: costs TU, consumes a
     * grenade, and detonates a high-explosive blast that damages units and
     * destroys terrain in a radius. Returns true if the throw happened.
     */
    public boolean throwGrenade(BattleUnit unit, int tx, int ty) {
        if (unit == null || !unit.alive() || unit.grenades() <= 0 || !map.inBounds(tx, ty)) {
            return false;
        }
        if (BattleMap.distance(unit.x(), unit.y(), tx, ty) > throwRange(unit)) {
            return false;
        }
        int cost = grenadeCost(unit);
        if (!unit.hasTU(cost)) {
            return false;
        }
        unit.spendTU(cost);
        unit.useGrenade();
        unit.setFacing(directionTo(tx - unit.x(), ty - unit.y()));
        listener.onEvent(unit.name() + " throws a grenade!");
        explode(tx, ty, GRENADE_POWER, DamageType.HIGH_EXPLOSIVE);
        checkVictory();
        return true;
    }

    /** Detonate a blast of {@code power} at {@code (cx,cy)}: terrain + unit damage. */
    public void explode(int cx, int cy, int power, DamageType type) {
        List<int[]> tiles = new ArrayList<int[]>();
        for (int dx = -GRENADE_RADIUS; dx <= GRENADE_RADIUS; dx++) {
            for (int dy = -GRENADE_RADIUS; dy <= GRENADE_RADIUS; dy++) {
                int x = cx + dx;
                int y = cy + dy;
                if (!map.inBounds(x, y)) {
                    continue;
                }
                int d = BattleMap.distance(cx, cy, x, y);
                if (d > GRENADE_RADIUS) {
                    continue;
                }
                tiles.add(new int[] {x, y});
                map.tile(x, y).destroy();
                BattleUnit u = unitAt(x, y);
                if (u != null) {
                    int eff = power * (GRENADE_RADIUS + 1 - d) / (GRENADE_RADIUS + 1);
                    int dmg = ruleset.damage().rollDamage(rng, eff, type, u.armor(),
                            DamageModel.Facing.UNDER);
                    boolean dead = u.applyDamage(dmg);
                    listener.onEvent("  " + u.name() + " caught in blast (-" + dmg + ")"
                            + (dead ? " — KILLED!" : ""));
                    if (dead) {
                        onDeath(u);
                    }
                }
            }
        }
        recomputeVisibility();
        listener.onExplosion(tiles);
        listener.onChanged();
    }

    private void reactionFireAgainst(BattleUnit mover) {
        List<BattleUnit> reactors = living(mover.side().opponent());
        for (BattleUnit r : reactors) {
            if (!mover.alive()) {
                return;
            }
            WeaponDef w = r.weapon();
            if (w == null || !w.supports(FireMode.SNAP)) {
                continue;
            }
            if (!canSee(r, mover)) {
                continue;
            }
            int snapCost = fireCost(r, FireMode.SNAP);
            if (!r.hasTU(snapCost)) {
                continue;
            }
            boolean triggers = ruleset.reactions().triggers(
                    r.reactions(), r.tu(), r.maxTU(),
                    mover.reactions(), mover.tu(), mover.maxTU());
            if (triggers) {
                listener.onEvent(r.name() + " reaction fire!");
                fire(r, mover, FireMode.SNAP);
            }
        }
    }

    private void onDeath(BattleUnit u) {
        listener.onEvent(u.name() + " is down.");
        if (u.alien()) {
            aliensKilled++;
        }
        // Morale hit to the fallen unit's side.
        for (BattleUnit m : living(u.side())) {
            m.changeMorale(-10);
        }
        recomputeVisibility();
    }

    // ---- turn flow + AI -----------------------------------------------------

    /** End the player's turn: aliens refresh and act, then a new player turn begins. */
    public void endTurn() {
        if (winner != null) {
            return;
        }
        currentSide = Side.ALIEN;
        for (BattleUnit a : living(Side.ALIEN)) {
            a.refreshTU();
        }
        listener.onEvent("— Alien turn —");
        runAlienTurn();
        checkVictory();
        if (winner != null) {
            listener.onChanged();
            return;
        }
        currentSide = Side.XCOM;
        for (BattleUnit s : living(Side.XCOM)) {
            s.refreshTU();
        }
        processPanic();
        turn++;
        listener.onEvent("— Turn " + turn + " —");
        listener.onChanged();
    }

    /** Panicked soldiers cower for their turn (lose all TU), then recover. */
    private void processPanic() {
        for (BattleUnit s : living(Side.XCOM)) {
            if (s.panicked()) {
                s.spendTU(s.tu());
                s.setPanicked(false);
                listener.onEvent(s.name() + " is panicking and cannot act this turn!");
            }
        }
    }

    private void runAlienTurn() {
        int guard = 0;
        boolean acted = true;
        while (acted && guard++ < 500 && winner == null) {
            acted = false;
            for (BattleUnit a : living(Side.ALIEN)) {
                if (aiActOnce(a)) {
                    acted = true;
                }
            }
        }
    }

    private static final int PSI_TU_PERCENT = 25;
    private static final int PSI_MIN_STRENGTH = 45; // only leaders/commanders wield psi

    public int psiCost(BattleUnit u) {
        return (int) Math.round(u.maxTU() * PSI_TU_PERCENT / 100.0);
    }

    /** A psi-capable alien assaults a soldier's mind; on success the soldier panics. */
    public boolean psiAttack(BattleUnit attacker, BattleUnit target) {
        if (attacker == null || target == null || !attacker.alive() || !target.alive()) {
            return false;
        }
        int cost = psiCost(attacker);
        if (!attacker.hasTU(cost)) {
            return false;
        }
        attacker.spendTU(cost);
        int dist = BattleMap.distance(attacker.x(), attacker.y(), target.x(), target.y());
        int chance = ruleset.psi().panicChancePercent(
                attacker.psiStrength(), target.psiStrength(), dist);
        if (rng.chance(chance / 100.0)) {
            target.setPanicked(true);
            target.changeMorale(-30);
            listener.onEvent(attacker.name() + " assaults " + target.name()
                    + "'s mind — panic sets in!");
        } else {
            listener.onEvent(target.name() + " resists a psi attack.");
        }
        listener.onChanged();
        return true;
    }

    /** One small alien action: psi-attack, shoot a visible soldier, else advance. */
    private boolean aiActOnce(BattleUnit a) {
        BattleUnit target = nearestVisibleFoe(a);
        // Psi-capable leaders prefer to break the squad's will.
        if (target != null && a.psiStrength() >= PSI_MIN_STRENGTH && !target.panicked()
                && a.hasTU(psiCost(a))
                && ruleset.psi().panicChancePercent(a.psiStrength(), target.psiStrength(),
                        BattleMap.distance(a.x(), a.y(), target.x(), target.y())) > 0) {
            return psiAttack(a, target);
        }
        WeaponDef w = a.weapon();
        if (target != null && w != null) {
            // Prefer the best affordable shot with a real hit chance.
            FireMode mode = chooseAlienMode(a, target);
            if (mode != null && a.hasTU(fireCost(a, mode))) {
                return fire(a, target, mode);
            }
        }
        // Otherwise advance toward the nearest soldier (known even without LOS).
        BattleUnit foe = target != null ? target : nearestFoe(a);
        if (foe == null) {
            return false;
        }
        int[] step = stepToward(a, foe.x(), foe.y());
        if (step == null) {
            return false;
        }
        int before = a.tu();
        moveUnit(a, step[0], step[1]);
        return a.tu() < before && a.alive();
    }

    private FireMode chooseAlienMode(BattleUnit a, BattleUnit target) {
        FireMode[] order = {FireMode.AIMED, FireMode.SNAP, FireMode.AUTO};
        for (FireMode m : order) {
            if (a.weapon().supports(m) && a.hasTU(fireCost(a, m)) && hitChance(a, target, m) > 0) {
                // Snap is cheaper; use aimed only when we can afford it and target is far.
                if (m == FireMode.AIMED
                        && BattleMap.distance(a.x(), a.y(), target.x(), target.y()) < 6) {
                    continue;
                }
                return m;
            }
        }
        for (FireMode m : order) {
            if (a.weapon().supports(m) && a.hasTU(fireCost(a, m)) && hitChance(a, target, m) > 0) {
                return m;
            }
        }
        return null;
    }

    private BattleUnit nearestVisibleFoe(BattleUnit a) {
        BattleUnit best = null;
        int bestD = Integer.MAX_VALUE;
        for (BattleUnit f : living(a.side().opponent())) {
            if (canSee(a, f)) {
                int d = BattleMap.distance(a.x(), a.y(), f.x(), f.y());
                if (d < bestD) {
                    bestD = d;
                    best = f;
                }
            }
        }
        return best;
    }

    private BattleUnit nearestFoe(BattleUnit a) {
        BattleUnit best = null;
        int bestD = Integer.MAX_VALUE;
        for (BattleUnit f : living(a.side().opponent())) {
            int d = BattleMap.distance(a.x(), a.y(), f.x(), f.y());
            if (d < bestD) {
                bestD = d;
                best = f;
            }
        }
        return best;
    }

    /** The next walkable, unoccupied tile toward a goal (greedy on Chebyshev). */
    private int[] stepToward(BattleUnit a, int gx, int gy) {
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        int bestD = Integer.MAX_VALUE;
        int[] best = null;
        for (int d = 0; d < 8; d++) {
            int nx = a.x() + dx[d];
            int ny = a.y() + dy[d];
            if (!map.walkable(nx, ny) || unitAt(nx, ny) != null) {
                continue;
            }
            int dd = BattleMap.distance(nx, ny, gx, gy);
            if (dd < bestD) {
                bestD = dd;
                best = new int[] {nx, ny};
            }
        }
        // only step if it gets us closer than we are now
        if (best != null && bestD < BattleMap.distance(a.x(), a.y(), gx, gy)) {
            return best;
        }
        return null;
    }

    // ---- victory ------------------------------------------------------------

    private void checkVictory() {
        if (winner != null) {
            return;
        }
        boolean aliensLeft = !living(Side.ALIEN).isEmpty();
        boolean xcomLeft = !living(Side.XCOM).isEmpty();
        if (!aliensLeft) {
            winner = Side.XCOM;
            listener.onEvent("*** All aliens eliminated — X-COM victory! ***");
        } else if (!xcomLeft) {
            winner = Side.ALIEN;
            listener.onEvent("*** Squad wiped out — mission failed. ***");
        }
    }

    public boolean finished() {
        return winner != null;
    }

    public Side winner() {
        return winner;
    }

    public BattleOutcome outcome() {
        List<String> alive = new ArrayList<String>();
        List<String> fallen = new ArrayList<String>();
        for (BattleUnit u : units) {
            if (u.side() == Side.XCOM) {
                (u.alive() ? alive : fallen).add(u.name());
            }
        }
        return new BattleOutcome(winner, turn, alive, fallen, aliensKilled);
    }

    /** Run the whole battle to completion with only the AI acting (headless helper). */
    public BattleOutcome autoResolve(int maxTurns) {
        int guard = 0;
        while (!finished() && guard++ < maxTurns) {
            // player does nothing; overwatch + alien turns decide it
            endTurn();
        }
        return outcome();
    }
}

package com.whim.populous.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import com.whim.populous.api.ActionResult;
import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.api.Enums.TerrainType;
import com.whim.populous.api.GameController;
import com.whim.populous.api.Views.FollowerView;
import com.whim.populous.api.Views.GameStateView;
import com.whim.populous.api.Views.MapView;
import com.whim.populous.api.Views.PapalMagnetView;
import com.whim.populous.api.Views.TileView;

/**
 * A dependency-free fake {@link GameController} so the UI (Task 3) is runnable,
 * testable and visually verifiable BEFORE the real engine (Task 2) exists. It
 * fakes a small evolving world: procedural terrain, a handful of followers that
 * wander / rally to a papal magnet, mana that ticks up with population, and
 * working terraforming from clicks.
 *
 * This is DEV-ONLY. Production wires the engine's real controller into
 * {@link GameFrame}. StubController deliberately depends only on {@code api}.
 *
 * Threading: a daemon thread ticks ~15/sec. All model mutation and snapshot
 * construction happen under {@link #lock}, so {@link #state()} is
 * snapshot-consistent and safe to call from the EDT.
 */
public class StubController implements GameController {

    private static final int COLS = 64;
    private static final int ROWS = 64;
    private static final int SEA_LEVEL = 0;
    private static final int MAX_MANA = 6000;
    private static final int POP_CAP = 60;

    private final Object lock = new Object();
    private final List<ChangeListener> listeners = new CopyOnWriteArrayList<ChangeListener>();

    // ---- live model (guarded by lock) ----
    private int[][] elevation = new int[ROWS][COLS];
    private TerrainType[][] override = new TerrainType[ROWS][COLS]; // swamp/lava, null=derive
    private Allegiance[][] owner = new Allegiance[ROWS][COLS];
    private SettlementType[][] settle = new SettlementType[ROWS][COLS];
    private int[][] settleLevel = new int[ROWS][COLS];

    private final List<Walker> walkers = new ArrayList<Walker>();
    private Magnet goodMagnet = new Magnet(Allegiance.GOOD);
    private Magnet evilMagnet = new Magnet(Allegiance.EVIL);

    private int goodMana = 800;
    private int evilMana = 800;
    private int goodPop = 0;
    private int evilPop = 0;
    private long tick = 0;
    private String status = "Ready";
    private boolean over = false;
    private Allegiance winner = Allegiance.NEUTRAL;
    private GodPower armed = GodPower.RAISE_LAND;

    private Random rng = new Random(0);
    private volatile Thread simThread;
    private volatile boolean running = false;

    public StubController() {
        newGame(1L);
    }

    // ===================== GameController =====================

    @Override
    public GameStateView state() {
        synchronized (lock) {
            return snapshot();
        }
    }

    @Override
    public void selectPower(GodPower power) {
        synchronized (lock) {
            armed = power;
            status = "Armed: " + power.label();
        }
        fire();
    }

    @Override
    public ActionResult primaryClick(int col, int row) {
        if (!inBounds(col, row)) {
            return ActionResult.fail("Out of bounds");
        }
        synchronized (lock) {
            GodPower p = armed;
            if (p == null || p == GodPower.RAISE_LAND) {
                raise(col, row, +1);
                return ActionResult.ok("Raised land");
            }
            if (p == GodPower.LOWER_LAND) {
                raise(col, row, -1);
                return ActionResult.ok("Lowered land");
            }
            if (!affordable(p)) {
                return ActionResult.fail(p.label() + " needs " + p.manaCost() + " mana");
            }
            return castTargeted(p, col, row);
        }
    }

    @Override
    public ActionResult secondaryClick(int col, int row) {
        if (!inBounds(col, row)) {
            return ActionResult.fail("Out of bounds");
        }
        synchronized (lock) {
            raise(col, row, -1);
        }
        fire();
        return ActionResult.ok("Lowered land");
    }

    @Override
    public ActionResult castGlobal(GodPower power) {
        synchronized (lock) {
            if (power.targeted()) {
                return ActionResult.fail(power.label() + " needs a target");
            }
            if (!affordable(power)) {
                return ActionResult.fail(power.label() + " needs " + power.manaCost() + " mana");
            }
            goodMana -= power.manaCost();
            if (power == GodPower.FLOOD) {
                floodStep();
                status = "FLOOD! the seas rise";
            } else if (power == GodPower.ARMAGEDDON) {
                armageddon();
                status = "ARMAGEDDON — final battle";
            }
        }
        fire();
        return ActionResult.ok(power.label() + " cast");
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        simThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    tickOnce();
                    try {
                        Thread.sleep(66);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        }, "stub-sim");
        simThread.setDaemon(true);
        simThread.start();
    }

    @Override
    public void stop() {
        running = false;
        Thread t = simThread;
        if (t != null) {
            t.interrupt();
        }
        simThread = null;
    }

    @Override
    public void newGame(long seed) {
        synchronized (lock) {
            rng = new Random(seed);
            generateTerrain();
            walkers.clear();
            goodMagnet = new Magnet(Allegiance.GOOD);
            evilMagnet = new Magnet(Allegiance.EVIL);
            goodMana = 800;
            evilMana = 800;
            tick = 0;
            over = false;
            winner = Allegiance.NEUTRAL;
            armed = GodPower.RAISE_LAND;
            status = "New game (seed " + seed + ")";
            seedSettlements();
            recount();
        }
        fire();
    }

    @Override
    public void tickOnce() {
        synchronized (lock) {
            if (over) {
                return;
            }
            tick++;
            stepWalkers();
            accrueMana();
            maybeBreed();
            recount();
            checkVictory();
        }
        fire();
    }

    @Override
    public void addChangeListener(ChangeListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    private void fire() {
        for (ChangeListener l : listeners) {
            l.onStateChanged();
        }
    }

    // ===================== world generation =====================

    private void generateTerrain() {
        // Value-noise style: random field, smoothed, biased into an island.
        double[][] f = new double[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                f[r][c] = rng.nextDouble();
            }
        }
        for (int pass = 0; pass < 4; pass++) {
            double[][] g = new double[ROWS][COLS];
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    double sum = 0;
                    int n = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            int rr = r + dr, cc = c + dc;
                            if (rr >= 0 && rr < ROWS && cc >= 0 && cc < COLS) {
                                sum += f[rr][cc];
                                n++;
                            }
                        }
                    }
                    g[r][c] = sum / n;
                }
            }
            f = g;
        }
        double cx = COLS / 2.0, cy = ROWS / 2.0, maxD = Math.hypot(cx, cy);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                double d = Math.hypot(c - cx, r - cy) / maxD;      // 0 centre -> 1 edge
                double v = f[r][c] * 1.6 - d * 1.25;               // island falloff
                int e = (int) Math.round(v * 8.0) - 1;             // -> roughly -5..+7
                elevation[r][c] = clampElev(e);
                override[r][c] = null;
                owner[r][c] = Allegiance.NEUTRAL;
                settle[r][c] = SettlementType.NONE;
                settleLevel[r][c] = 0;
            }
        }
    }

    private void seedSettlements() {
        // A couple of Good settlements west, Evil east, plus starting walkers.
        placeCamp(COLS / 4, ROWS / 2, Allegiance.GOOD);
        placeCamp(3 * COLS / 4, ROWS / 2, Allegiance.EVIL);
        for (int i = 0; i < 10; i++) {
            spawnWalker(Allegiance.GOOD, COLS / 4, ROWS / 2);
        }
        for (int i = 0; i < 10; i++) {
            spawnWalker(Allegiance.EVIL, 3 * COLS / 4, ROWS / 2);
        }
    }

    private void placeCamp(int col, int row, Allegiance a) {
        // Flatten a small plateau above sea so a settlement can sit on it.
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                int r = row + dr, c = col + dc;
                if (inBounds(c, r)) {
                    elevation[r][c] = 1;
                    override[r][c] = null;
                }
            }
        }
        owner[row][col] = a;
        settle[row][col] = SettlementType.HOUSE;
        settleLevel[row][col] = 1;
        owner[row][col + 1] = a;
        settle[row][col + 1] = SettlementType.TENT;
    }

    // ===================== simulation steps =====================

    private void stepWalkers() {
        for (int i = 0; i < walkers.size(); i++) {
            Walker w = walkers.get(i);
            if (!w.alive) {
                continue;
            }
            Magnet mag = (w.allegiance == Allegiance.GOOD) ? goodMagnet : evilMagnet;
            double tx, ty;
            if (mag.active) {
                tx = mag.col + 0.5;
                ty = mag.row + 0.5;
            } else {
                // wander with gentle drift
                tx = w.x + (rng.nextDouble() - 0.5) * 3;
                ty = w.y + (rng.nextDouble() - 0.5) * 3;
            }
            double dx = tx - (w.x + 0.5);
            double dy = ty - (w.y + 0.5);
            double len = Math.hypot(dx, dy);
            double speed = 0.15;
            if (len > 0.01) {
                w.x += dx / len * speed;
                w.y += dy / len * speed;
            }
            w.x = clamp(w.x, 0, COLS - 1);
            w.y = clamp(w.y, 0, ROWS - 1);

            // terrain effects
            int cc = (int) Math.floor(w.x);
            int rr = (int) Math.floor(w.y);
            TerrainType tt = terrainAt(cc, rr);
            if (tt == TerrainType.WATER || tt == TerrainType.SHALLOW || tt == TerrainType.SWAMP) {
                w.health -= 8; // drowning / sinking
            } else if (tt == TerrainType.LAVA) {
                w.health -= 20;
            } else {
                w.health = Math.min(100, w.health + 1);
            }
            w.stamina = Math.max(0, w.stamina - 1);
            if (w.health <= 0) {
                w.alive = false;
            }
        }
        // cull dead
        for (int i = walkers.size() - 1; i >= 0; i--) {
            if (!walkers.get(i).alive) {
                walkers.remove(i);
            }
        }
    }

    private void accrueMana() {
        goodMana = Math.min(MAX_MANA, goodMana + 2 + goodPop);
        evilMana = Math.min(MAX_MANA, evilMana + 2 + evilPop);
    }

    private void maybeBreed() {
        if (tick % 20 != 0) {
            return;
        }
        if (goodPop < POP_CAP && goodPop > 0 && rng.nextDouble() < 0.6) {
            spawnWalker(Allegiance.GOOD, COLS / 4, ROWS / 2);
        }
        if (evilPop < POP_CAP && evilPop > 0 && rng.nextDouble() < 0.6) {
            spawnWalker(Allegiance.EVIL, 3 * COLS / 4, ROWS / 2);
        }
    }

    private void recount() {
        int g = 0, e = 0;
        for (int i = 0; i < walkers.size(); i++) {
            Walker w = walkers.get(i);
            if (!w.alive) {
                continue;
            }
            if (w.allegiance == Allegiance.GOOD) {
                g++;
            } else if (w.allegiance == Allegiance.EVIL) {
                e++;
            }
        }
        goodPop = g;
        evilPop = e;
        if (!over) {
            status = "Good " + g + "  vs  Evil " + e
                    + (armed != null ? "   [" + armed.label() + "]" : "");
        }
    }

    private void checkVictory() {
        if (over) {
            return;
        }
        if (goodPop == 0 && evilPop > 0) {
            over = true;
            winner = Allegiance.EVIL;
            status = "EVIL wins";
        } else if (evilPop == 0 && goodPop > 0) {
            over = true;
            winner = Allegiance.GOOD;
            status = "GOOD wins";
        }
    }

    // ===================== power effects =====================

    private ActionResult castTargeted(GodPower p, int col, int row) {
        goodMana -= p.manaCost();
        switch (p) {
            case PAPAL_MAGNET:
                goodMagnet.active = true;
                goodMagnet.col = col;
                goodMagnet.row = row;
                status = "Papal magnet set";
                return ActionResult.ok("Papal magnet placed");
            case EARTHQUAKE:
                for (int i = -3; i <= 3; i++) {
                    int c = col + i;
                    int r = row + (int) Math.round(Math.sin(i) * 2);
                    if (inBounds(c, r)) {
                        elevation[r][c] = clampElev(elevation[r][c] - 2);
                        settle[r][c] = SettlementType.NONE;
                        owner[r][c] = Allegiance.NEUTRAL;
                    }
                }
                status = "Earthquake!";
                return ActionResult.ok("Earthquake");
            case SWAMP:
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (inBounds(col + dc, row + dr)) {
                            override[row + dr][col + dc] = TerrainType.SWAMP;
                        }
                    }
                }
                status = "Swamp spreads";
                return ActionResult.ok("Swamp");
            case VOLCANO:
                for (int dr = -3; dr <= 3; dr++) {
                    for (int dc = -3; dc <= 3; dc++) {
                        int c = col + dc, r = row + dr;
                        if (inBounds(c, r)) {
                            int rise = Math.max(0, 5 - (Math.abs(dr) + Math.abs(dc)));
                            elevation[r][c] = clampElev(elevation[r][c] + rise);
                            if (Math.abs(dr) + Math.abs(dc) <= 1) {
                                override[r][c] = TerrainType.LAVA;
                            }
                        }
                    }
                }
                status = "Volcano erupts";
                return ActionResult.ok("Volcano");
            default:
                return ActionResult.fail("Unhandled power");
        }
    }

    private void floodStep() {
        // Rising seas: drop everything a notch, drowning low walkers.
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                elevation[r][c] = clampElev(elevation[r][c] - 1);
            }
        }
    }

    private void armageddon() {
        int mc = COLS / 2, mr = ROWS / 2;
        for (int i = 0; i < walkers.size(); i++) {
            Walker w = walkers.get(i);
            w.x = mc + (rng.nextDouble() - 0.5) * 6;
            w.y = mr + (rng.nextDouble() - 0.5) * 6;
        }
        goodMagnet.active = false;
        evilMagnet.active = false;
    }

    // ===================== helpers =====================

    private void raise(int col, int row, int delta) {
        // Brush of 3x3, centre full delta, edges smoothed toward centre.
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int c = col + dc, r = row + dr;
                if (!inBounds(c, r)) {
                    continue;
                }
                int step = (dr == 0 && dc == 0) ? delta : delta; // uniform brush
                elevation[r][c] = clampElev(elevation[r][c] + step);
                if (override[r][c] != null && elevation[r][c] > SEA_LEVEL) {
                    override[r][c] = null; // raising clears swamp/lava mark
                }
                if (elevation[r][c] <= SEA_LEVEL) {
                    settle[r][c] = SettlementType.NONE; // sunk settlement
                    owner[r][c] = Allegiance.NEUTRAL;
                }
            }
        }
    }

    private void spawnWalker(Allegiance a, int col, int row) {
        Walker w = new Walker();
        w.allegiance = a;
        w.x = clamp(col + (rng.nextDouble() - 0.5) * 4, 0, COLS - 1);
        w.y = clamp(row + (rng.nextDouble() - 0.5) * 4, 0, ROWS - 1);
        w.health = 100;
        w.stamina = 100;
        w.alive = true;
        walkers.add(w);
    }

    private boolean affordable(GodPower p) {
        return goodMana >= p.manaCost();
    }

    private boolean inBounds(int col, int row) {
        return col >= 0 && col < COLS && row >= 0 && row < ROWS;
    }

    private static int clampElev(int e) {
        return e < -6 ? -6 : (e > 9 ? 9 : e);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private TerrainType terrainAt(int col, int row) {
        if (!inBounds(col, row)) {
            return TerrainType.WATER;
        }
        if (override[row][col] != null) {
            return override[row][col];
        }
        return deriveTerrain(elevation[row][col]);
    }

    private static TerrainType deriveTerrain(int e) {
        int rel = e - SEA_LEVEL;
        if (rel < -1) {
            return TerrainType.WATER;
        }
        if (rel == -1) {
            return TerrainType.SHALLOW;
        }
        if (rel == 0) {
            return TerrainType.SAND;
        }
        if (rel <= 2) {
            return TerrainType.GRASS;
        }
        if (rel <= 4) {
            return TerrainType.HILL;
        }
        if (rel <= 6) {
            return TerrainType.MOUNTAIN;
        }
        return TerrainType.ROCK;
    }

    // ===================== snapshot (immutable views) =====================

    private GameStateView snapshot() {
        SnapTile[][] tiles = new SnapTile[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                tiles[r][c] = new SnapTile(c, r, elevation[r][c], terrainAt(c, r),
                        owner[r][c], settle[r][c], settleLevel[r][c]);
            }
        }
        SnapMap map = new SnapMap(tiles);
        List<FollowerView> fs = new ArrayList<FollowerView>(walkers.size());
        for (int i = 0; i < walkers.size(); i++) {
            Walker w = walkers.get(i);
            fs.add(new SnapFollower(w.x, w.y, w.allegiance, w.health, w.stamina, w.alive));
        }
        return new Snapshot(map, fs, goodMana, evilMana, MAX_MANA,
                goodPop, evilPop, POP_CAP, armed,
                new SnapMagnet(goodMagnet), new SnapMagnet(evilMagnet),
                over, winner, tick, status);
    }

    // ---- mutable model structs ----
    private static final class Walker {
        double x, y;
        Allegiance allegiance;
        int health, stamina;
        boolean alive;
    }

    private static final class Magnet {
        boolean active;
        int col, row;
        final Allegiance side;
        Magnet(Allegiance side) { this.side = side; }
    }

    // ---- immutable snapshot view implementations ----
    private static final class SnapTile implements TileView {
        private final int col, row, elevation, level;
        private final TerrainType terrain;
        private final Allegiance owner;
        private final SettlementType settlement;
        SnapTile(int col, int row, int elevation, TerrainType terrain,
                 Allegiance owner, SettlementType settlement, int level) {
            this.col = col; this.row = row; this.elevation = elevation;
            this.terrain = terrain; this.owner = owner;
            this.settlement = settlement; this.level = level;
        }
        public int col() { return col; }
        public int row() { return row; }
        public int elevation() { return elevation; }
        public TerrainType terrain() { return terrain; }
        public Allegiance owner() { return owner; }
        public SettlementType settlement() { return settlement; }
        public int settlementLevel() { return level; }
    }

    private static final class SnapMap implements MapView {
        private final SnapTile[][] tiles;
        SnapMap(SnapTile[][] tiles) { this.tiles = tiles; }
        public int cols() { return COLS; }
        public int rows() { return ROWS; }
        public int seaLevel() { return SEA_LEVEL; }
        public TileView tileAt(int col, int row) { return tiles[row][col]; }
    }

    private static final class SnapFollower implements FollowerView {
        private final double x, y;
        private final Allegiance allegiance;
        private final int health, stamina;
        private final boolean alive;
        SnapFollower(double x, double y, Allegiance a, int h, int s, boolean alive) {
            this.x = x; this.y = y; this.allegiance = a;
            this.health = h; this.stamina = s; this.alive = alive;
        }
        public double x() { return x; }
        public double y() { return y; }
        public Allegiance allegiance() { return allegiance; }
        public int health() { return health; }
        public int stamina() { return stamina; }
        public boolean alive() { return alive; }
    }

    private static final class SnapMagnet implements PapalMagnetView {
        private final boolean active;
        private final int col, row;
        private final Allegiance side;
        SnapMagnet(Magnet m) {
            this.active = m.active; this.col = m.col; this.row = m.row; this.side = m.side;
        }
        public boolean active() { return active; }
        public int col() { return col; }
        public int row() { return row; }
        public Allegiance side() { return side; }
    }

    private static final class Snapshot implements GameStateView {
        private final MapView map;
        private final List<FollowerView> followers;
        private final int goodMana, evilMana, maxMana, goodPop, evilPop, popCap;
        private final GodPower armed;
        private final PapalMagnetView goodMag, evilMag;
        private final boolean over;
        private final Allegiance winner;
        private final long tick;
        private final String status;
        Snapshot(MapView map, List<FollowerView> followers, int goodMana, int evilMana,
                 int maxMana, int goodPop, int evilPop, int popCap, GodPower armed,
                 PapalMagnetView goodMag, PapalMagnetView evilMag, boolean over,
                 Allegiance winner, long tick, String status) {
            this.map = map; this.followers = followers; this.goodMana = goodMana;
            this.evilMana = evilMana; this.maxMana = maxMana; this.goodPop = goodPop;
            this.evilPop = evilPop; this.popCap = popCap; this.armed = armed;
            this.goodMag = goodMag; this.evilMag = evilMag; this.over = over;
            this.winner = winner; this.tick = tick; this.status = status;
        }
        public MapView map() { return map; }
        public List<FollowerView> followers() { return followers; }
        public int goodMana() { return goodMana; }
        public int evilMana() { return evilMana; }
        public int maxMana() { return maxMana; }
        public int goodPopulation() { return goodPop; }
        public int evilPopulation() { return evilPop; }
        public int populationCap() { return popCap; }
        public GodPower selectedPower() { return armed; }
        public boolean powerAffordable(GodPower p) { return goodMana >= p.manaCost(); }
        public PapalMagnetView goodMagnet() { return goodMag; }
        public PapalMagnetView evilMagnet() { return evilMag; }
        public boolean gameOver() { return over; }
        public Allegiance winner() { return winner; }
        public long tick() { return tick; }
        public String statusLine() { return status; }
    }
}

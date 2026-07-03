package com.whim.powermonger.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.powermonger.api.ActionResult;
import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Job;
import com.whim.powermonger.api.Enums.Posture;
import com.whim.powermonger.api.Enums.Season;
import com.whim.powermonger.api.Enums.TerrainType;
import com.whim.powermonger.api.Enums.Weather;
import com.whim.powermonger.api.GameController;
import com.whim.powermonger.api.Views.CaptainView;
import com.whim.powermonger.api.Views.GameStateView;
import com.whim.powermonger.api.Views.PigeonView;
import com.whim.powermonger.api.Views.TileView;
import com.whim.powermonger.api.Views.TownView;
import com.whim.powermonger.api.Views.TownspersonView;

/**
 * Dev-only {@link GameController}: a static, hand-built world snapshot so the UI
 * package compiles and runs WITHOUT the real engine. A tiny wander animation
 * runs on {@link #start()} so the frame looks alive. The orchestrator swaps in
 * the real {@code engine.GameEngine} via {@code app.Main}.
 *
 * <p>Depends only on {@code api}; imports no domain/engine types.</p>
 */
public final class StubController implements GameController {

    private static final int W = 26;
    private static final int H = 20;
    private static final int MAX_ELEV = 6;

    private final Tile[][] tiles = new Tile[W][H];
    private final List<TownView> towns = new ArrayList<TownView>();
    private final List<Person> people = new ArrayList<Person>();
    private final List<Cap> caps = new ArrayList<Cap>();
    private final List<Pigeon> pigeons = new ArrayList<Pigeon>();

    private int selectedId = -1;
    private long tick = 0;
    private Thread anim;
    private volatile boolean running;

    public StubController() {
        newGame(1L);
    }

    // ---- world construction --------------------------------------------

    @Override public void newGame(long seed) {
        buildTerrain();
        buildTowns();
        buildPeople();
        buildCaptains();
        pigeons.clear();
        // One in-flight pigeon (command lag) from commander to a subordinate.
        pigeons.add(new Pigeon(caps.get(0), caps.get(2), CommandType.SCOUT));
        selectedId = caps.get(0).id;
    }

    private void buildTerrain() {
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                // Smooth pseudo-height field (deterministic, no assets).
                double nx = x / (double) W, ny = y / (double) H;
                double hgt = 0.5
                        + 0.35 * Math.sin(nx * Math.PI * 2 + 0.4)
                        + 0.30 * Math.cos(ny * Math.PI * 2 - 0.7)
                        + 0.18 * Math.sin((nx + ny) * Math.PI * 3);
                hgt = (hgt + 1) / 2.0; // ~[0,1]
                int elev = (int) Math.round(clamp01(hgt) * MAX_ELEV);
                TerrainType terr;
                boolean trees = false;
                if (hgt < 0.30) { terr = TerrainType.DEEP_WATER; elev = 0; }
                else if (hgt < 0.38) { terr = TerrainType.SHALLOW_WATER; elev = 0; }
                else if (hgt < 0.44) { terr = TerrainType.BEACH; elev = Math.min(elev, 1); }
                else if (hgt < 0.62) { terr = TerrainType.GRASS; }
                else if (hgt < 0.74) { terr = TerrainType.FOREST; trees = true; }
                else if (hgt < 0.86) { terr = TerrainType.HILL; }
                else { terr = TerrainType.MOUNTAIN; }
                int food = terr == TerrainType.GRASS ? 60
                         : terr == TerrainType.FOREST ? 40
                         : terr == TerrainType.SHALLOW_WATER ? 30 : 10;
                boolean snow = terr == TerrainType.MOUNTAIN && elev >= 5;
                tiles[x][y] = new Tile(x, y, terr, elev, trees, food, snow);
            }
        }
    }

    private void buildTowns() {
        towns.clear();
        addTown(0, 9, 7, "Aldreth", 120, Allegiance.NEUTRAL);
        addTown(1, 17, 12, "Bramwyck", 90, Allegiance.ENEMY);
    }

    private void addTown(int id, int tx, int ty, String name, int pop, Allegiance a) {
        Tile t = tiles[tx][ty];
        // Force a town tile with modest elevation.
        tiles[tx][ty] = new Tile(tx, ty, TerrainType.TOWN,
                Math.max(1, Math.min(3, t.elevation())), false, 20, false);
        tiles[tx][ty].townId = id;
        towns.add(new Town(id, tx, ty, name, pop, a));
    }

    private void buildPeople() {
        people.clear();
        people.add(new Person(0, 8.5, 6.5, Job.FARMING));
        people.add(new Person(1, 10.5, 7.5, Job.HERDING));
        people.add(new Person(2, 9.5, 8.5, Job.CRAFTING));
        people.add(new Person(3, 6.0, 10.0, Job.FISHING));
        people.add(new Person(4, 18.0, 11.0, Job.FARMING));
    }

    private void buildCaptains() {
        caps.clear();
        caps.add(new Cap(0, "Sir Cedric", 8.0, 10.0, Allegiance.PLAYER, 130, 80, true));
        caps.add(new Cap(1, "Aldous", 7.0, 12.0, Allegiance.PLAYER, 70, 40, false));
        caps.add(new Cap(2, "Merek", 11.0, 11.0, Allegiance.PLAYER, 55, 30, false));
        caps.add(new Cap(3, "Godwin", 9.0, 13.0, Allegiance.PLAYER, 40, 25, false));
        caps.add(new Cap(4, "Blackthorn", 16.0, 12.0, Allegiance.ENEMY, 110, 60, false));
        caps.add(new Cap(5, "Vane", 19.0, 14.0, Allegiance.ENEMY, 80, 45, false));
        caps.get(1).posture = Posture.AGGRESSIVE;
        caps.get(2).posture = Posture.PASSIVE;
    }

    // ---- GameController -------------------------------------------------

    @Override public synchronized GameStateView state() {
        return new Snapshot();
    }

    @Override public synchronized ActionResult issueOrder(int id, CommandType type, int tx, int ty) {
        Cap c = cap(id);
        if (c == null) return ActionResult.fail("No such captain");
        c.order = type;
        if (type == CommandType.MOVE) { c.hasDest = true; c.destX = tx; c.destY = ty; }
        return ActionResult.ok(c.name + ": " + type.label());
    }

    @Override public synchronized ActionResult setDestination(int id, int tx, int ty) {
        Cap c = cap(id);
        if (c == null) return ActionResult.fail("No such captain");
        c.hasDest = true; c.destX = tx; c.destY = ty; c.order = CommandType.MOVE;
        return ActionResult.ok(c.name + " marching to " + tx + "," + ty);
    }

    @Override public synchronized ActionResult setPosture(int id, Posture p) {
        Cap c = cap(id);
        if (c == null) return ActionResult.fail("No such captain");
        c.posture = p;
        return ActionResult.ok(c.name + " posture " + p);
    }

    @Override public synchronized void selectCaptain(int id) {
        selectedId = id;
    }

    @Override public synchronized int selectedCaptainId() { return selectedId; }

    @Override public void start() {
        if (running) return;
        running = true;
        anim = new Thread(new Runnable() {
            @Override public void run() {
                while (running) {
                    stepAnimation();
                    try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                }
            }
        }, "stub-anim");
        anim.setDaemon(true);
        anim.start();
    }

    @Override public void stop() {
        running = false;
        if (anim != null) anim.interrupt();
    }

    /** Cheap wander/march animation so the stub UI is not static. */
    private synchronized void stepAnimation() {
        tick++;
        // Captains drift toward their destination.
        for (int i = 0; i < caps.size(); i++) {
            Cap c = caps.get(i);
            if (!c.alive) continue;
            if (c.hasDest) {
                double dx = c.destX - c.x, dy = c.destY - c.y;
                double d = Math.hypot(dx, dy);
                if (d < 0.05) { c.hasDest = false; }
                else { c.x += dx / d * 0.04; c.y += dy / d * 0.04; }
            }
        }
        // Townspeople wander gently around their home point.
        for (int i = 0; i < people.size(); i++) {
            Person p = people.get(i);
            p.phase += 0.05;
            p.x = p.homeX + Math.cos(p.phase + i) * 0.6;
            p.y = p.homeY + Math.sin(p.phase * 0.8 + i) * 0.6;
        }
        // Pigeon flight progresses.
        for (int i = 0; i < pigeons.size(); i++) {
            pigeons.get(i).advance(0.006);
        }
    }

    private Cap cap(int id) {
        for (int i = 0; i < caps.size(); i++) if (caps.get(i).id == id) return caps.get(i);
        return null;
    }

    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    // ---- view holders (implement api.Views.*) --------------------------

    private static final class Tile implements TileView {
        final int x, y; final TerrainType terr; final int elev;
        final boolean trees; final int food; final boolean snow;
        int townId = -1;
        Tile(int x, int y, TerrainType t, int e, boolean tr, int f, boolean s) {
            this.x = x; this.y = y; this.terr = t; this.elev = e;
            this.trees = tr; this.food = f; this.snow = s;
        }
        public int x() { return x; }
        public int y() { return y; }
        public TerrainType terrain() { return terr; }
        public int elevation() { return elev; }
        public boolean hasTown() { return townId >= 0; }
        public int townId() { return townId; }
        public boolean hasTrees() { return trees; }
        public int foodPotential() { return food; }
        public boolean snowCovered() { return snow; }
    }

    private static final class Town implements TownView {
        final int id, tx, ty, pop; final String name; final Allegiance a;
        Town(int id, int tx, int ty, String name, int pop, Allegiance a) {
            this.id = id; this.tx = tx; this.ty = ty; this.name = name; this.pop = pop; this.a = a;
        }
        public int id() { return id; }
        public int tileX() { return tx; }
        public int tileY() { return ty; }
        public String name() { return name; }
        public int population() { return pop; }
        public Allegiance allegiance() { return a; }
        public boolean captured() { return false; }
    }

    private static final class Person implements TownspersonView {
        final int id; double x, y; final double homeX, homeY; final Job job; double phase;
        Person(int id, double x, double y, Job job) {
            this.id = id; this.x = x; this.y = y; this.homeX = x; this.homeY = y; this.job = job;
        }
        public int id() { return id; }
        public double x() { return x; }
        public double y() { return y; }
        public Job job() { return job; }
        public Allegiance allegiance() { return Allegiance.NEUTRAL; }
    }

    private final class Cap implements CaptainView {
        final int id; final String name; double x, y; final Allegiance a;
        Posture posture = Posture.NEUTRAL; int strength, food;
        CommandType order = CommandType.MOVE;
        boolean hasDest; double destX, destY; final boolean supreme; boolean alive = true;
        Cap(int id, String name, double x, double y, Allegiance a, int str, int food, boolean supreme) {
            this.id = id; this.name = name; this.x = x; this.y = y; this.a = a;
            this.strength = str; this.food = food; this.supreme = supreme;
        }
        public int id() { return id; }
        public String name() { return name; }
        public double x() { return x; }
        public double y() { return y; }
        public Allegiance allegiance() { return a; }
        public Posture posture() { return posture; }
        public int strength() { return strength; }
        public int food() { return food; }
        public CommandType currentOrder() { return order; }
        public boolean hasDestination() { return hasDest; }
        public double destX() { return destX; }
        public double destY() { return destY; }
        public boolean selected() { return id == selectedId; }
        public boolean alive() { return alive; }
        public boolean supremeCommander() { return supreme; }
    }

    private static final class Pigeon implements PigeonView {
        final double sx, sy, tx, ty; final CommandType order; double prog;
        Pigeon(Cap from, Cap to, CommandType order) {
            this.sx = from.x; this.sy = from.y; this.tx = to.x; this.ty = to.y; this.order = order;
        }
        void advance(double d) { prog += d; if (prog > 1) prog = 0; }
        public double x() { return sx + (tx - sx) * prog; }
        public double y() { return sy + (ty - sy) * prog; }
        public double targetX() { return tx; }
        public double targetY() { return ty; }
        public CommandType order() { return order; }
        public double progress() { return prog; }
    }

    /** Immutable-ish snapshot handed to the UI each frame. */
    private final class Snapshot implements GameStateView {
        private final List<TownView> t = Collections.unmodifiableList(new ArrayList<TownView>(towns));
        private final List<TownspersonView> pp = Collections.unmodifiableList(new ArrayList<TownspersonView>(people));
        private final List<CaptainView> cc = Collections.unmodifiableList(new ArrayList<CaptainView>(caps));
        private final List<PigeonView> pg = Collections.unmodifiableList(new ArrayList<PigeonView>(pigeons));
        private final long snapTick = tick;

        public int mapWidth() { return W; }
        public int mapHeight() { return H; }
        public int maxElevation() { return MAX_ELEV; }
        public TileView tile(int x, int y) {
            if (x < 0 || y < 0 || x >= W || y >= H) return null;
            return tiles[x][y];
        }
        public List<TownView> towns() { return t; }
        public List<TownspersonView> townspeople() { return pp; }
        public List<CaptainView> captains() { return cc; }
        public List<PigeonView> pigeons() { return pg; }
        public Season season() { return Season.AUTUMN; }
        public Weather weather() { return Weather.CLEAR; }
        public double movementFactor() { return 0.9; }
        public double balanceOfPower() { return 0.15 + 0.05 * Math.sin(snapTick * 0.01); }
        public long tickCount() { return snapTick; }
        public boolean gameOver() { return false; }
        public boolean playerWon() { return false; }
        public String statusMessage() { return "Skirmish in progress"; }
    }
}

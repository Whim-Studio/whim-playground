package com.whim.warroom.engine;

import com.whim.warroom.domain.Faction;
import com.whim.warroom.domain.MapState;
import com.whim.warroom.domain.SandboxState;
import com.whim.warroom.domain.SimEngine;
import com.whim.warroom.domain.SimListener;
import com.whim.warroom.domain.SimSnapshot;
import com.whim.warroom.domain.Unit;
import com.whim.warroom.domain.Vec2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real-time, deterministic tactical simulation engine.
 *
 * <p>Owns a single background thread that converts wall-clock time into integer
 * ticks at {@code TICKS_PER_SECOND * speed} and fires {@link SimListener#onFrame}
 * once per presented tick while playing. The thread never touches Swing/AWT.
 *
 * <p><b>Determinism &amp; rewind.</b> The simulation is a pure function of the
 * loaded scenario plus its seed. Each simulated tick is <i>recorded</i> as an
 * immutable {@link SimSnapshot} in {@link #frames} (index == tick). A given tick
 * is therefore simulated exactly once; {@link #seek}/{@link #snapshotAt} for any
 * {@code t <= getMaxSimTick()} just return the recorded frame, and ticks beyond
 * are simulated forward on demand. Because we never re-simulate a tick, the same
 * tick always yields byte-identical positions (record-based rewind). A fresh
 * {@code Random} seeded from the scenario seed guarantees a {@link #reset} +
 * replay reproduces the identical sequence.
 */
public final class SimEngineImpl implements SimEngine {

    private static final int BLAST_LIFE_TICKS = 30; // ~0.5s fade for rendering

    // ---- config / listeners ----
    private final CopyOnWriteArrayList<SimListener> listeners = new CopyOnWriteArrayList<SimListener>();
    private volatile double speed = 1.0;
    private final AtomicBoolean playing = new AtomicBoolean(false);

    // ---- loaded scenario ----
    private volatile SandboxState loaded;

    // ---- simulation state (guarded by simLock) ----
    private final Object simLock = new Object();
    private final MovementSystem movement = new MovementSystem();
    private final CombatResolver combat = new CombatResolver();
    private final MoraleSystem morale = new MoraleSystem();
    private final AiController ai = new AiController();

    private MapState map;
    private List<Unit> units = new ArrayList<Unit>();
    private Random simRng = new Random(0);
    private final List<SimSnapshot> frames = new ArrayList<SimSnapshot>();
    private final Map<Integer, Integer> wpIndex = new HashMap<Integer, Integer>();
    private final Set<String> firedDeton = new HashSet<String>();
    private final List<ActiveBlast> activeBlasts = new ArrayList<ActiveBlast>();
    private boolean blueStart, redStart;

    private volatile int simTick = 0;      // furthest tick recorded
    private volatile int presentedTick = 0; // tick currently presented

    // ---- background thread ----
    private volatile boolean shutdown = false;
    private Thread thread;

    private static final class ActiveBlast {
        final double x, y, radius;
        final int bornTick;
        ActiveBlast(double x, double y, double radius, int bornTick) {
            this.x = x; this.y = y; this.radius = radius; this.bornTick = bornTick;
        }
    }

    public SimEngineImpl() {
        thread = new Thread(new Runnable() {
            public void run() { loop(); }
        }, "warroom-sim");
        thread.setDaemon(true);
        thread.start();
    }

    // ------------------------------------------------------------------
    // SimEngine API
    // ------------------------------------------------------------------

    public void loadScenario(SandboxState state) {
        synchronized (simLock) {
            this.loaded = state;
            initSim();
        }
    }

    public void addListener(SimListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    public void play() {
        if (loaded == null) {
            return;
        }
        playing.set(true);
    }

    public void pause() {
        playing.set(false);
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public void setSpeed(double multiplier) {
        if (multiplier > 0) {
            this.speed = multiplier;
        }
    }

    public double getSpeed() {
        return speed;
    }

    public void seek(int tick) {
        if (tick < 0) {
            tick = 0;
        }
        SimSnapshot frame;
        synchronized (simLock) {
            simulateTo(tick);
            presentedTick = tick;
            frame = frames.get(tick);
        }
        fireFrame(frame);
    }

    public int getCurrentTick() {
        return presentedTick;
    }

    public int getMaxSimTick() {
        return simTick;
    }

    public SimSnapshot snapshotAt(int tick) {
        if (tick < 0) {
            tick = 0;
        }
        synchronized (simLock) {
            simulateTo(tick);
            return frames.get(tick);
        }
    }

    public void reset() {
        synchronized (simLock) {
            playing.set(false);
            initSim();
        }
        SimSnapshot frame;
        synchronized (simLock) {
            frame = frames.get(0);
        }
        fireFrame(frame);
    }

    public void shutdown() {
        shutdown = true;
        playing.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    // ------------------------------------------------------------------
    // Simulation core
    // ------------------------------------------------------------------

    /** Rebuild the working world from the loaded scenario at tick 0. */
    private void initSim() {
        map = loaded.getMap();
        units = new ArrayList<Unit>();
        for (int i = 0; i < loaded.getUnits().size(); i++) {
            units.add(cloneUnit(loaded.getUnits().get(i)));
        }
        simRng = new Random(loaded.getSeed());
        frames.clear();
        wpIndex.clear();
        firedDeton.clear();
        activeBlasts.clear();
        simTick = 0;
        presentedTick = 0;

        blueStart = false;
        redStart = false;
        for (int i = 0; i < units.size(); i++) {
            Faction f = units.get(i).getFaction();
            if (f == Faction.BLUE) blueStart = true;
            else if (f == Faction.RED) redStart = true;
        }
        frames.add(buildSnapshot(0, buildBlastViews(0), checkFinished()));
    }

    private static Unit cloneUnit(Unit u) {
        Unit c = new Unit(u.getId(), u.getType(), u.getFaction(),
                new Vec2(u.getPos().x, u.getPos().y));
        c.setHeading(u.getHeading());
        c.setHealth(u.getHealth());
        c.setMorale(u.getMorale());
        c.setStance(u.getStance());
        c.setRouted(u.isRouted());
        c.setRoute(u.getRoute()); // route is read-only during sim; safe to share
        return c;
    }

    /** Simulate forward until {@code simTick == target}. Caller holds simLock. */
    private void simulateTo(int target) {
        while (simTick < target) {
            step();
        }
    }

    /** Advance the world exactly one tick and record its frame. Caller holds simLock. */
    private void step() {
        int tick = simTick + 1;

        // capture liveness to detect deaths this tick
        boolean[] wasAlive = new boolean[units.size()];
        for (int i = 0; i < units.size(); i++) {
            wasAlive[i] = units.get(i).isAlive();
        }

        Map<Integer, AiController.Order> orders = ai.plan(units, map, tick);

        List<SimSnapshot.BlastView> newBlasts = new ArrayList<SimSnapshot.BlastView>();
        movement.update(units, map, tick, orders, wpIndex, firedDeton, newBlasts);
        for (int i = 0; i < newBlasts.size(); i++) {
            SimSnapshot.BlastView b = newBlasts.get(i);
            activeBlasts.add(new ActiveBlast(b.x, b.y, b.radius, tick));
        }

        combat.resolve(units, map, tick, simRng);

        List<Unit> deadThisTick = new ArrayList<Unit>();
        for (int i = 0; i < units.size(); i++) {
            if (wasAlive[i] && !units.get(i).isAlive()) {
                deadThisTick.add(units.get(i));
            }
        }

        morale.update(units, map, tick, combat.damageThisTick, deadThisTick);

        boolean finished = checkFinished();
        frames.add(buildSnapshot(tick, buildBlastViews(tick), finished));
        simTick = tick;
    }

    /** A side is defeated when it has no alive, un-routed unit. */
    private boolean checkFinished() {
        if (!(blueStart && redStart)) {
            return false; // need two opposing sides to declare a winner
        }
        boolean blueActive = false;
        boolean redActive = false;
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (!u.isAlive() || u.isRouted()) {
                continue;
            }
            if (u.getFaction() == Faction.BLUE) blueActive = true;
            else if (u.getFaction() == Faction.RED) redActive = true;
        }
        return !(blueActive && redActive);
    }

    private List<SimSnapshot.BlastView> buildBlastViews(int tick) {
        List<SimSnapshot.BlastView> out = new ArrayList<SimSnapshot.BlastView>();
        // prune expired, emit active with fade age
        for (int i = activeBlasts.size() - 1; i >= 0; i--) {
            ActiveBlast b = activeBlasts.get(i);
            int age = tick - b.bornTick;
            if (age < 0 || age >= BLAST_LIFE_TICKS) {
                if (age >= BLAST_LIFE_TICKS) {
                    activeBlasts.remove(i);
                }
                continue;
            }
            double frac = (double) age / (double) BLAST_LIFE_TICKS;
            out.add(new SimSnapshot.BlastView(b.x, b.y, b.radius, frac));
        }
        return out;
    }

    private SimSnapshot buildSnapshot(int tick, List<SimSnapshot.BlastView> blasts, boolean finished) {
        List<SimSnapshot.UnitView> views = new ArrayList<SimSnapshot.UnitView>();
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            views.add(new SimSnapshot.UnitView(
                    u.getId(),
                    u.getType().getId(),
                    u.getFaction(),
                    u.getPos().x, u.getPos().y,
                    u.getHeading(),
                    u.getHealth(), u.getType().getMaxHealth(),
                    u.getMorale(), u.getType().getMaxMorale(),
                    u.getStance(),
                    u.isRouted(),
                    u.isAlive()));
        }
        return new SimSnapshot(tick, views, blasts, finished);
    }

    private void fireFrame(SimSnapshot snap) {
        for (SimListener l : listeners) {
            l.onFrame(snap);
        }
    }

    // ------------------------------------------------------------------
    // Background thread
    // ------------------------------------------------------------------

    private void loop() {
        long last = System.nanoTime();
        double acc = 0.0; // accumulated fractional ticks owed
        while (!shutdown) {
            if (playing.get()) {
                long now = System.nanoTime();
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                acc += dt * TICKS_PER_SECOND * speed;
                int steps = (int) acc;
                if (steps > 0) {
                    acc -= steps;
                    for (int s = 0; s < steps && !shutdown && playing.get(); s++) {
                        SimSnapshot frame;
                        synchronized (simLock) {
                            int next = presentedTick + 1;
                            simulateTo(next);
                            presentedTick = next;
                            frame = frames.get(next);
                        }
                        fireFrame(frame);
                        if (frame.isFinished()) {
                            playing.set(false);
                            break;
                        }
                    }
                }
                sleepQuiet(4);
            } else {
                // idle: keep the clock from jumping on resume
                last = System.nanoTime();
                acc = 0.0;
                sleepQuiet(10);
            }
        }
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

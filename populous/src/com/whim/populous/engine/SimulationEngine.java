package com.whim.populous.engine;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.whim.populous.api.ActionResult;
import com.whim.populous.api.GameController;
import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.Views.GameStateView;
import com.whim.populous.domain.Follower;
import com.whim.populous.domain.GameState;
import com.whim.populous.domain.GameStateManager;

/**
 * The simulation engine — the single implementation of {@link GameController}
 * that the UI talks to. It owns a {@link GameStateManager} and drives the live
 * {@link GameState} on a background {@link SimLoop} at ~30 ticks/sec, routing
 * player powers through {@link DivinePowers}, running the {@link RivalAI},
 * accruing mana via {@link ManaSystem}, and ending the game through
 * {@link VictoryMonitor}.
 *
 * Threading:
 *  - All state mutation (ticks + player actions) happens under {@link #lock}.
 *  - {@link #state()} returns an immutable {@link Snapshots.SnapState} rebuilt
 *    under the lock after every mutation, so EDT reads never tear.
 *  - {@link GameController.ChangeListener#onStateChanged()} is always fired off
 *    the EDT via a dedicated notifier thread, even for EDT-originated actions.
 *
 * The human player is the GOOD deity; the {@link RivalAI} is EVIL.
 */
public final class SimulationEngine implements GameController {

    private static final Allegiance PLAYER = Allegiance.GOOD;

    private final Object lock = new Object();

    private final CopyOnWriteArrayList<ChangeListener> listeners =
            new CopyOnWriteArrayList<ChangeListener>();
    private final ExecutorService notifier;

    // Guarded by lock:
    private final GameStateManager manager = new GameStateManager();
    private GameState gs;
    private ManaSystem mana;
    private FollowerAI followerAI;
    private RivalAI rivalAI;
    private DivinePowers powers;
    private VictoryMonitor victory;
    private Random rng;

    private volatile GameStateView snapshot;

    private final SimLoop loop;

    public SimulationEngine() {
        this(0L);
    }

    public SimulationEngine(long seed) {
        this.notifier = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "populous-notifier");
                t.setDaemon(true);
                return t;
            }
        });
        this.loop = new SimLoop(new Runnable() {
            public void run() {
                driveOneTick();
            }
        });
        newGame(seed);
    }

    // ------------------------------------------------------------------
    // GameController — state.
    // ------------------------------------------------------------------

    public GameStateView state() {
        return snapshot;
    }

    // ------------------------------------------------------------------
    // GameController — player actions (called from the EDT).
    // ------------------------------------------------------------------

    public void selectPower(GodPower power) {
        if (power == null) {
            return;
        }
        synchronized (lock) {
            gs.setSelectedPower(power);
            rebuildSnapshot();
        }
        fireChanged();
    }

    public ActionResult primaryClick(int col, int row) {
        ActionResult result;
        synchronized (lock) {
            GodPower p = gs.selectedPower();
            if (p == null) {
                p = GodPower.RAISE_LAND;
            }
            result = castTargeted(p, col, row);
            rebuildSnapshot();
        }
        fireChanged();
        return result;
    }

    public ActionResult secondaryClick(int col, int row) {
        ActionResult result;
        synchronized (lock) {
            // Classic right-click always lowers land, regardless of armed power.
            result = powers.lower(gs, PLAYER, col, row);
            rebuildSnapshot();
        }
        fireChanged();
        return result;
    }

    public ActionResult castGlobal(GodPower power) {
        ActionResult result;
        synchronized (lock) {
            if (power == GodPower.FLOOD) {
                result = powers.flood(gs, PLAYER);
            } else if (power == GodPower.ARMAGEDDON) {
                result = powers.armageddon(gs, PLAYER);
            } else {
                result = ActionResult.fail(power.label() + " is not a global power");
            }
            rebuildSnapshot();
        }
        fireChanged();
        return result;
    }

    private ActionResult castTargeted(GodPower p, int col, int row) {
        switch (p) {
            case RAISE_LAND:   return powers.raise(gs, PLAYER, col, row);
            case LOWER_LAND:   return powers.lower(gs, PLAYER, col, row);
            case PAPAL_MAGNET: return powers.papalMagnet(gs, PLAYER, col, row);
            case EARTHQUAKE:   return powers.earthquake(gs, PLAYER, col, row);
            case SWAMP:        return powers.swamp(gs, PLAYER, col, row);
            case VOLCANO:      return powers.volcano(gs, PLAYER, col, row);
            case FLOOD:        return powers.flood(gs, PLAYER);
            case ARMAGEDDON:   return powers.armageddon(gs, PLAYER);
            default:           return ActionResult.fail("Unknown power");
        }
    }

    // ------------------------------------------------------------------
    // GameController — lifecycle.
    // ------------------------------------------------------------------

    public void start() {
        loop.start();
    }

    public void stop() {
        loop.stop();
    }

    public void newGame(long seed) {
        synchronized (lock) {
            this.rng = new Random(seed);
            this.gs = manager.newGame(seed);
            this.mana = new ManaSystem();
            this.powers = new DivinePowers(rng);
            this.followerAI = new FollowerAI(rng);
            this.rivalAI = new RivalAI(rng);
            this.victory = new VictoryMonitor();
            gs.setSelectedPower(GodPower.RAISE_LAND);
            gs.setStatusLine("A new world awaits your hand.");
            rebuildSnapshot();
        }
        fireChanged();
    }

    public void tickOnce() {
        driveOneTick();
    }

    // ------------------------------------------------------------------
    // The tick.
    // ------------------------------------------------------------------

    private void driveOneTick() {
        boolean over;
        synchronized (lock) {
            if (gs.gameOver()) {
                return;
            }
            gs.incrementTick();

            mana.accrue(manager, gs);
            followerAI.update(gs);
            rivalAI.update(gs, powers);
            powers.tickGlobals(gs);
            gs.grid().ageTransients();

            reapDead();
            manager.recomputePopulations(gs);
            updateStatus();

            over = victory.check(gs);
            rebuildSnapshot();
        }
        fireChanged();
        if (over) {
            // Called on the sim thread — ask the loop to exit; never join self.
            loop.requestStop();
        }
    }

    /** Remove dead walkers from the live list and free their AI scratch. */
    private void reapDead() {
        List<Follower> followers = gs.followerList();
        Iterator<Follower> it = followers.iterator();
        while (it.hasNext()) {
            Follower f = it.next();
            if (!f.alive()) {
                followerAI.forget(f);
                it.remove();
            }
        }
    }

    private void updateStatus() {
        int good = gs.goodPopulation();
        int evil = gs.evilPopulation();
        StringBuilder sb = new StringBuilder();
        if (gs.gameOver()) {
            Allegiance w = gs.winner();
            sb.append(w == Allegiance.GOOD ? "Victory! Evil is vanquished."
                    : "Defeat — Evil rules the world.");
        } else {
            sb.append("Good ").append(good).append("  vs  Evil ").append(evil);
            if (powers.floodActive()) {
                sb.append("  |  FLOOD");
            }
            if (powers.armageddonActive()) {
                sb.append("  |  ARMAGEDDON");
            }
        }
        gs.setStatusLine(sb.toString());
    }

    // ------------------------------------------------------------------
    // Listeners — always fired off the EDT.
    // ------------------------------------------------------------------

    public void addChangeListener(ChangeListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    private void fireChanged() {
        if (listeners.isEmpty()) {
            return;
        }
        notifier.execute(new Runnable() {
            public void run() {
                for (ChangeListener l : listeners) {
                    try {
                        l.onStateChanged();
                    } catch (RuntimeException ex) {
                        System.err.println("[populous] listener error: " + ex);
                    }
                }
            }
        });
    }

    // Must be called while holding lock.
    private void rebuildSnapshot() {
        this.snapshot = new Snapshots.SnapState(gs);
    }
}

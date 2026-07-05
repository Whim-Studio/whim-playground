package com.whim.populous.engine;

import java.util.List;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.Views.FollowerView;
import com.whim.populous.api.Views.GameStateView;

/**
 * Headless self-check for the engine — NO GUI, NO Swing. Constructs a fresh
 * game, advances a few hundred ticks synchronously, and asserts the core
 * invariants: walkers exist and move, settlements get founded, the population
 * breeds, and mana accrues over time. Run via {@code main} or the JUnit wrapper
 * method {@link #run()}.
 *
 * This lets Task 2 prove the simulation behaves without waiting on the UI, and
 * gives the orchestrator a fast integration smoke test once the real domain
 * lands.
 */
public final class EngineSmokeTest {

    private EngineSmokeTest() { }

    public static void main(String[] args) {
        try {
            run();
            System.out.println("EngineSmokeTest: PASS");
        } catch (AssertionError err) {
            System.out.println("EngineSmokeTest: FAIL - " + err.getMessage());
            System.exit(1);
        }
    }

    /** Runs the checks; throws {@link AssertionError} on the first failure. */
    public static void run() {
        SimulationEngine engine = new SimulationEngine(1234L);

        GameStateView start = engine.state();
        require(start != null, "state() must never be null");
        require(start.map().cols() > 0 && start.map().rows() > 0, "map must have size");

        int startPop = start.goodPopulation() + start.evilPopulation();
        require(startPop > 0, "a fresh game must spawn followers, saw " + startPop);

        double[] startX = new double[start.followers().size()];
        double[] startY = new double[start.followers().size()];
        List<FollowerView> f0 = start.followers();
        for (int i = 0; i < f0.size(); i++) {
            startX[i] = f0.get(i).x();
            startY[i] = f0.get(i).y();
        }

        int startGoodMana = start.goodMana();

        // Advance the simulation, sampling invariants across the whole run. The
        // game may legitimately end early (one side wiped out by disasters), so
        // we track PEAK values rather than assume everyone survives to tick 400.
        int peakPop = startPop;
        int peakGoodMana = startGoodMana;
        boolean anyMoved = false;
        long lastTick = start.tick();

        for (int t = 0; t < 500; t++) {
            engine.tickOnce();
            GameStateView cur = engine.state();
            lastTick = cur.tick();

            peakPop = Math.max(peakPop, cur.goodPopulation() + cur.evilPopulation());
            peakGoodMana = Math.max(peakGoodMana, cur.goodMana());

            if (!anyMoved) {
                List<FollowerView> fl = cur.followers();
                int lim = Math.min(fl.size(), startX.length);
                for (int i = 0; i < lim; i++) {
                    if (Math.abs(fl.get(i).x() - startX[i]) > 1e-6
                            || Math.abs(fl.get(i).y() - startY[i]) > 1e-6) {
                        anyMoved = true;
                        break;
                    }
                }
            }
            if (cur.gameOver()) {
                break;
            }
        }

        GameStateView later = engine.state();

        // 1. Tick counter advanced.
        require(lastTick > 50, "tick counter should advance, saw " + lastTick);

        // 2. Mana accrued for the player at some point.
        require(peakGoodMana > startGoodMana,
                "good mana should accrue: " + startGoodMana + " -> peak " + peakGoodMana);

        // 3. At least some walkers moved from their start positions.
        require(anyMoved, "at least one follower should have moved");

        // 4. Population should breed upward over the run (settlements + births).
        require(peakPop > startPop,
                "population should breed: start " + startPop + " -> peak " + peakPop);

        // 5. Snapshot isolation: the old snapshot must stay frozen at its tick.
        require(start.tick() < later.tick(),
                "old snapshot must be frozen at its own tick");

        // 6. If the game ended, a real winner must be recorded.
        if (later.gameOver()) {
            require(later.winner() != Allegiance.NEUTRAL,
                    "a finished game must have a non-neutral winner");
        }

        // 7. On a FRESH game: power selection sticks and terraforming responds.
        SimulationEngine fresh = new SimulationEngine(9L);
        fresh.selectPower(GodPower.ARMAGEDDON);
        require(fresh.state().selectedPower() == GodPower.ARMAGEDDON,
                "power selection should stick");

        fresh.selectPower(GodPower.RAISE_LAND);
        int cx = fresh.state().map().cols() / 2;
        int cy = fresh.state().map().rows() / 2;
        int before = fresh.state().map().tileAt(cx, cy).elevation();
        fresh.primaryClick(cx, cy);
        int after = fresh.state().map().tileAt(cx, cy).elevation();
        require(after > before, "raise click should lift terrain: " + before + " -> " + after);

        int lowBefore = fresh.state().map().tileAt(cx, cy).elevation();
        fresh.secondaryClick(cx, cy);
        int lowAfter = fresh.state().map().tileAt(cx, cy).elevation();
        require(lowAfter < lowBefore, "right-click should lower terrain: "
                + lowBefore + " -> " + lowAfter);

        // 8. Clean start/stop of the background loop (real threading path).
        fresh.start();
        sleepQuietly(150);
        long running = fresh.state().tick();
        require(running > 0, "background loop should advance ticks, saw " + running);
        fresh.stop();

        System.out.println("  ranTo tick " + lastTick
                + "  peakPop " + peakPop
                + "  peakGoodMana " + peakGoodMana
                + "  final good/evil " + later.goodPopulation() + "/" + later.evilPopulation()
                + "  over=" + later.gameOver() + " winner=" + later.winner()
                + "  status: " + later.statusLine());
    }

    private static void require(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionError(msg);
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

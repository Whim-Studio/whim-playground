package com.whim.powermonger.engine;

import java.util.List;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Posture;
import com.whim.powermonger.api.Views.CaptainView;
import com.whim.powermonger.api.Views.GameStateView;

/**
 * Headless proof that the engine runs without any UI. Builds a world, issues a few
 * orders (including a subordinate order that flies by pigeon), then advances several
 * hundred ticks printing the Balance-of-Power progression, season/weather, and a
 * tail of the event log.
 *
 * Run: {@code java com.whim.powermonger.engine.EngineSelfCheck [seed] [ticks]}
 */
public final class EngineSelfCheck {

    private EngineSelfCheck() {}

    public static void main(String[] args) {
        long seed = args.length > 0 ? parseLong(args[0], 42L) : 42L;
        int ticks = args.length > 1 ? (int) parseLong(args[1], 600L) : 600;

        GameEngine engine = new GameEngine(seed);
        GameStateView s0 = engine.state();

        System.out.println("=== Powermonger Engine Self-Check ===");
        System.out.println("seed=" + seed + "  map=" + s0.mapWidth() + "x" + s0.mapHeight()
                + "  maxElev=" + s0.maxElevation());
        System.out.println("towns=" + s0.towns().size()
                + "  townspeople=" + s0.townspeople().size()
                + "  captains=" + s0.captains().size());
        printCaptains(s0);
        System.out.println("initial balance=" + fmt(s0.balanceOfPower())
                + "  season=" + s0.season() + "  weather=" + s0.weather());
        System.out.println();

        // Aggressive stance + march every player captain at the nearest enemy.
        for (int i = 0; i < s0.captains().size(); i++) {
            CaptainView c = s0.captains().get(i);
            if (c.allegiance() == Allegiance.PLAYER) {
                engine.setPosture(c.id(), Posture.AGGRESSIVE);
            }
        }
        orderFightNearestEnemy(engine);

        System.out.println("--- running " + ticks + " ticks (stepOnce) ---");
        System.out.printf("%6s %8s %8s %8s %8s %6s %6s%n",
                "tick", "balance", "season", "weather", "moveF", "P.cap", "E.cap");
        for (int t = 1; t <= ticks; t++) {
            engine.stepOnce();
            if (t % 100 == 0) {
                orderFightNearestEnemy(engine); // keep the pressure on
            }
            if (t % 50 == 0 || t == 1) {
                printRow(engine.state());
            }
            if (engine.state().gameOver()) {
                printRow(engine.state());
                break;
            }
        }
        System.out.println();

        GameStateView end = engine.state();
        System.out.println("final balance=" + fmt(end.balanceOfPower())
                + "  gameOver=" + end.gameOver() + "  playerWon=" + end.playerWon());
        System.out.println("status: " + end.statusMessage());
        System.out.println();

        System.out.println("--- event log (tail) ---");
        List<String> log = engine.recentEvents(25);
        for (int i = 0; i < log.size(); i++) {
            System.out.println("  " + log.get(i));
        }
        System.out.println();

        // Prove the real background daemon thread also runs the loop (fresh world so
        // it is not already game-over).
        System.out.println("--- background thread check ---");
        GameEngine live = new GameEngine(seed + 1);
        long before = live.state().tickCount();
        live.start();
        sleep(300);
        live.stop();
        long after = live.state().tickCount();
        System.out.println("daemon loop advanced tickCount " + before + " -> " + after
                + " over ~300ms (running=" + live.isRunning() + ")");
        System.out.println("=== self-check complete ===");
    }

    private static void orderFightNearestEnemy(GameEngine engine) {
        GameStateView s = engine.state();
        for (int i = 0; i < s.captains().size(); i++) {
            CaptainView c = s.captains().get(i);
            if (c.allegiance() != Allegiance.PLAYER || !c.alive()) {
                continue;
            }
            CaptainView enemy = nearestEnemy(s, c);
            if (enemy != null) {
                engine.issueOrder(c.id(), CommandType.FIGHT,
                        (int) Math.round(enemy.x()), (int) Math.round(enemy.y()));
            }
        }
    }

    private static CaptainView nearestEnemy(GameStateView s, CaptainView from) {
        CaptainView best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < s.captains().size(); i++) {
            CaptainView c = s.captains().get(i);
            if (c.allegiance() == Allegiance.ENEMY && c.alive()) {
                double dx = c.x() - from.x();
                double dy = c.y() - from.y();
                double d = dx * dx + dy * dy;
                if (d < bestD) {
                    bestD = d;
                    best = c;
                }
            }
        }
        return best;
    }

    private static void printCaptains(GameStateView s) {
        for (int i = 0; i < s.captains().size(); i++) {
            CaptainView c = s.captains().get(i);
            System.out.printf("  #%d %-8s %-7s pos=(%.1f,%.1f) str=%d food=%d%s%n",
                    c.id(), c.name(), c.allegiance(), c.x(), c.y(), c.strength(), c.food(),
                    c.supremeCommander() ? " [SUPREME]" : "");
        }
    }

    private static void printRow(GameStateView s) {
        int pcap = 0;
        int ecap = 0;
        for (int i = 0; i < s.captains().size(); i++) {
            CaptainView c = s.captains().get(i);
            if (!c.alive()) {
                continue;
            }
            if (c.allegiance() == Allegiance.PLAYER) {
                pcap++;
            } else if (c.allegiance() == Allegiance.ENEMY) {
                ecap++;
            }
        }
        System.out.printf("%6d %8s %8s %8s %8s %6d %6d%n",
                s.tickCount(), fmt(s.balanceOfPower()), s.season(), s.weather(),
                fmt(s.movementFactor()), pcap, ecap);
    }

    private static String fmt(double v) {
        return String.format("%+.3f", v);
    }

    private static long parseLong(String s, long def) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

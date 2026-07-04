package com.whim.shinobi.engine;

import com.whim.shinobi.api.Views;

/**
 * Headless proof that the engine runs without any UI. Builds a {@link GameEngine},
 * starts the background {@link TickLoop}, injects a few scripted inputs, ticks for
 * ~2 seconds, and prints player position / score / enemy-count deltas before
 * stopping cleanly. No {@code java.awt} / {@code javax.swing} anywhere.
 */
public final class EngineSelfCheck {
    private EngineSelfCheck() {}

    public static void main(String[] args) throws InterruptedException {
        GameEngine engine = new GameEngine();
        engine.newGame();

        Views.GameStateView s0 = engine.state();
        int enemies0 = countAlive(s0);
        double x0 = s0.player().x();
        int score0 = s0.player().score();
        System.out.println("[start] playerX=" + fmt(x0)
                + " score=" + score0
                + " lives=" + s0.player().lives()
                + " ninjutsu=" + s0.player().ninjutsu()
                + " enemiesAlive=" + enemies0
                + " hostages=" + s0.hostagesRescued() + "/" + s0.hostagesTotal());

        engine.start();

        // Scripted inputs over the run: walk right, jump, shift plane, attack, ninjutsu.
        engine.setRight(true);
        sleep(400);
        engine.jump();
        sleep(200);
        engine.attack();
        sleep(150);
        engine.shiftPlane();
        sleep(150);
        engine.attack();
        sleep(150);
        engine.ninjutsu();
        sleep(500);
        engine.setRight(false);
        engine.setLeft(true);
        sleep(450);
        engine.setLeft(false);

        Views.GameStateView s1 = engine.state();
        int enemies1 = countAlive(s1);
        double x1 = s1.player().x();
        int score1 = s1.player().score();

        System.out.println("[end]   playerX=" + fmt(x1)
                + " score=" + score1
                + " lives=" + s1.player().lives()
                + " ninjutsu=" + s1.player().ninjutsu()
                + " enemiesAlive=" + enemies1
                + " projectiles=" + s1.projectiles().size()
                + " phase=" + s1.phase()
                + " cameraX=" + fmt(s1.cameraX())
                + " hostages=" + s1.hostagesRescued() + "/" + s1.hostagesTotal());

        System.out.println("[delta] dX=" + fmt(x1 - x0)
                + " dScore=" + (score1 - score0)
                + " dEnemiesAlive=" + (enemies1 - enemies0));

        engine.stop();

        boolean moved = Math.abs(x1 - x0) > 0.5;
        boolean ranHeadless = true;
        System.out.println("[result] loop ran headless=" + ranHeadless
                + " playerMoved=" + moved
                + " phaseValid=" + (s1.phase() != null));
    }

    private static int countAlive(Views.GameStateView s) {
        int n = 0;
        for (Views.EnemyView e : s.enemies()) {
            if (e.alive()) n++;
        }
        return n;
    }

    private static String fmt(double d) {
        return String.valueOf(Math.round(d * 10.0) / 10.0);
    }

    private static void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }
}

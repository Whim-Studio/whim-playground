package com.whim.shinobi.ui;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * Dev entry point: launches a {@link GameFrame} wired to the {@link StubController}
 * so the rendering, camera scroll, HUD, and input can be seen and played before the
 * real engine lands. In a headless environment it instead constructs the controller,
 * runs a few simulated ticks, and prints a sanity summary so CI can prove it wires up.
 *
 * Controls: A/D or arrows move, S/Down crouch, Space/W/Up jump, L/Shift plane-shift,
 * J/Z attack, K/X ninjutsu, P pause, Enter new game.
 */
public final class UiPreview {

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            runHeadlessSanity();
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                GameFrame frame = new GameFrame(new StubController());
                frame.launch();
            }
        });
    }

    /** No display: prove the controller + snapshot pipeline works without a window. */
    private static void runHeadlessSanity() {
        StubController c = new StubController();
        c.newGame();
        c.setRight(true);
        for (int i = 0; i < 120; i++) {
            // manually advance without the background thread to stay deterministic
            if (i == 20) c.attack();
            if (i == 40) c.jump();
            if (i == 60) c.shiftPlane();
            if (i == 80) c.ninjutsu();
            c.tickForTest();
        }
        com.whim.shinobi.api.Views.GameStateView s = c.state();
        System.out.println("[UiPreview headless] player.x=" + Math.round(s.player().x())
                + " plane=" + s.player().plane()
                + " cameraX=" + Math.round(s.cameraX())
                + " enemies=" + s.enemies().size()
                + " hostages=" + s.hostages().size()
                + " platforms=" + s.platforms().size()
                + " score=" + s.player().score()
                + " secs=" + s.secondsRemaining());
        System.out.println("[UiPreview headless] frame content = "
                + GameFrame.contentWidth() + "x" + GameFrame.contentHeight());
        System.out.println("[UiPreview headless] OK");
    }
}

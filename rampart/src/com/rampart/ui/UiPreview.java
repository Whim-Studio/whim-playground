package com.rampart.ui;

import com.rampart.engine.GameApi;
import com.rampart.model.GameStateView;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * Dev entry point for the Rampart UI. Launches a {@link GameFrame} wired to the
 * {@link StubGameApi} so the rendering, HUD, ghost preview, input, and CardLayout
 * screen transitions can be seen and played before the real engine lands. The
 * production entry point ({@code com.rampart.app.Main}) is written by the
 * orchestrator and wires the real {@code GameEngine} instead.
 *
 * <p>Run with the {@code --check} argument (or in a headless environment) to skip
 * the window and instead prove the stub&harr;view pipeline works: it builds the
 * stub, drives a few ticks through the phase loop, and prints a sanity summary.
 *
 * <p>Controls: BUILD — click enclosed land to place a cannon. BATTLE — click a
 * target to fire. REPAIR — move to aim the ghost piece, left-click to drop,
 * right-click or {@code R} to rotate. SPACE/ENTER ends the current phase early;
 * ENTER on the title/game-over screens starts a round / new game.
 */
public final class UiPreview {

    public static void main(String[] args) {
        boolean check = GraphicsEnvironment.isHeadless() || hasFlag(args, "--check");
        if (check) {
            runHeadlessSanity();
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                GameFrame frame = new GameFrame(new StubGameApi());
                frame.launch();
            }
        });
    }

    private static boolean hasFlag(String[] args, String flag) {
        if (args == null) return false;
        for (int i = 0; i < args.length; i++) {
            if (flag.equals(args[i])) return true;
        }
        return false;
    }

    /** No display: prove the stub + snapshot pipeline works without a window. */
    private static void runHeadlessSanity() {
        GameApi api = new StubGameApi();
        api.newGame();
        GameStateView s = api.state();
        if (s == null) {
            throw new IllegalStateException("StubGameApi.state() returned null");
        }
        System.out.println("[UiPreview] initial phase=" + s.phase()
                + " round=" + s.round()
                + " grid=" + s.grid().cols() + "x" + s.grid().rows()
                + " castles=" + s.castles().size());

        // Drive one full phase loop: TITLE -> BUILD -> BATTLE -> REPAIR -> ROUND.
        api.startRound();
        System.out.println("[UiPreview] after startRound phase=" + api.state().phase()
                + " cannonsToPlace=" + api.state().cannonsRemainingToPlace());
        api.placeCannon(11, 10);
        for (int i = 0; i < 4; i++) {
            api.endPhaseEarly();
            api.tick(50);
            System.out.println("[UiPreview] tick " + i + " -> phase="
                    + api.state().phase()
                    + " ships=" + api.state().ships().size()
                    + " currentPiece="
                    + (api.state().currentPiece() != null
                        ? api.state().currentPiece().shape() : "none"));
        }
        System.out.println("[UiPreview] frame content = "
                + GameFrame.contentWidth() + "x" + GameFrame.contentHeight()
                + "  cell=" + GamePanel.CELL);
        System.out.println("[UiPreview] OK");
    }
}

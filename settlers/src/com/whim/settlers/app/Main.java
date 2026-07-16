package com.whim.settlers.app;

import com.whim.settlers.engine.Game;
import com.whim.settlers.engine.GameLoop;
import com.whim.settlers.engine.InputHandler;
import com.whim.settlers.ui.BuildMenu;
import com.whim.settlers.ui.EconomyPanel;
import com.whim.settlers.ui.MetaScreen;
import com.whim.settlers.ui.MilitaryPanel;
import com.whim.settlers.ui.Minimap;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;

/**
 * Entry point. Opens the desktop window and starts the game loop. On a headless
 * machine (no display) it runs a short console self-test instead, so the build
 * can be exercised in CI or a container without a screen.
 */
public final class Main {

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[settlers] Headless environment — running self-test.");
            com.whim.settlers.engine.SelfTest.run();
            return;
        }
        final Game game = new Game();
        applyArgs(game, args); // pre-seed the setup screen from the command line
        SwingUtilities.invokeLater(() -> launch(game));
    }

    /**
     * Optional command-line pre-configuration of the new-game setup:
     * <ul>
     *   <li>{@code --map} selects the hand-built tutorial valley,</li>
     *   <li>{@code --seed <n>} selects a generated map from that seed.</li>
     * </ul>
     * The game still opens on the main menu; these just prime the setup screen.
     */
    private static void applyArgs(Game game, String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                if ("--map".equals(args[i])) {
                    game.config().setTutorialMap(true);
                } else if ("--seed".equals(args[i]) && i + 1 < args.length) {
                    long seed = Long.parseLong(args[i + 1]);
                    game.config().setTutorialMap(false);
                    game.config().bumpSeed((int) (seed - game.config().seed()));
                }
            }
        } catch (Exception e) {
            System.err.println("[settlers] ignoring bad args (" + e.getMessage() + ").");
        }
    }

    private static void launch(Game game) {
        MetaScreen meta = new MetaScreen();
        Minimap minimap = new Minimap();
        BuildMenu buildMenu = new BuildMenu();
        EconomyPanel economyPanel = new EconomyPanel();
        MilitaryPanel militaryPanel = new MilitaryPanel();
        InputHandler input = new InputHandler(game, meta, minimap, buildMenu, economyPanel, militaryPanel);

        Canvas canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(1024, 720));
        canvas.setFocusable(true);
        canvas.addKeyListener(input);
        canvas.addMouseListener(input);
        canvas.addMouseMotionListener(input);
        canvas.addMouseWheelListener(input);
        canvas.setIgnoreRepaint(true); // we drive rendering ourselves

        JFrame frame = new JFrame("The Settlers — Java 8 / Swing recreation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        canvas.requestFocus();

        GameLoop loop = new GameLoop(canvas, game, input, meta, minimap, buildMenu,
                economyPanel, militaryPanel);
        loop.start();
    }

    private Main() { }
}

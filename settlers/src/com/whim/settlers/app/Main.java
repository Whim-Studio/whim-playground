package com.whim.settlers.app;

import com.whim.settlers.engine.GameLoop;
import com.whim.settlers.engine.InputHandler;
import com.whim.settlers.engine.World;
import com.whim.settlers.io.MapLoader;
import com.whim.settlers.map.MapGenerator;
import com.whim.settlers.map.TileMap;
import com.whim.settlers.ui.BuildMenu;
import com.whim.settlers.ui.EconomyPanel;
import com.whim.settlers.ui.MilitaryPanel;
import com.whim.settlers.ui.Minimap;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.nio.file.Paths;

/**
 * Entry point. Opens the desktop window and starts the game loop. On a headless
 * machine (no display) it runs a short console self-test instead, so the build
 * can be exercised in CI or a container without a screen.
 */
public final class Main {

    private static final int MAP_W = 80;
    private static final int MAP_H = 80;
    private static final long DEFAULT_SEED = 1993L;

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[settlers] Headless environment — running self-test.");
            com.whim.settlers.engine.SelfTest.run();
            return;
        }
        final TileMap map = buildMap(args);
        SwingUtilities.invokeLater(() -> launch(map));
    }

    /**
     * Choose the starting map from the command line:
     * <ul>
     *   <li>{@code --map <file>} loads a hand-built text map,</li>
     *   <li>{@code --seed <n>} generates a map from that seed,</li>
     *   <li>otherwise a generated map from the default seed.</li>
     * </ul>
     */
    private static TileMap buildMap(String[] args) {
        try {
            for (int i = 0; i < args.length - 1; i++) {
                if ("--map".equals(args[i])) {
                    return MapLoader.fromFile(Paths.get(args[i + 1]));
                }
                if ("--seed".equals(args[i])) {
                    return MapGenerator.generate(MAP_W, MAP_H, Long.parseLong(args[i + 1]));
                }
            }
        } catch (Exception e) {
            System.err.println("[settlers] map load failed (" + e.getMessage()
                    + "); using generated map.");
        }
        return MapGenerator.generate(MAP_W, MAP_H, DEFAULT_SEED);
    }

    private static void launch(TileMap map) {
        World world = new World(map);
        world.foundSettlement(); // place the human Castle to start the game
        world.spawnEnemy();      // place a static enemy settlement (AI arrives in Phase 6)
        Minimap minimap = new Minimap();
        BuildMenu buildMenu = new BuildMenu();
        EconomyPanel economyPanel = new EconomyPanel();
        MilitaryPanel militaryPanel = new MilitaryPanel();
        InputHandler input = new InputHandler(world, minimap, buildMenu, economyPanel, militaryPanel);

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

        GameLoop loop = new GameLoop(canvas, world, input, minimap, buildMenu,
                economyPanel, militaryPanel);
        loop.start();
    }

    private Main() { }
}

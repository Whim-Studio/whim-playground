package com.whim.settlers.app;

import com.whim.settlers.engine.GameLoop;
import com.whim.settlers.engine.InputHandler;
import com.whim.settlers.engine.World;
import com.whim.settlers.map.TileMap;

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

    private static final int MAP_W = 80;
    private static final int MAP_H = 80;

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[settlers] Headless environment — running self-test.");
            com.whim.settlers.engine.SelfTest.run();
            return;
        }
        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        TileMap map = TileMap.flat(MAP_W, MAP_H, 1993L);
        World world = new World(map);
        InputHandler input = new InputHandler(world.camera());

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

        GameLoop loop = new GameLoop(canvas, world, input);
        loop.start();
    }

    private Main() { }
}

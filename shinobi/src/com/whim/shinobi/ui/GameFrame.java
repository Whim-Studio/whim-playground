package com.whim.shinobi.ui;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.GameController;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;

/**
 * Top-level window: sized to the viewport plus HUD bars, non-resizable, hosting a
 * single {@link GamePanel}. Construct with any {@link GameController} (the stub for
 * dev, the real engine for {@code app.Main}) then call {@link #launch()}.
 */
public final class GameFrame extends JFrame {

    private final GamePanel panel;
    private final GameController controller;

    public GameFrame(GameController controller) {
        super("SHINOBI (1987) — Java 8 Swing");
        this.controller = controller;
        this.panel = new GamePanel(controller);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    /** Prepare the game, start the engine, and begin the repaint loop. */
    public void launch() {
        controller.newGame();
        controller.start();
        setVisible(true);
        panel.startRenderLoop();
    }

    /** Stop the render loop and engine. */
    public void shutdown() {
        panel.stopRenderLoop();
        controller.stop();
    }

    public GamePanel panel() { return panel; }

    /** Sanity: window content dimensions the frame targets. */
    public static int contentWidth()  { return Config.VIEW_W; }
    public static int contentHeight() { return Hud.TOTAL_H; }
}

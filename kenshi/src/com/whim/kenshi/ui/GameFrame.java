package com.whim.kenshi.ui;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.GameController;
import com.whim.kenshi.api.Views;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

/**
 * Top-level window that wires the whole UI to a {@link GameController}. Layout:
 * a {@link Hud} top bar (NORTH), the {@link WorldPanel} viewport (CENTER) and a
 * bottom strip (SOUTH) holding the {@link BodyChart} (west), squad portrait
 * chips (centre) and the event log (east). One Swing timer inside the
 * {@code WorldPanel} polls {@code state()} each frame and fans the snapshot out
 * to every panel via a {@link WorldPanel.FrameSink}.
 *
 * This class is engine-agnostic: it works against the real engine or the
 * dev-only {@link StubController} without change.
 */
public final class GameFrame extends JFrame {

    private final GameController controller;
    private final Camera camera;
    private final WorldPanel worldPanel;
    private final BodyChart bodyChart;
    private final Hud hud;
    private final InputHandler input;

    public GameFrame(GameController controller) {
        super("Kenshi — demake");
        this.controller = controller;
        this.camera = new Camera();
        this.bodyChart = new BodyChart();
        this.hud = new Hud(controller);
        this.worldPanel = new WorldPanel(controller, camera, new Renderer());
        this.input = new InputHandler(worldPanel, camera, controller);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildLayout();

        // Fan the once-per-frame snapshot out to the HUD + body chart.
        worldPanel.setFrameSink(new WorldPanel.FrameSink() {
            public void onFrame(Views.GameStateView s) {
                bodyChart.setState(s);
                hud.setState(s);
            }
        });

        centerCameraOnPlayer();
        input.install();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout());

        root.add(hud.topBar(), BorderLayout.NORTH);
        root.add(worldPanel, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(Palette.HUD_BG);
        south.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Palette.HUD_BORDER));

        JPanel bc = bodyChart;
        bc.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, Palette.HUD_BORDER));
        south.add(bc, BorderLayout.WEST);
        south.add(hud.portraits(), BorderLayout.CENTER);

        JPanel logWrap = hud.eventLog();
        logWrap.setPreferredSize(new Dimension(320, 100));
        south.add(logWrap, BorderLayout.EAST);

        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
    }

    /** Point the camera at the squad's centroid so units are on screen at launch. */
    private void centerCameraOnPlayer() {
        Views.GameStateView s = controller.state();
        if (s == null) { camera.centerOn(Config.WORLD_SIZE / 2, Config.WORLD_SIZE / 2); return; }
        double sx = 0, sy = 0;
        int n = 0;
        List<Views.CharacterView> chars = s.characters();
        for (int i = 0; i < chars.size(); i++) {
            if (chars.get(i).playerControlled()) { sx += chars.get(i).x(); sy += chars.get(i).y(); n++; }
        }
        if (n > 0) camera.centerOn(sx / n, sy / n);
        else camera.centerOn(Config.WORLD_SIZE / 2, Config.WORLD_SIZE / 2);
    }

    /** Start the controller (idempotent) and begin the repaint timer. */
    public void launch() {
        controller.start();
        worldPanel.startAnimating();
        setVisible(true);
        worldPanel.requestFocusInWindow();
    }

    /** Stop the timer and controller. */
    public void shutdown() {
        worldPanel.stopAnimating();
        controller.stop();
    }

    /** Convenience: build + show a frame on the EDT for the given controller. */
    public static void show(final GameController controller) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GameFrame f = new GameFrame(controller);
                f.launch();
            }
        });
    }
}

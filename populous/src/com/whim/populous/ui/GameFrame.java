package com.whim.populous.ui;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.GameController;

/**
 * Top-level window: assembles a {@link MapPanel} (centre) and a
 * {@link DashboardPanel} (bottom) around a single injected {@link GameController}.
 * Wires a {@link GameController.ChangeListener} that marshals repaints onto the
 * EDT, and provides a small menu / key bindings for a new game and start/stop.
 *
 * The UI depends ONLY on {@code com.whim.populous.api}. In production the app
 * module injects the engine's controller; for standalone dev runs the
 * {@link StubController} fakes one (see {@link #main}).
 */
public class GameFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final GameController controller;
    private final MapPanel mapPanel;
    private final DashboardPanel dashboard;
    private final GameController.ChangeListener listener;

    public GameFrame(GameController controller) {
        super("Populous — Whim (Java 8 Swing)");
        this.controller = controller;
        this.mapPanel = new MapPanel(controller);
        this.dashboard = new DashboardPanel(controller);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(mapPanel, BorderLayout.CENTER);
        add(dashboard, BorderLayout.SOUTH);
        setJMenuBar(buildMenu());
        installKeys();
        pack();
        setLocationRelativeTo(null);

        // Repaint whenever the engine reports a state change. The listener fires
        // off the EDT (sim thread); we hop back onto the EDT to repaint.
        this.listener = new GameController.ChangeListener() {
            @Override
            public void onStateChanged() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        mapPanel.repaint();
                        dashboard.repaint();
                    }
                });
            }
        };
        controller.addChangeListener(listener);
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu game = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New Game");
        newGame.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        newGame.addActionListener(e -> {
            controller.newGame(System.nanoTime());
            repaintAll();
        });
        JMenuItem start = new JMenuItem("Start");
        start.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        start.addActionListener(e -> controller.start());
        JMenuItem stop = new JMenuItem("Pause");
        stop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
        stop.addActionListener(e -> controller.stop());
        JMenuItem step = new JMenuItem("Step Once");
        step.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, KeyEvent.CTRL_DOWN_MASK));
        step.addActionListener(e -> { controller.tickOnce(); repaintAll(); });
        JMenuItem quit = new JMenuItem("Quit");
        quit.addActionListener(e -> dispose());
        game.add(newGame);
        game.addSeparator();
        game.add(start);
        game.add(stop);
        game.add(step);
        game.addSeparator();
        game.add(quit);

        JMenu powers = new JMenu("Powers");
        GodPower[] all = GodPower.values();
        for (int i = 0; i < all.length; i++) {
            final GodPower p = all[i];
            JMenuItem mi = new JMenuItem(p.label()
                    + (p.manaCost() > 0 ? "  (" + p.manaCost() + ")" : ""));
            mi.addActionListener(e -> {
                if (p.targeted()) {
                    controller.selectPower(p);
                } else {
                    controller.castGlobal(p);
                }
                repaintAll();
            });
            powers.add(mi);
        }

        bar.add(game);
        bar.add(powers);
        return bar;
    }

    private void installKeys() {
        // Number keys 1..8 arm / cast the corresponding power for quick play.
        GodPower[] all = GodPower.values();
        javax.swing.JComponent root = getRootPane();
        for (int i = 0; i < all.length && i < 9; i++) {
            final GodPower p = all[i];
            String name = "power" + i;
            root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke((char) ('1' + i)), name);
            root.getActionMap().put(name, new javax.swing.AbstractAction() {
                private static final long serialVersionUID = 1L;
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (p.targeted()) {
                        controller.selectPower(p);
                    } else {
                        controller.castGlobal(p);
                    }
                    repaintAll();
                }
            });
        }
    }

    private void repaintAll() {
        mapPanel.repaint();
        dashboard.repaint();
    }

    @Override
    public void dispose() {
        controller.removeChangeListener(listener);
        controller.stop();
        super.dispose();
    }

    /** Standalone dev launch against the {@link StubController}. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StubController controller = new StubController();
                controller.newGame(1234L);
                GameFrame frame = new GameFrame(controller);
                frame.setVisible(true);
                controller.start();
            }
        });
    }
}

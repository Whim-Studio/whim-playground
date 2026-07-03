package com.whim.powermonger.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.whim.powermonger.api.GameController;

/**
 * Top-level window with the classic Powermonger framing: a clickable minimap on
 * the LEFT, the large 2.5D pseudo-isometric {@link MapPanel} in the CENTER, the
 * {@link ConsolePanel} on the RIGHT, and the {@link BalancePanel} along the
 * BOTTOM. A ~30 fps {@link Timer} re-reads {@code controller.state()} and repaints.
 */
public final class GameFrame extends JFrame {

    private final GameController controller;
    private final MapPanel mapPanel;
    private final MiniMapPanel miniMap;
    private final ConsolePanel console;
    private final BalancePanel balance;
    private final Timer timer;

    public GameFrame(GameController controller) {
        super("Powermonger");
        this.controller = controller;

        this.mapPanel = new MapPanel(controller);
        this.miniMap = new MiniMapPanel(controller, mapPanel);
        this.console = new ConsolePanel(controller);
        this.balance = new BalancePanel(controller);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(4, 4));
        getContentPane().setBackground(UiPalette.PANEL_BG_DARK);

        // Left rail: minimap on top, a thin legend below.
        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setBackground(UiPalette.PANEL_BG_DARK);
        left.setPreferredSize(new Dimension(190, 100));
        left.add(titleStrip("MAP"), BorderLayout.NORTH);
        left.add(miniMap, BorderLayout.CENTER);

        // Right rail: command console.
        JPanel right = new JPanel(new BorderLayout(0, 4));
        right.setBackground(UiPalette.PANEL_BG_DARK);
        right.setPreferredSize(new Dimension(220, 100));
        right.add(titleStrip("COMMAND"), BorderLayout.NORTH);
        right.add(console, BorderLayout.CENTER);

        add(left, BorderLayout.WEST);
        add(mapPanel, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        add(balance, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1040, 720));
        pack();
        setLocationRelativeTo(null);

        // ~30 fps refresh.
        this.timer = new Timer(1000 / 30, e -> {
            mapPanel.tickAnimation();
            mapPanel.repaint();
            miniMap.repaint();
            console.repaint();
            balance.repaint();
        });
    }

    private JPanel titleStrip(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UiPalette.PANEL_BG);
        javax.swing.JLabel l = new javax.swing.JLabel("  " + text);
        l.setForeground(UiPalette.HILIGHT);
        l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        l.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 2, 3, 2));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    /** Show the window and start the render loop + simulation. */
    public void launch() {
        controller.start();
        setVisible(true);
        timer.start();
    }

    /** Stop the render timer and the underlying simulation. */
    public void shutdown() {
        timer.stop();
        controller.stop();
    }

    public MapPanel mapPanel() { return mapPanel; }
}

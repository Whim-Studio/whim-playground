package com.whim.nobunaga.ui;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Top-level game window. {@link BorderLayout} with the {@link MapPanel} in the
 * CENTER, the {@link DashboardPanel} on the EAST, the {@link ActionPanel} on the
 * SOUTH, and a header label (driven by {@code GameLoopManager.seasonHeader}) on
 * the NORTH.
 *
 * <p>It wires the map's selection back into the controller and exposes
 * {@link #refresh()} so panels can repaint the whole frame after an action.
 */
public final class GameFrame extends JFrame {

    private final GameController controller;
    private final MapPanel mapPanel;
    private final DashboardPanel dashboard;
    private final JLabel header;

    public GameFrame(GameController controller) {
        super("Nobunaga's Ambition: Zenkokuban");
        this.controller = controller;

        this.mapPanel = new MapPanel(controller.state());
        this.dashboard = new DashboardPanel(controller);
        ActionPanel actions = new ActionPanel(controller, this);

        header = new JLabel();
        header.setFont(new Font("Monospaced", Font.BOLD, 18));
        header.setForeground(new Color(235, 225, 200));
        header.setBackground(new Color(28, 30, 36));
        header.setOpaque(true);
        header.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        mapPanel.setSelectionListener(new MapPanel.SelectionListener() {
            public void provinceSelected(int provinceId) {
                GameFrame.this.controller.setSelected(provinceId);
                dashboard.refresh();
            }
        });

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        add(dashboard, BorderLayout.EAST);
        add(actions, BorderLayout.SOUTH);

        updateHeader();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    /** Repaints the map, refreshes the dashboard, and updates the header. */
    public void refresh() {
        mapPanel.setSelected(controller.selected());
        mapPanel.repaint();
        dashboard.refresh();
        updateHeader();
    }

    private void updateHeader() {
        String name = controller.state().player().getName();
        header.setText(controller.header() + "    —    Lord " + name);
    }
}

package com.whim.ruinlander.ui;

import com.whim.ruinlander.domain.GameStateManager;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;

/** Top-level window. Assembles the dashboard around the central map canvas. */
public class GameFrame extends JFrame {

    public GameFrame(GameStateManager gsm) {
        super("Ruinlander — Wasteland Survival");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GameController controller = new GameController(gsm);
        MapPanel map = new MapPanel(controller);
        StatusPanel status = new StatusPanel(gsm);
        LogPanel log = new LogPanel();
        InventoryPanel inv = new InventoryPanel(controller);
        controller.wire(map, status, log, inv);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(18, 16, 14));
        root.add(map, BorderLayout.CENTER);
        root.add(status, BorderLayout.EAST);
        root.add(inv, BorderLayout.WEST);
        root.add(log, BorderLayout.SOUTH);
        setContentPane(root);

        // Keyboard input is owned by the controller.
        addKeyListener(controller);
        map.addKeyListener(controller);
        setFocusable(true);

        pack();
        setLocationRelativeTo(null);
    }
}

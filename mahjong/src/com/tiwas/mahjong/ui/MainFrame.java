package com.tiwas.mahjong.ui;

import java.awt.Dimension;

import javax.swing.JFrame;

import com.tiwas.mahjong.engine.GameEngine;

/** The top-level window. Hosts the {@link GamePanel} and starts the first game. */
public final class MainFrame extends JFrame {

    private final GamePanel gamePanel;

    public MainFrame(GameEngine engine) {
        super("Tiwa's Mah Jong — Demo Version");
        this.gamePanel = new GamePanel(engine);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(gamePanel);
        setMinimumSize(new Dimension(1024, 720));
        pack();
        setLocationRelativeTo(null);
    }

    /** Show the window and deal the first hand. */
    public void launch() {
        setVisible(true);
        gamePanel.startGame();
    }
}

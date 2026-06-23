package com.tiwas.mahjong.app;

import java.util.Random;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.tiwas.mahjong.engine.GameEngine;
import com.tiwas.mahjong.ui.MainFrame;

/**
 * Entry point for Tiwa's Mah Jong — Demo Version. Builds the engine and launches
 * the Swing UI for a fresh single-player game (1 human vs 3 AI).
 */
public final class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // fall back to the default look and feel
                }
                GameEngine engine = new GameEngine(new Random());
                new MainFrame(engine).launch();
            }
        });
    }
}

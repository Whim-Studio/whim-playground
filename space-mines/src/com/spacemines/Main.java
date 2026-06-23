package com.spacemines;

import java.util.Random;

import javax.swing.SwingUtilities;

/**
 * Entry point: builds the initial game state, wires up the engine, and shows
 * the Swing UI on the Event Dispatch Thread.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ColonyState state = GameConstants.newGame();
                GameEngine engine = new GameEngine(state, new Random());
                new SpaceMinesUI(engine).setVisible(true);
            }
        });
    }
}

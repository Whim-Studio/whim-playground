package com.whim.necromunda.app;

import javax.swing.SwingUtilities;

import com.whim.necromunda.engine.GameState;
import com.whim.necromunda.engine.TurnManager;
import com.whim.necromunda.engine.setup.DemoSetup;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.board.Board;
import com.whim.necromunda.ui.MainWindow;

/**
 * Entry point. Builds the demo battle (board + two gangs), wires the turn engine,
 * and launches the Swing window on the EDT. Both gangs are human (hotseat);
 * per-fighter actions arrive in Milestone 5.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        final Board board = DemoSetup.demoBoard();
        final Gang gangA = DemoSetup.gangA();
        final Gang gangB = DemoSetup.gangB();
        DemoSetup.placeGangs(board, gangA, gangB);

        final long seed = 20260708L;
        final GameState state = new GameState(board, gangA, gangB, seed);
        final TurnManager turns = new TurnManager(state);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainWindow window = new MainWindow(state, turns);
                window.setVisible(true);
                // Kick off the first turn once the UI is listening.
                turns.startBattle(0);
            }
        });
    }
}

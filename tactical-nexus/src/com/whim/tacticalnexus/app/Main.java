package com.whim.tacticalnexus.app;

import com.whim.tacticalnexus.data.FloorFactory;
import com.whim.tacticalnexus.state.GameState;
import com.whim.tacticalnexus.state.StateManager;
import com.whim.tacticalnexus.ui.GameFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for Tactical Nexus.
 *
 * <p>Wires the three packages together: {@link FloorFactory} (Task 1) builds the
 * initial {@link GameState}, a {@link StateManager} (Task 1) wraps it for
 * undo/redo, and the Swing {@link GameFrame} (Task 3) renders it and drives the
 * pure engine. All UI construction happens on the EDT.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the default look and feel.
                }
                GameState initial = FloorFactory.initialState();
                StateManager stateManager = new StateManager(initial);
                new GameFrame(stateManager).setVisible(true);
            }
        });
    }
}

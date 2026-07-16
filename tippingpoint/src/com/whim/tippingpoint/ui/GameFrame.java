package com.whim.tippingpoint.ui;

import com.whim.tippingpoint.engine.DefaultGameEngine;
import com.whim.tippingpoint.engine.GameEngine;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Top-level window for the Tipping Point Swing adaptation.
 *
 * <p>Entry point: {@link #run()} builds and shows the frame on the EDT. A small
 * modal new-game dialog collects player count/names/human-or-AI/mode, then calls
 * {@link DefaultGameEngine#newGame} to build the {@link GameEngine} this frame drives.
 *
 * <p>The UI reads only from {@code domain} objects and mutates state exclusively
 * through the {@link GameEngine} interface.
 */
public final class GameFrame extends JFrame {

    private final BoardPanel board;

    public GameFrame(GameEngine engine) {
        super("Tipping Point");
        this.board = new BoardPanel(engine);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(board, BorderLayout.CENTER);
        setMinimumSize(new Dimension(1024, 720));
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Orchestrator entry point. Shows the new-game dialog and, if confirmed,
     * constructs the engine and displays the board. All work happens on the EDT.
     */
    public static void run() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                NewGameDialog dialog = new NewGameDialog(null);
                dialog.setVisible(true);
                if (!dialog.isConfirmed()) {
                    return; // user cancelled
                }
                GameEngine engine = DefaultGameEngine.newGame(
                        dialog.getNames(),
                        dialog.getAiFlags(),
                        dialog.getMode(),
                        dialog.getSeed());
                GameFrame frame = new GameFrame(engine);
                frame.setVisible(true);
                frame.board.startGame();
            }
        });
    }

    /** Convenience {@code main} so the module can be launched directly in dev. */
    public static void main(String[] args) {
        run();
    }
}

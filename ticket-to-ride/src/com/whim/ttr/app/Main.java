package com.whim.ttr.app;

import com.whim.ttr.api.GameEngine;
import com.whim.ttr.domain.Board;
import com.whim.ttr.domain.Deck;
import com.whim.ttr.domain.GameState;
import com.whim.ttr.domain.Player;
import com.whim.ttr.ui.DevStubEngine;
import com.whim.ttr.ui.GameFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point. Wires Board → Deck → Players → {@link GameState} → engine →
 * {@link GameFrame} and shows the window.
 *
 * <p>By default it uses the real rules engine
 * {@code com.whim.ttr.engine.RulesEngine} (the one and only place this module
 * touches the {@code engine} package). Pass {@code stub} as the first argument
 * to swap in {@link DevStubEngine} for UI-only previews.</p>
 */
public final class Main {

    /** Distinct player token colors, indexed by seat. */
    private static final Color[] TOKENS = {
        new Color(205, 45, 45),    // red
        new Color(40, 90, 200),    // blue
        new Color(40, 155, 70),    // green
        new Color(235, 200, 40),   // yellow
        new Color(35, 35, 40)      // black
    };

    private static final String[] DEFAULT_NAMES = {
        "Red", "Blue", "Green", "Yellow", "Black"
    };

    private Main() { }

    public static void main(String[] args) {
        final boolean stub = args.length > 0 && "stub".equalsIgnoreCase(args[0]);
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                launch(stub);
            }
        });
    }

    private static void launch(boolean stub) {
        int players = askPlayerCount();
        if (players < 0) {
            return;
        }

        List<Player> roster = new ArrayList<Player>();
        for (int i = 0; i < players; i++) {
            roster.add(new Player(i, DEFAULT_NAMES[i], TOKENS[i]));
        }

        Board board = new Board();
        Deck deck = new Deck(System.currentTimeMillis());
        GameState state = new GameState(roster, board, deck);

        GameEngine engine;
        if (stub) {
            engine = new DevStubEngine(state);
        } else {
            // The single, deliberate reference to the engine package.
            engine = new com.whim.ttr.engine.RulesEngine(state);
        }

        new GameFrame(engine).start();
    }

    /** Simple 2–5 player start dialog. Returns the count, or -1 if cancelled. */
    private static int askPlayerCount() {
        Object[] options = { "2", "3", "4", "5" };
        Object choice = JOptionPane.showInputDialog(null,
                "How many players? (2–5)", "Ticket to Ride — Europe",
                JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        if (choice == null) {
            return -1;
        }
        try {
            int n = Integer.parseInt(choice.toString());
            if (n < 2) {
                n = 2;
            }
            if (n > 5) {
                n = 5;
            }
            return n;
        } catch (NumberFormatException e) {
            return 2;
        }
    }
}

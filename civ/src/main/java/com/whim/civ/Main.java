package com.whim.civ;

import com.whim.civ.domain.EconomyView;
import com.whim.civ.domain.GameFactory;
import com.whim.civ.domain.GameState;
import com.whim.civ.engine.EngineEconomyView;
import com.whim.civ.engine.GameEngine;
import com.whim.civ.ui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Entry point for the standalone Civilization (1991) clone — Java 8 Swing.
 *
 * <p>Wires the three independently-authored layers together:
 * <ul>
 *   <li>{@code domain} — the world model + {@link GameFactory} that seeds a ready-to-play game.</li>
 *   <li>{@code engine} — {@link GameEngine} (the {@code EngineServices} bridge the turn loop
 *       calls into) plus {@link EngineEconomyView}, the engine-backed {@link EconomyView}.</li>
 *   <li>{@code ui} — {@link MainFrame}, the square-tiled Swing front end.</li>
 * </ul>
 *
 * <p>The UI talks to the engine only through the domain {@code EngineServices} / {@code
 * EconomyView} interfaces, so the packages stay decoupled. We inject the real
 * {@link EngineEconomyView} via {@link MainFrame#setEconomyView} so city screens show
 * live engine numbers rather than the UI's fallback.
 */
public final class Main {

    /** Standard map / civ-count for a quick-start game; tweak freely. */
    private static final int MAP_WIDTH = 60;
    private static final int MAP_HEIGHT = 40;
    private static final int NUM_CIVS = 5;

    private Main() {
    }

    public static void main(String[] args) {
        final long seed = parseSeed(args);

        // Build the world and the engine. Share one Random-seeded engine so AI/combat are
        // reproducible for a given seed.
        final GameState state =
                GameFactory.newStandardGame(MAP_WIDTH, MAP_HEIGHT, NUM_CIVS, seed);
        final GameEngine engine = new GameEngine(new java.util.Random(seed));
        final EconomyView economyView = new EngineEconomyView();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MainFrame frame = new MainFrame(state, engine);
                frame.setEconomyView(economyView);
                frame.showGame();
            }
        });
    }

    private static long parseSeed(String[] args) {
        if (args != null && args.length > 0) {
            try {
                return Long.parseLong(args[0].trim());
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return 4000L; // a fixed, friendly default seed (a nod to 4000 B.C.)
    }
}

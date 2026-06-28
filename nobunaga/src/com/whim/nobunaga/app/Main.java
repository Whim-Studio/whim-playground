package com.whim.nobunaga.app;

import com.whim.nobunaga.domain.GameEngine;
import com.whim.nobunaga.domain.GameLoopManager;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.engine.GameEngineImpl;
import com.whim.nobunaga.map.ProvinceData;
import com.whim.nobunaga.ui.GameController;
import com.whim.nobunaga.ui.GameFrame;
import com.whim.nobunaga.ui.StartScreen;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for <i>Nobunaga's Ambition: Zenkokuban</i>.
 *
 * <p>All UI construction happens on the EDT. The flow is exactly as the contract
 * prescribes: {@link StartScreen} (pick a roster daimyo) →
 * {@link ProvinceData#newGame} builds the seeded {@link GameState} →
 * {@link GameEngineImpl} (the only engine concrete class the UI references) is
 * wrapped in a {@link GameLoopManager} and a {@link GameController} →
 * {@link GameFrame}.
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
                new StartScreen(new StartScreen.SelectionHandler() {
                    public void onDaimyoChosen(int daimyoId) {
                        startGame(daimyoId);
                    }
                }).setVisible(true);
            }
        });
    }

    private static void startGame(int daimyoId) {
        long seed = System.nanoTime();
        GameState state = ProvinceData.newGame(daimyoId, seed);
        GameEngine engine = new GameEngineImpl();
        GameLoopManager loop = new GameLoopManager(engine);
        GameController controller = new GameController(state, engine, loop);
        new GameFrame(controller).setVisible(true);
    }
}

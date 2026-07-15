package com.whim.ruinlander.app;

import com.whim.ruinlander.domain.GameStateManager;
import com.whim.ruinlander.domain.WorldFactory;
import com.whim.ruinlander.engine.ItemDb;
import com.whim.ruinlander.ui.GameFrame;

import javax.swing.SwingUtilities;

/**
 * Entry point. Builds the starter world (domain {@link WorldFactory} seeded with
 * engine {@link ItemDb} items) and shows the Swing {@link GameFrame} with a
 * playable exploration loop.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // Inject the engine's item catalogue so loot/starter gear use real defs.
        final WorldFactory.World world = WorldFactory.build(ItemDb.all());
        final GameStateManager gsm = new GameStateManager(world.map, world.player);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GameFrame frame = new GameFrame(gsm);
                frame.setVisible(true);
                frame.requestFocusInWindow();
            }
        });
    }
}

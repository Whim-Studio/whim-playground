package com.whim.merchantprince;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.ui.FleetScreen;
import com.whim.merchantprince.ui.GameOverScreen;
import com.whim.merchantprince.ui.MainMenuScreen;
import com.whim.merchantprince.ui.MapScreen;
import com.whim.merchantprince.ui.MarketScreen;
import com.whim.merchantprince.ui.NewGameScreen;
import com.whim.merchantprince.ui.VeniceScreen;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

/**
 * Application entry point. Wires the screen manager, registers every screen by name
 * and shows the main menu. Each screen keeps a fixed {@code (Game)} constructor and
 * CardLayout {@code name()}, so individual screens can be developed independently.
 */
public class MerchantPrince {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { launch(); }
        });
    }

    private static void launch() {
        Game game = new Game();
        game.screens.register(new MainMenuScreen(game));
        game.screens.register(new NewGameScreen(game));
        game.screens.register(new MapScreen(game));
        game.screens.register(new MarketScreen(game));
        game.screens.register(new FleetScreen(game));
        game.screens.register(new VeniceScreen(game));
        game.screens.register(new GameOverScreen(game));

        JFrame frame = new JFrame("Merchant Prince — Java 8 / Swing recreation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(game.screens.root());
        frame.setPreferredSize(new Dimension(900, 680));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        game.screens.show(Game.MENU);
    }
}

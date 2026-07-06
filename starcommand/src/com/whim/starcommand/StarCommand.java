package com.whim.starcommand;

import com.whim.starcommand.app.Game;
import com.whim.starcommand.ui.CharCreateScreen;
import com.whim.starcommand.ui.GalaxyMapScreen;
import com.whim.starcommand.ui.GroundCombatScreen;
import com.whim.starcommand.ui.HelpScreen;
import com.whim.starcommand.ui.MainMenuScreen;
import com.whim.starcommand.ui.ShipCombatScreen;
import com.whim.starcommand.ui.StarportScreen;
import com.whim.starcommand.ui.UniqueAreaScreen;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Application entry point. Wires the screen manager, registers every screen,
 * and shows the main menu.
 */
public class StarCommand {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { launch(); }
        });
    }

    private static void launch() {
        Game game = new Game();
        game.screens.register(new MainMenuScreen(game));
        game.screens.register(new CharCreateScreen(game));
        game.screens.register(new StarportScreen(game));
        game.screens.register(new GalaxyMapScreen(game));
        game.screens.register(new ShipCombatScreen(game));
        game.screens.register(new GroundCombatScreen(game));
        game.screens.register(new UniqueAreaScreen(game));
        game.screens.register(new HelpScreen(game));

        JFrame frame = new JFrame("Star Command — Java 8 / Swing recreation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(game.screens.root());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        game.screens.show(Game.MENU);
    }
}

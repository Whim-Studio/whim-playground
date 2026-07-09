package com.whim.samurai;

import com.whim.samurai.app.Game;
import com.whim.samurai.ui.BattleScreen;
import com.whim.samurai.ui.CharCreateScreen;
import com.whim.samurai.ui.CharacterScreen;
import com.whim.samurai.ui.DuelScreen;
import com.whim.samurai.ui.EncounterScreen;
import com.whim.samurai.ui.FamilyScreen;
import com.whim.samurai.ui.GameOverScreen;
import com.whim.samurai.ui.HelpScreen;
import com.whim.samurai.ui.MainMenuScreen;
import com.whim.samurai.ui.MapScreen;
import com.whim.samurai.ui.NinjaScreen;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Application entry point. Wires the screen manager, registers every screen by
 * name and shows the main menu. Each screen owns a fixed CardLayout key from
 * {@link Game}; individual screen implementations may be developed independently
 * as long as they keep the {@code (Game)} constructor and their {@code name()}.
 */
public class SwordOfTheSamurai {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { launch(); }
        });
    }

    private static void launch() {
        Game game = new Game();
        game.screens.register(new MainMenuScreen(game));
        game.screens.register(new CharCreateScreen(game));
        game.screens.register(new MapScreen(game));
        game.screens.register(new CharacterScreen(game));
        game.screens.register(new FamilyScreen(game));
        game.screens.register(new DuelScreen(game));
        game.screens.register(new BattleScreen(game));
        game.screens.register(new NinjaScreen(game));
        game.screens.register(new EncounterScreen(game));
        game.screens.register(new GameOverScreen(game));
        game.screens.register(new HelpScreen(game));

        JFrame frame = new JFrame("Sword of the Samurai — Java 8 / Swing recreation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(game.screens.root());
        frame.setPreferredSize(new java.awt.Dimension(880, 660));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        game.screens.show(Game.MENU);
    }
}

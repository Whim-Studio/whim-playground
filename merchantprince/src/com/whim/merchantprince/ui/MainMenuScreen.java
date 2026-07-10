package com.whim.merchantprince.ui;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.app.Screen;
import com.whim.merchantprince.engine.SaveManager;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.render.Palette;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Title screen: New Game, Continue (if a save exists), Quit. */
public class MainMenuScreen extends Screen {

    public MainMenuScreen(Game game) {
        super(game);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel title = UiKit.label("Merchant Prince", UiKit.TITLE, Palette.INK);
        JLabel sub = UiKit.label("Venice, Anno Domini 1300 — a house rises by trade and intrigue",
                UiKit.BODY, Palette.INK);
        center(title); center(sub);

        add(Box.createVerticalGlue());
        add(title);
        add(Box.createVerticalStrut(8));
        add(sub);
        add(Box.createVerticalStrut(36));

        JButton newGame = UiKit.button("New Game");
        newGame.addActionListener(e -> game.screens.show(Game.NEWGAME));
        center(newGame); add(newGame); add(Box.createVerticalStrut(14));

        if (SaveManager.saveExists()) {
            JButton cont = UiKit.button("Continue");
            cont.addActionListener(e -> {
                try {
                    GameState st = SaveManager.load();
                    game.state = st;
                    game.screens.show(Game.MAP);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            center(cont); add(cont); add(Box.createVerticalStrut(14));
        }

        JButton quit = UiKit.button("Quit");
        quit.addActionListener(e -> System.exit(0));
        center(quit); add(quit);
        add(Box.createVerticalGlue());
    }

    private void center(JComponent c) { c.setAlignmentX(CENTER_ALIGNMENT); }

    @Override public String name() { return Game.MENU; }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(Palette.PARCHMENT);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }
}

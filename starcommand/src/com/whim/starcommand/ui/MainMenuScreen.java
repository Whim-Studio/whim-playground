package com.whim.starcommand.ui;

import com.whim.starcommand.app.Game;
import com.whim.starcommand.app.Screen;
import com.whim.starcommand.engine.SaveManager;
import com.whim.starcommand.render.Palette;
import com.whim.starcommand.render.Starfield;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;

/** Title screen: New Game / Continue / Help / Quit, over a starfield. */
public class MainMenuScreen extends Screen {

    private final Starfield stars = new Starfield(900, 640, 260, 42L);
    private JButton continueBtn;

    public MainMenuScreen(Game game) {
        super(game);
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(900, 640));

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel title = UiKit.label("STAR  COMMAND", UiKit.TITLE, Palette.ACCENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = UiKit.label("A faithful Java 8 / Swing recreation — The Triangle awaits",
                UiKit.BODY, Palette.TEXT_DIM);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setHorizontalAlignment(SwingConstants.CENTER);

        col.add(title);
        col.add(Box.createVerticalStrut(6));
        col.add(sub);
        col.add(Box.createVerticalStrut(34));

        col.add(menuButton("New Game  (N)", new Runnable() {
            public void run() { newGame(); }
        }));
        col.add(Box.createVerticalStrut(12));
        continueBtn = menuButton("Continue  (C)", new Runnable() {
            public void run() { continueGame(); }
        });
        col.add(continueBtn);
        col.add(Box.createVerticalStrut(12));
        col.add(menuButton("Controls / Help  (H)", new Runnable() {
            public void run() { game.screens.show(Game.HELP); }
        }));
        col.add(Box.createVerticalStrut(12));
        col.add(menuButton("Quit  (Q)", new Runnable() {
            public void run() { System.exit(0); }
        }));

        add(col);
        installKeys();
    }

    private JButton menuButton(String text, final Runnable action) {
        JButton b = UiKit.button(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(320, 46));
        b.addActionListener(e -> action.run());
        return b;
    }

    private void installKeys() {
        Keys.bind(this, "N", new Runnable() { public void run() { newGame(); } });
        Keys.bind(this, "C", new Runnable() { public void run() { continueGame(); } });
        Keys.bind(this, "H", new Runnable() { public void run() { game.screens.show(Game.HELP); } });
        Keys.bind(this, "Q", new Runnable() { public void run() { System.exit(0); } });
    }

    private void newGame() {
        game.newGame();
        game.screens.show(Game.CREATE);
    }

    private void continueGame() {
        if (!SaveManager.saveExists()) {
            JOptionPane.showMessageDialog(this, "No saved game found.");
            return;
        }
        try {
            game.state = SaveManager.load(SaveManager.defaultSaveFile());
            game.screens.show(Game.STARPORT);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not load save: " + ex.getMessage());
        }
    }

    @Override
    public void onShow() {
        continueBtn.setEnabled(SaveManager.saveExists());
    }

    @Override
    public String name() { return Game.MENU; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        stars.paint(g2, getWidth(), getHeight());
    }
}

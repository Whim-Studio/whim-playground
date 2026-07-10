package com.whim.merchantprince.ui;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.app.Screen;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.render.Palette;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** End-of-game screen: victory or defeat and the reason, then back to the menu. */
public class GameOverScreen extends Screen {

    private final JLabel headline = UiKit.label("", UiKit.TITLE, Palette.INK);
    private final JLabel reason = UiKit.label("", UiKit.BODY, Palette.INK);

    public GameOverScreen(Game game) {
        super(game);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalGlue());
        center(headline); add(headline);
        add(Box.createVerticalStrut(12));
        center(reason); add(reason);
        add(Box.createVerticalStrut(30));
        JButton menu = UiKit.button("Return to Menu");
        menu.addActionListener(e -> game.screens.show(Game.MENU));
        center(menu); add(menu);
        add(Box.createVerticalGlue());
    }

    private void center(JComponent c) { c.setAlignmentX(CENTER_ALIGNMENT); }

    @Override public String name() { return Game.GAMEOVER; }

    @Override public void onShow() {
        GameState s = game.state;
        if (s == null) return;
        headline.setText(s.victory ? "Your House Triumphs!" : "Your House Falls");
        reason.setText(s.gameOverReason);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(Palette.PARCHMENT);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }
}

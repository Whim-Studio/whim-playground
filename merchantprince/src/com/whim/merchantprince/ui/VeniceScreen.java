package com.whim.merchantprince.ui;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.app.Screen;
import com.whim.merchantprince.render.Palette;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * PLACEHOLDER (owned by the Market/Venice UI task, T4). The Venice politics screen:
 * bribe the Council of Ten for offices, ascend toward Doge, influence cardinals and
 * the papacy, and operate the den of iniquities for dirty tricks
 * (GAME_DESIGN_REFERENCE §6). All actions route through {@code PoliticsEngine}.
 */
public class VeniceScreen extends Screen {

    public VeniceScreen(Game game) {
        super(game);
        setLayout(new BorderLayout());
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        nav.setBackground(Palette.PARCHMENT_DK);
        JButton back = UiKit.button("Back to Map");
        back.addActionListener(e -> game.screens.show(Game.MAP));
        nav.add(back);
        nav.add(UiKit.label("Venice & Intrigue — to be implemented (T4)", UiKit.HEAD, Palette.INK));
        add(nav, BorderLayout.NORTH);
    }

    @Override public String name() { return Game.VENICE; }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(Palette.PARCHMENT);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }
}

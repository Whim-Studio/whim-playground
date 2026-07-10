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
 * PLACEHOLDER (owned by the Map/Fleet UI task, T3). The fleet manager: list the
 * player's ships/caravans, their cargo and location, and let the player dispatch a
 * unit to a destination city or assign an automated route
 * (GAME_DESIGN_REFERENCE §4). Movement goes through {@code TravelEngine}.
 */
public class FleetScreen extends Screen {

    public FleetScreen(Game game) {
        super(game);
        setLayout(new BorderLayout());
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        nav.setBackground(Palette.PARCHMENT_DK);
        JButton back = UiKit.button("Back to Map");
        back.addActionListener(e -> game.screens.show(Game.MAP));
        nav.add(back);
        nav.add(UiKit.label("Fleet — to be implemented (T3)", UiKit.HEAD, Palette.INK));
        add(nav, BorderLayout.NORTH);
    }

    @Override public String name() { return Game.FLEET; }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(Palette.PARCHMENT);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }
}

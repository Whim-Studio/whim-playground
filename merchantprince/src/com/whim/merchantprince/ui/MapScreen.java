package com.whim.merchantprince.ui;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.app.Screen;
import com.whim.merchantprince.engine.Rng;
import com.whim.merchantprince.engine.TurnManager;
import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.render.Palette;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * PLACEHOLDER (owned by the Map/Fleet UI task, T3). Draws the Afro-Eurasian map
 * with cities and the player's fleets, click-to-select a city to open its market,
 * and hosts the top navigation + End Turn control (GAME_DESIGN_REFERENCE §2).
 *
 * <p>This T0 version renders cities as dots and provides working navigation and a
 * working End Turn so the whole loop is playable before T3 lands the real map.
 */
public class MapScreen extends Screen {

    private final JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));

    public MapScreen(Game game) {
        super(game);
        setLayout(new BorderLayout());

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        nav.setBackground(Palette.PARCHMENT_DK);
        nav.add(navButton("Market", Game.MARKET));
        nav.add(navButton("Fleet", Game.FLEET));
        nav.add(navButton("Venice", Game.VENICE));
        JButton end = UiKit.button("End Turn");
        end.addActionListener(e -> {
            TurnManager.endTurn(game.state, game.rng);
            if (game.state.gameOver) game.screens.show(Game.GAMEOVER);
            else { refreshStatus(); repaint(); }
        });
        nav.add(end);

        status.setBackground(Palette.PARCHMENT_DK);

        JPanel top = new JPanel(new BorderLayout());
        top.add(nav, BorderLayout.NORTH);
        top.add(status, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
    }

    private JButton navButton(String label, String card) {
        JButton b = UiKit.button(label);
        b.addActionListener(e -> game.screens.show(card));
        return b;
    }

    private void refreshStatus() {
        status.removeAll();
        GameState s = game.state;
        if (s != null) {
            Family p = s.player();
            status.add(UiKit.label("Year " + s.year + " / " + s.endYear
                    + "    House " + p.surname
                    + "    " + p.florins + " florins", UiKit.HEAD, Palette.INK));
        }
        status.revalidate();
        status.repaint();
    }

    @Override public String name() { return Game.MAP; }

    @Override public void onShow() { refreshStatus(); repaint(); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(Palette.SEA);
        g2.fillRect(0, 0, getWidth(), getHeight());
        GameState s = game.state;
        if (s == null) return;
        for (City c : s.cities) {
            g2.setColor(c.open ? Palette.GOLD : new Color(0x6B4E2E));
            g2.fillOval(c.x, c.y + 60, 14, 14);
            g2.setColor(Palette.INK);
            g2.setFont(UiKit.SMALL);
            g2.drawString(c.name, c.x + 18, c.y + 72);
        }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }
}

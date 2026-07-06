package com.whim.starcommand.ui;

import com.whim.starcommand.app.Game;
import com.whim.starcommand.app.Screen;
import com.whim.starcommand.engine.Content;
import com.whim.starcommand.engine.SaveManager;
import com.whim.starcommand.model.Mission;
import com.whim.starcommand.model.Ship;
import com.whim.starcommand.model.Weapon;
import com.whim.starcommand.render.Palette;
import com.whim.starcommand.render.Starfield;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Star Command HQ starport: review crew and ship, buy weapons, upgrade the hull,
 * repair, accept missions, save, and launch to the galaxy map.
 */
public class StarportScreen extends Screen {

    private final Starfield stars = new Starfield(900, 640, 160, 99L);
    private JLabel creditsLabel;
    private JPanel statusPanel;
    private JPanel shopPanel;

    public StarportScreen(Game game) {
        super(game);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiKit.label("STARPORT — STAR COMMAND HQ", UiKit.HEAD, Palette.ACCENT), BorderLayout.WEST);
        creditsLabel = UiKit.label("", UiKit.HEAD, Palette.ACCENT_2);
        top.add(creditsLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        statusPanel = new JPanel();
        statusPanel.setOpaque(false);
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        JScrollPane statusScroll = new JScrollPane(statusPanel);
        statusScroll.setPreferredSize(new Dimension(360, 400));
        statusScroll.getViewport().setOpaque(false);
        statusScroll.setOpaque(false);
        add(statusScroll, BorderLayout.WEST);

        shopPanel = new JPanel();
        shopPanel.setOpaque(false);
        shopPanel.setLayout(new BoxLayout(shopPanel, BoxLayout.Y_AXIS));
        JScrollPane shopScroll = new JScrollPane(shopPanel);
        shopScroll.getViewport().setOpaque(false);
        shopScroll.setOpaque(false);
        add(shopScroll, BorderLayout.CENTER);

        add(footer(), BorderLayout.SOUTH);

        Keys.bind(this, "G", new Runnable() { public void run() { toGalaxy(); } });
        Keys.bind(this, "S", new Runnable() { public void run() { saveGame(); } });
    }

    private JPanel footer() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.setOpaque(false);
        JButton repair = UiKit.button("Repair ship");
        repair.addActionListener(e -> repair());
        JButton save = UiKit.button("Save (S)");
        save.addActionListener(e -> saveGame());
        JButton galaxy = UiKit.button("Launch to Galaxy (G)");
        galaxy.addActionListener(e -> toGalaxy());
        p.add(repair);
        p.add(save);
        p.add(galaxy);
        return p;
    }

    private void rebuild() {
        creditsLabel.setText(game.state.credits + " cr");
        buildStatus();
        buildShop();
        revalidate();
        repaint();
    }

    private void buildStatus() {
        statusPanel.removeAll();
        Ship s = game.state.ship;
        statusPanel.add(UiKit.label("SHIP: " + s.className, UiKit.BODY, Palette.TEXT));
        statusPanel.add(UiKit.label("  Hull " + s.hull + "/" + s.maxHull
                + "   Shield " + s.shield + "/" + s.maxShield, UiKit.MONO, Palette.TEXT_DIM));
        statusPanel.add(UiKit.label("  Engines " + s.engines
                + "   Weapon slots " + s.weapons.size() + "/" + s.weaponSlots, UiKit.MONO, Palette.TEXT_DIM));
        for (Weapon w : s.weapons) {
            statusPanel.add(UiKit.label("   • " + w.name, UiKit.MONO, Palette.ACCENT));
        }
        statusPanel.add(Box.createVerticalStrut(10));
        statusPanel.add(UiKit.label("CREW (" + game.state.crew.size() + ")", UiKit.BODY, Palette.TEXT));
        for (com.whim.starcommand.model.Character c : game.state.crew) {
            statusPanel.add(UiKit.label("  " + c.name + " — " + c.role
                    + "  HP " + c.hp + "/" + c.maxHp, UiKit.MONO, Palette.TEXT_DIM));
        }
        statusPanel.add(Box.createVerticalStrut(10));
        statusPanel.add(UiKit.label("MISSIONS", UiKit.BODY, Palette.TEXT));
        for (Mission m : game.state.missions) {
            String tag = m.complete ? "[done] " : (m.accepted ? "[active] " : "[open] ");
            statusPanel.add(UiKit.label("  " + tag + m.title, UiKit.MONO,
                    m.complete ? Palette.GOOD : Palette.ACCENT_2));
        }
    }

    private void buildShop() {
        shopPanel.removeAll();
        shopPanel.add(UiKit.label("WEAPON BAY", UiKit.BODY, Palette.ACCENT));
        for (final Weapon w : Content.weaponShop()) {
            shopPanel.add(shopRow(w.toString(), w.cost, new Runnable() {
                public void run() { buyWeapon(w); }
            }));
        }
        shopPanel.add(Box.createVerticalStrut(10));
        shopPanel.add(UiKit.label("SHIPYARD (trade-up)", UiKit.BODY, Palette.ACCENT));
        for (final Ship hull : Content.shipShop()) {
            int cost = Content.shipCost(hull.className);
            if (cost == 0) continue;
            shopPanel.add(shopRow(hull.className + "  hull " + hull.maxHull
                    + " shield " + hull.maxShield + " slots " + hull.weaponSlots, cost, new Runnable() {
                public void run() { buyShip(hull, cost); }
            }));
        }
        shopPanel.add(Box.createVerticalStrut(10));
        shopPanel.add(UiKit.label("HQ BRIEFING ROOM", UiKit.BODY, Palette.ACCENT));
        for (final Mission m : game.state.missions) {
            if (m.complete || m.accepted) continue;
            shopPanel.add(shopRow("Accept: " + m.title + "  (+" + m.reward + "cr)", 0, new Runnable() {
                public void run() { acceptMission(m); }
            }));
        }
    }

    private JPanel shopRow(String text, int cost, final Runnable action) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(9999, 34));
        row.add(UiKit.label(text, UiKit.MONO, Palette.TEXT), BorderLayout.CENTER);
        JButton buy = UiKit.button(cost > 0 ? "Buy " + cost + "cr" : "Accept");
        buy.setFont(UiKit.MONO);
        buy.addActionListener(e -> action.run());
        row.add(buy, BorderLayout.EAST);
        return row;
    }

    private void buyWeapon(Weapon w) {
        Ship s = game.state.ship;
        if (s.weapons.size() >= s.weaponSlots) {
            JOptionPane.showMessageDialog(this, "No free weapon slots on the " + s.className + ".");
            return;
        }
        if (game.state.credits < w.cost) { notEnough(); return; }
        game.state.credits -= w.cost;
        s.weapons.add(new Weapon(w.name, w.type, w.minDamage, w.maxDamage, w.accuracy, w.cost));
        rebuild();
    }

    private void buyShip(Ship template, int cost) {
        if (game.state.credits < cost) { notEnough(); return; }
        game.state.credits -= cost;
        Ship old = game.state.ship;
        Ship s = Content.makeShip(template.className, template.maxHull,
                template.maxShield, template.engines, template.weaponSlots);
        // carry over as many weapons as the new hull can fit
        List<Weapon> keep = old.weapons;
        for (int i = 0; i < keep.size() && s.weapons.size() < s.weaponSlots; i++) {
            s.weapons.add(keep.get(i));
        }
        game.state.ship = s;
        rebuild();
    }

    private void acceptMission(Mission m) {
        m.accepted = true;
        rebuild();
    }

    private void repair() {
        int need = (game.state.ship.maxHull - game.state.ship.hull)
                + (game.state.ship.maxShield - game.state.ship.shield);
        int cost = need * 3;
        if (cost == 0) { JOptionPane.showMessageDialog(this, "Ship is already at full integrity."); return; }
        if (game.state.credits < cost) { notEnough(); return; }
        game.state.credits -= cost;
        game.state.ship.repairFull();
        for (com.whim.starcommand.model.Character c : game.state.crew) { c.hp = c.maxHp; c.alive = true; }
        rebuild();
    }

    private void saveGame() {
        try {
            SaveManager.save(game.state, SaveManager.defaultSaveFile());
            JOptionPane.showMessageDialog(this, "Game saved.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    private void toGalaxy() { game.screens.show(Game.GALAXY); }

    private void notEnough() { JOptionPane.showMessageDialog(this, "Not enough credits."); }

    @Override
    public void onShow() { rebuild(); }

    @Override
    public String name() { return Game.STARPORT; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        stars.paint(g2, getWidth(), getHeight());
    }
}

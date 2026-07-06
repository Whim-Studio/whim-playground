package com.whim.starcommand.ui;

import com.whim.starcommand.app.Game;
import com.whim.starcommand.app.Screen;
import com.whim.starcommand.model.GameState;
import com.whim.starcommand.model.Mission;
import com.whim.starcommand.model.Planet;
import com.whim.starcommand.model.Sector;
import com.whim.starcommand.render.Palette;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The galaxy map of The Triangle. Move the ship between adjacent sectors with
 * the arrow keys or by clicking; scan planets; entering a hostile sector
 * triggers ship-to-ship combat. Return to HQ to resupply.
 */
public class GalaxyMapScreen extends Screen {

    private final GridPanel grid = new GridPanel();
    private JLabel info;
    private JLabel status;

    public GalaxyMapScreen(Game game) {
        super(game);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiKit.label("GALAXY MAP — THE TRIANGLE", UiKit.HEAD, Palette.ACCENT), BorderLayout.WEST);
        status = UiKit.label("", UiKit.BODY, Palette.ACCENT_2);
        top.add(status, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        add(grid, BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(260, 100));
        info = UiKit.label("", UiKit.MONO, Palette.TEXT);
        info.setVerticalAlignment(JLabel.TOP);
        side.add(info, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);

        add(footer(), BorderLayout.SOUTH);

        Keys.bind(this, "UP",    moveAction(0, -1));
        Keys.bind(this, "DOWN",  moveAction(0, 1));
        Keys.bind(this, "LEFT",  moveAction(-1, 0));
        Keys.bind(this, "RIGHT", moveAction(1, 0));
        Keys.bind(this, "S", new Runnable() { public void run() { scan(); } });
        Keys.bind(this, "D", new Runnable() { public void run() { deploy(); } });
        Keys.bind(this, "B", new Runnable() { public void run() { returnToPort(); } });
    }

    private JPanel footer() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setOpaque(false);
        p.add(UiKit.label("Arrows: move   S: scan   D: deploy   B: dock", UiKit.MONO, Palette.TEXT_DIM));
        JButton scan = UiKit.button("Scan (S)");
        scan.addActionListener(e -> scan());
        JButton deploy = UiKit.button("Deploy (D)");
        deploy.addActionListener(e -> deploy());
        JButton dock = UiKit.button("Dock at HQ (B)");
        dock.addActionListener(e -> returnToPort());
        p.add(scan);
        p.add(deploy);
        p.add(dock);
        return p;
    }

    private Runnable moveAction(final int dx, final int dy) {
        return new Runnable() { public void run() { moveShip(dx, dy); } };
    }

    private void moveShip(int dx, int dy) {
        int nx = game.state.shipX + dx;
        int ny = game.state.shipY + dy;
        if (nx < 0 || ny < 0 || nx >= GameState.GALAXY_W || ny >= GameState.GALAXY_H) return;
        game.state.shipX = nx;
        game.state.shipY = ny;
        game.state.turn++;
        Sector s = game.state.currentSector();
        s.visited = true;
        refresh();
        if (s.hostilePresence) {
            triggerCombat(s);
        }
    }

    private void triggerCombat(Sector s) {
        boolean boss = s.planet != null && s.planet.kind == Planet.Kind.PIRATE_BASE
                && "Blackbeard's Hideout".equals(s.planet.name);
        ShipCombatScreen combat = (ShipCombatScreen) game.screens.get(Game.COMBAT);
        combat.begin(s, boss);
        game.screens.show(Game.COMBAT);
    }

    private void scan() {
        Sector s = game.state.currentSector();
        if (s.planet == null) {
            info.setText("<html>Deep space. Long-range scan finds no bodies here.</html>");
            return;
        }
        s.planet.scanned = true;
        s.planet.scanReport = describe(s.planet);
        refresh();
    }

    private String describe(Planet p) {
        switch (p.kind) {
            case STARPORT: return "Star Command HQ — friendly starport. Dock to resupply.";
            case WORLD: return "Inhabited world. Trade and rumours available.";
            case PIRATE_BASE: return "PIRATE BASE — heavily defended. Expect a fight.";
            case HIVE: return "Insectoid HIVE of the Beta Frontier. Extreme hazard.";
            case DERELICT: return "Derelict hulk drifting silent. Salvage possible.";
            default: return "Nothing of note.";
        }
    }

    /** Deploy a drop ship for a ground raid on a hostile "unique area" (base/hive). */
    private void deploy() {
        Sector s = game.state.currentSector();
        if (s.planet == null
                || !(s.planet.kind == Planet.Kind.PIRATE_BASE || s.planet.kind == Planet.Kind.HIVE)) {
            JOptionPane.showMessageDialog(this, "No surface objective to raid here. Scan for a base or hive.");
            return;
        }
        if (game.state.livingCrew() == 0) {
            JOptionPane.showMessageDialog(this, "No crew able to deploy.");
            return;
        }
        boolean boss = "Blackbeard's Hideout".equals(s.planet.name);
        UniqueAreaScreen area = (UniqueAreaScreen) game.screens.get(Game.AREA);
        area.begin(s, boss);
        game.screens.show(Game.AREA);
    }

    private void returnToPort() {
        Sector s = game.state.currentSector();
        if (s.planet != null && s.planet.kind == Planet.Kind.STARPORT) {
            game.screens.show(Game.STARPORT);
        } else {
            JOptionPane.showMessageDialog(this, "You must be at Star Command HQ to dock.");
        }
    }

    private void refresh() {
        Sector s = game.state.currentSector();
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("Sector ").append((char) ('A' + s.x)).append(s.y).append("<br>");
        sb.append("Frontier: ").append(s.frontier).append("<br>");
        if (s.planet != null) {
            sb.append("Body: ").append(s.planet.name).append("<br>");
            if (s.planet.scanned) sb.append(s.planet.scanReport).append("<br>");
            else sb.append("(unscanned — press S)<br>");
        } else {
            sb.append("Empty space<br>");
        }
        if (s.hostilePresence) sb.append("<font color='#ff5a5a'>Hostiles detected!</font>");
        sb.append("</html>");
        info.setText(sb.toString());
        status.setText("Turn " + game.state.turn + "   Credits " + game.state.credits);
        grid.repaint();
    }

    @Override
    public void onShow() { refresh(); requestFocusInWindow(); }

    @Override
    public String name() { return Game.GALAXY; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Palette.SPACE);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** The clickable/painted sector grid. */
    private class GridPanel extends JPanel {
        GridPanel() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { onClick(e.getX(), e.getY()); }
            });
        }

        private int cell() {
            int cw = getWidth() / GameState.GALAXY_W;
            int ch = getHeight() / GameState.GALAXY_H;
            return Math.max(20, Math.min(cw, ch));
        }

        private void onClick(int px, int py) {
            int c = cell();
            int gx = px / c;
            int gy = py / c;
            int dx = gx - game.state.shipX;
            int dy = gy - game.state.shipY;
            if (Math.abs(dx) + Math.abs(dy) == 1) moveShip(dx, dy);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (game.state == null) return;
            int c = cell();
            for (int x = 0; x < GameState.GALAXY_W; x++) {
                for (int y = 0; y < GameState.GALAXY_H; y++) {
                    Sector s = game.state.galaxy[x][y];
                    int px = x * c, py = y * c;
                    g2.setColor(frontierColor(s.frontier, s.visited));
                    g2.fillRect(px + 2, py + 2, c - 4, c - 4);
                    g2.setColor(Palette.PANEL_LINE);
                    g2.drawRect(px + 2, py + 2, c - 4, c - 4);
                    if (s.planet != null) {
                        g2.setColor(planetColor(s.planet));
                        g2.fillOval(px + c / 2 - 6, py + c / 2 - 6, 12, 12);
                    }
                    if (s.hostilePresence && s.visited) {
                        g2.setColor(Palette.DANGER);
                        g2.drawString("!", px + c - 12, py + 16);
                    }
                }
            }
            // player ship
            int sx = game.state.shipX * c + c / 2;
            int sy = game.state.shipY * c + c / 2;
            com.whim.starcommand.render.ShipSprite.draw(g2, sx, sy, c / 3, Palette.ACCENT, false);
        }

        private Color frontierColor(Sector.Frontier f, boolean visited) {
            Color base;
            switch (f) {
                case ALPHA: base = Palette.ALPHA; break;
                case BETA:  base = Palette.BETA; break;
                default:    base = Palette.CORE; break;
            }
            if (!visited) return base.darker().darker();
            return base.darker();
        }

        private Color planetColor(Planet p) {
            switch (p.kind) {
                case STARPORT: return Palette.GOOD;
                case PIRATE_BASE: return Palette.DANGER;
                case HIVE: return Palette.BETA.brighter();
                case DERELICT: return Palette.TEXT_DIM;
                default: return Palette.ACCENT_2;
            }
        }
    }
}

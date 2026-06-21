package com.whim.startrek.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.whim.startrek.domain.Ship;
import com.whim.startrek.engine.BattleSimulator;
import com.whim.startrek.engine.Projectile;

/**
 * Real-time RTS arena: top-down render of a {@link BattleSimulator}. A Swing
 * {@link Timer} advances the sim ({@code battle.step(dt)}) then repaints. Draws
 * directional ship sprites, phaser beams, particle-style torpedo arcs, and
 * per-ship shield/hull bars. Click selects a ship (visual targeting overlay).
 */
public class BattlePanel extends JPanel {

    private static final int FPS = 30;
    private static final double DT = 1.0 / FPS;

    private BattleSimulator battle;
    private final Timer timer;

    /** Last known position per ship, to derive a facing angle for the sprite. */
    private final Map<Ship, double[]> lastPos = new HashMap<Ship, double[]>();
    private Ship targeted;

    public BattlePanel() {
        setBackground(UiTheme.SPACE_BG);
        setPreferredSize(new Dimension(900, 720));
        this.timer = new Timer(1000 / FPS, e -> tick());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                selectAt(ev.getX(), ev.getY());
            }
        });
    }

    /** Attach a battle and (re)start the animation loop. */
    public void setBattle(BattleSimulator battle) {
        this.battle = battle;
        this.targeted = null;
        this.lastPos.clear();
        repaint();
    }

    public BattleSimulator getBattle() {
        return battle;
    }

    public void start() {
        if (battle != null && !timer.isRunning()) {
            timer.start();
        }
    }

    public void stop() {
        timer.stop();
    }

    private void tick() {
        if (battle == null) {
            return;
        }
        if (!battle.isFinished()) {
            battle.step(DT);
        } else {
            timer.stop();
        }
        repaint();
    }

    // ---- scaling --------------------------------------------------------

    private double scaleX() {
        if (battle == null) {
            return 1.0;
        }
        double aw = arenaWidth();
        return aw <= 0 ? 1.0 : getWidth() / aw;
    }

    private double scaleY() {
        if (battle == null) {
            return 1.0;
        }
        double ah = arenaHeight();
        return ah <= 0 ? 1.0 : getHeight() / ah;
    }

    /** Arena bounds inferred from current ship/projectile extents (sim-agnostic). */
    private double arenaWidth() {
        double max = 800;
        if (battle != null) {
            for (Ship s : battle.getShips()) {
                max = Math.max(max, s.getX());
            }
        }
        return max * 1.05;
    }

    private double arenaHeight() {
        double max = 600;
        if (battle != null) {
            for (Ship s : battle.getShips()) {
                max = Math.max(max, s.getY());
            }
        }
        return max * 1.05;
    }

    // ---- rendering ------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            drawStars(g2);
            if (battle == null) {
                g2.setColor(UiTheme.TEXT_DIM);
                g2.drawString("No active battle.", 16, 24);
                return;
            }
            double sx = scaleX();
            double sy = scaleY();
            for (Projectile p : battle.getProjectiles()) {
                drawProjectile(g2, p, sx, sy);
            }
            for (Ship s : battle.getShips()) {
                if (!s.isDestroyed()) {
                    drawShip(g2, s, sx, sy);
                }
            }
            drawTargetReticle(g2, sx, sy);
            drawBattleHud(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawStars(Graphics2D g2) {
        // Deterministic starfield (no RNG): hashed dots so it stays put across frames.
        g2.setColor(new Color(40, 48, 78));
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        for (int i = 0; i < 120; i++) {
            int x = (i * 73 + 17) % w;
            int y = (i * 131 + 29) % h;
            g2.fillRect(x, y, 1, 1);
        }
    }

    private double facing(Ship s) {
        double[] prev = lastPos.get(s);
        double[] cur = new double[] { s.getX(), s.getY() };
        double angle = 0.0;
        if (prev != null) {
            double dx = cur[0] - prev[0];
            double dy = cur[1] - prev[1];
            if (dx * dx + dy * dy > 1e-4) {
                angle = Math.atan2(dy, dx);
            } else {
                Double cached = facingCache.get(s);
                angle = cached == null ? 0.0 : cached;
            }
        }
        lastPos.put(s, cur);
        facingCache.put(s, angle);
        return angle;
    }

    private final Map<Ship, Double> facingCache = new HashMap<Ship, Double>();

    private void drawShip(Graphics2D g2, Ship s, double sx, double sy) {
        int px = (int) (s.getX() * sx);
        int py = (int) (s.getY() * sy);
        double ang = facing(s);
        int r = 10;

        Graphics2D gs = (Graphics2D) g2.create();
        gs.translate(px, py);
        gs.rotate(ang);
        Color c = UiTheme.raceColor(s.getOwner());
        // Directional arrowhead sprite pointing along travel.
        int[] arrowX = { r, -r, -r };
        int[] arrowY = { 0, -r + 3, r - 3 };
        gs.setColor(c);
        gs.fillPolygon(arrowX, arrowY, 3);
        gs.setColor(c.brighter());
        gs.drawPolygon(arrowX, arrowY, 3);
        if (s.isCloaked()) {
            gs.setColor(new Color(180, 180, 255, 120));
            gs.drawOval(-r - 2, -r - 2, (r + 2) * 2, (r + 2) * 2);
        }
        gs.dispose();

        drawBars(g2, s, px, py - r - 8);
    }

    private void drawBars(Graphics2D g2, Ship s, int x, int y) {
        int w = 24;
        int h = 3;
        // Shield bar on top, hull bar below.
        double shieldPct = s.getMaxShields() <= 0 ? 0 : (double) s.getShields() / s.getMaxShields();
        double hullPct = s.getMaxHull() <= 0 ? 0 : (double) s.getHull() / s.getMaxHull();
        drawBar(g2, x - w / 2, y, w, h, shieldPct, UiTheme.SHIELD);
        drawBar(g2, x - w / 2, y + h + 1, w, h, hullPct, UiTheme.HULL);
    }

    private void drawBar(Graphics2D g2, int x, int y, int w, int h, double pct, Color fill) {
        pct = Math.max(0, Math.min(1, pct));
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(x, y, w, h);
        g2.setColor(fill);
        g2.fillRect(x, y, (int) (w * pct), h);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawRect(x, y, w, h);
    }

    private void drawProjectile(Graphics2D g2, Projectile p, double sx, double sy) {
        int px = (int) (p.getX() * sx);
        int py = (int) (p.getY() * sy);
        double vx = p.getVx();
        double vy = p.getVy();
        double len = Math.sqrt(vx * vx + vy * vy);
        double ux = len > 1e-6 ? vx / len : 1.0;
        double uy = len > 1e-6 ? vy / len : 0.0;

        if (p.isTorpedo()) {
            // Particle-style torpedo: glowing head + short fading arc trail.
            for (int i = 3; i >= 1; i--) {
                int tx = (int) (px - ux * i * 5);
                int ty = (int) (py - uy * i * 5);
                int alpha = 70 - i * 18;
                g2.setColor(new Color(180, 130, 255, Math.max(20, alpha)));
                int rr = 5 - i;
                g2.fillOval(tx - rr, ty - rr, rr * 2, rr * 2);
            }
            g2.setColor(UiTheme.TORPEDO);
            g2.fillOval(px - 3, py - 3, 6, 6);
            g2.setColor(Color.WHITE);
            g2.fillOval(px - 1, py - 1, 2, 2);
        } else {
            // Phaser beam: bright streak along the velocity vector.
            int bx = (int) (px - ux * 14);
            int by = (int) (py - uy * 14);
            g2.setColor(UiTheme.PHASER);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(bx, by, px, py);
            g2.setColor(new Color(255, 220, 200));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(bx, by, px, py);
        }
    }

    private void drawTargetReticle(Graphics2D g2, double sx, double sy) {
        if (targeted == null || targeted.isDestroyed()) {
            return;
        }
        int px = (int) (targeted.getX() * sx);
        int py = (int) (targeted.getY() * sy);
        g2.setColor(UiTheme.SELECT);
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawOval(px - 16, py - 16, 32, 32);
        g2.drawLine(px - 20, py, px - 12, py);
        g2.drawLine(px + 12, py, px + 20, py);
        g2.drawLine(px, py - 20, px, py - 12);
        g2.drawLine(px, py + 12, px, py + 20);
    }

    private void drawBattleHud(Graphics2D g2) {
        if (battle == null) {
            return;
        }
        g2.setColor(UiTheme.TEXT);
        if (battle.isFinished()) {
            String win = battle.getWinner() == null ? "Draw" : (battle.getWinner().name() + " wins");
            g2.drawString("Battle over — " + win, 12, 20);
        } else {
            int alive = 0;
            for (Ship s : battle.getShips()) {
                if (!s.isDestroyed()) {
                    alive++;
                }
            }
            g2.drawString("Live battle — ships in play: " + alive, 12, 20);
        }
        g2.setColor(UiTheme.TEXT_DIM);
        g2.drawString("Click a ship to target it.", 12, getHeight() - 10);
    }

    private void selectAt(int mx, int my) {
        if (battle == null) {
            return;
        }
        double sx = scaleX();
        double sy = scaleY();
        Ship best = null;
        double bestD = 22 * 22;
        for (Ship s : battle.getShips()) {
            if (s.isDestroyed()) {
                continue;
            }
            double dx = s.getX() * sx - mx;
            double dy = s.getY() * sy - my;
            double d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        targeted = best;
        repaint();
    }
}

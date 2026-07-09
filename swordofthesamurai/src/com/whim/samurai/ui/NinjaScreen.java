package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.engine.StealthEngine;
import com.whim.samurai.model.Province;
import com.whim.samurai.render.Palette;
import com.whim.samurai.render.SamuraiSprite;

import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The ninja / castle-infiltration MELEE variant (design ref §2c). A top-down stealth
 * mini-game: slip past patrolling guards to reach the target's room and assassinate
 * him before the alarm rouses the castle. Throw poisoned shuriken to drop a guard in
 * your path. Success is dishonourable — power up, honor down (design ref §3.3–§3.4).
 *
 * <p>All resolution lives in {@link StealthEngine}. Handles a null {@code ninjaTarget}
 * as a practice infiltration (hard-rule 4).</p>
 */
public class NinjaScreen extends Screen implements ActionListener {

    private static final int FPS_MS = 33;
    private static final double PLAYER_R = 10, GUARD_R = 11;
    private static final double VISION = 140;      // guard sight range
    private static final double CONTACT = 22;      // melee contact radius

    private final Timer timer = new Timer(FPS_MS, this);
    private final Set<Integer> held = new HashSet<Integer>();

    private StealthEngine engine;
    private Province target;

    private double px, py, facing = -Math.PI / 2;  // player pos + heading (up = toward target)
    private double targetX, targetY;
    private final List<Guard> guards = new ArrayList<Guard>();
    private boolean laidOut;
    private boolean resolved;
    private boolean success;
    private String banner;
    private int endDelay;

    public NinjaScreen(Game game) {
        super(game);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e)  { onKey(e.getKeyCode(), true); }
            public void keyReleased(KeyEvent e) { onKey(e.getKeyCode(), false); }
        });
    }

    public String name() { return Game.NINJA; }

    private static final class Guard {
        double x, y, dir;
        double ax, ay, bx, by;   // patrol endpoints
        boolean toB = true;
        boolean down;
        boolean alerted;
    }

    public void onShow() {
        held.clear();
        target = game.ninjaTarget;
        engine = new StealthEngine(game.state != null ? game.state.player : null, game.rng);
        laidOut = false;
        resolved = false;
        success = false;
        banner = null;
        endDelay = 0;
        timer.start();
        requestFocusInWindow();
    }

    private void ensureLayout() {
        if (laidOut || getWidth() <= 0) return;
        laidOut = true;
        int w = getWidth(), h = getHeight();
        px = w / 2.0; py = h - 90;                 // enter from the bottom (the door)
        targetX = w / 2.0; targetY = 110;          // objective on the top level (§2c)

        guards.clear();
        // A few patrols strung between the door and the target's room.
        addGuard(w * 0.30, h * 0.55, w * 0.70, h * 0.55);
        addGuard(w * 0.65, h * 0.35, w * 0.35, h * 0.35);
        addGuard(w * 0.50, h * 0.70, w * 0.50, h * 0.45);
    }

    private void addGuard(double ax, double ay, double bx, double by) {
        Guard g = new Guard();
        g.ax = ax; g.ay = ay; g.bx = bx; g.by = by;
        g.x = ax; g.y = ay;
        guards.add(g);
    }

    private void onKey(int code, boolean down) {
        if (down) held.add(code); else held.remove(code);
        if (!down) return;
        if (resolved) return;
        if (code == KeyEvent.VK_SPACE) throwShuriken();
        if (code == KeyEvent.VK_ESCAPE) abort();
    }

    // --- loop ----------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        ensureLayout();
        if (!resolved && laidOut) {
            movePlayer();
            moveGuards();
            sense();
            checkObjective();
        } else if (resolved && --endDelay <= 0) {
            leave();
            return;
        }
        repaint();
    }

    private void movePlayer() {
        double dx = 0, dy = 0, sp = engine.wounds >= 1 ? 1.7 : 3.2; // first wound halves speed (§2c)
        if (held.contains(KeyEvent.VK_LEFT))  dx -= 1;
        if (held.contains(KeyEvent.VK_RIGHT)) dx += 1;
        if (held.contains(KeyEvent.VK_UP))    dy -= 1;
        if (held.contains(KeyEvent.VK_DOWN))  dy += 1;
        if (dx != 0 || dy != 0) {
            double len = Math.hypot(dx, dy);
            px += dx / len * sp;
            py += dy / len * sp;
            facing = Math.atan2(dy, dx);
            px = clamp(px, 30, getWidth() - 30);
            py = clamp(py, 90, getHeight() - 60);
        }
    }

    private void moveGuards() {
        for (Guard g : guards) {
            if (g.down) continue;
            double tx = g.toB ? g.bx : g.ax, ty = g.toB ? g.by : g.ay;
            double dx = tx - g.x, dy = ty - g.y, d = Math.hypot(dx, dy);
            double sp = g.alerted ? 3.0 : 1.4;       // roused guards move faster (§2c)
            if (g.alerted) { tx = px; ty = py; dx = tx - g.x; dy = ty - g.y; d = Math.hypot(dx, dy); }
            if (d < 2) { g.toB = !g.toB; continue; }
            g.x += dx / d * sp; g.y += dy / d * sp;
            g.dir = Math.atan2(dy, dx);
        }
    }

    /** Guards see the player within range and their facing half-plane → alarm rises (§2c). */
    private void sense() {
        boolean watched = false;
        for (Guard g : guards) {
            if (g.down) { g.alerted = false; continue; }
            double dx = px - g.x, dy = py - g.y, d = Math.hypot(dx, dy);
            boolean inCone = d < VISION && (Math.cos(g.dir) * dx + Math.sin(g.dir) * dy) > -0.2 * d;
            g.alerted = inCone || engine.fullyAlarmed();
            if (inCone) { engine.seenTick(d); watched = true; }
            // contact once alarmed = a melee wound (design ref §2c)
            if (d < CONTACT && (engine.fullyAlarmed() || g.alerted)) {
                engine.takeWound();
                // knock the guard back so a single brush isn't instant death
                g.x -= dx / (d + 0.01) * 26; g.y -= dy / (d + 0.01) * 26;
                if (engine.down()) fail("You are cut down in the dark.");
            }
        }
        if (!watched) engine.calmTick();
    }

    private void throwShuriken() {
        if (!engine.canThrow()) return;
        boolean hit = engine.throwShuriken();
        if (!hit) return;
        // Drop the nearest guard roughly ahead of the throw (design ref §2c).
        Guard best = null; double bestD = 220;
        for (Guard g : guards) {
            if (g.down) continue;
            double dx = g.x - px, dy = g.y - py, d = Math.hypot(dx, dy);
            boolean ahead = (Math.cos(facing) * dx + Math.sin(facing) * dy) > 0;
            if (ahead && d < bestD) { bestD = d; best = g; }
        }
        if (best != null) best.down = true;
    }

    private void checkObjective() {
        if (Math.hypot(px - targetX, py - targetY) < 24) {
            success = true;
            resolve("The target falls silently. Slip away before dawn.");
        }
    }

    private void abort() {
        // Leaving before you are identified is a sanctioned exit (design ref §2c).
        if (engine.alarm < 40) {
            banner = "You withdraw unseen.";
            resolved = true; endDelay = 45; // no lasting state change
        } else {
            fail("You break off, but you were seen.");
        }
    }

    private void fail(String msg) { success = false; resolve(msg); }

    private void resolve(String msg) {
        if (resolved) return;
        resolved = true;
        banner = msg;
        endDelay = 60;
        engine.applyOutcome(game, target, success);
    }

    private void leave() {
        timer.stop();
        if (game.state != null && game.state.gameOver) game.screens.show(Game.GAMEOVER);
        else game.returnFromAction();
    }

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }

    // --- rendering -----------------------------------------------------------

    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());
        ensureLayout();

        // dim castle interior
        g.setColor(new Color(38, 34, 44));
        g.fillRect(0, 80, getWidth(), getHeight() - 140);

        // objective room (top level)
        g.setColor(Palette.CINNABAR_DK);
        g.fillRect((int) targetX - 40, (int) targetY - 34, 80, 60);
        g.setColor(Palette.GOLD);
        g.setFont(UiKit.SMALL);
        g.drawString(target != null ? "Target: " + target.name : "Target (practice)", (int) targetX - 38, (int) targetY - 40);
        g.setColor(Palette.CINNABAR);
        g.fillOval((int) targetX - 7, (int) targetY - 7, 14, 14);

        if (laidOut) {
            for (Guard gd : guards) {
                if (gd.down) {
                    g.setColor(Palette.INK_SOFT);
                    g.fillOval((int) gd.x - 6, (int) gd.y - 6, 12, 12); // fallen (a corpse can raise alarm, §2c)
                } else {
                    // faint vision hint
                    g.setColor(new Color(200, 70, 60, gd.alerted ? 60 : 26));
                    g.fillOval((int) (gd.x + Math.cos(gd.dir) * 40 - VISION / 2),
                               (int) (gd.y + Math.sin(gd.dir) * 40 - VISION / 2),
                               (int) VISION, (int) VISION);
                    SamuraiSprite.drawGuard(g, (int) gd.x, (int) gd.y, (int) GUARD_R, gd.alerted, gd.dir);
                }
            }
            SamuraiSprite.drawHero(g, (int) px, (int) py, (int) PLAYER_R, Palette.INDIGO, facing);
        }

        hud(g);
        if (resolved && banner != null) {
            g.setColor(new Color(20, 16, 12, 190));
            g.fillRect(0, getHeight() / 2 - 30, getWidth(), 60);
            g.setColor(Palette.PAPER);
            g.setFont(UiKit.HEAD);
            int tw = g.getFontMetrics().stringWidth(banner);
            g.drawString(banner, (getWidth() - tw) / 2, getHeight() / 2 + 6);
        }
    }

    private void hud(Graphics2D g) {
        g.setColor(Palette.INK);
        g.setFont(UiKit.TITLE);
        g.drawString("Shadows", 30, 60);

        // alarm bar (design ref §2c)
        int bx = getWidth() - 280, by = 28, bw = 240, bh = 16;
        g.setColor(Palette.PANEL);
        g.fillRect(bx, by, bw, bh);
        double frac = Math.max(0, Math.min(1, engine.alarm / StealthEngine.ALARM_MAX));
        g.setColor(engine.fullyAlarmed() ? Palette.CINNABAR : Palette.GOLD);
        g.fillRect(bx, by, (int) (bw * frac), bh);
        g.setColor(Palette.PANEL_LINE);
        g.drawRect(bx, by, bw, bh);
        g.setColor(Palette.INK);
        g.setFont(UiKit.SMALL);
        g.drawString("Alarm", bx, by - 2);

        // wounds + shuriken
        g.drawString("Wounds: " + engine.wounds + " / " + StealthEngine.MAX_WOUNDS, bx, by + 36);
        g.drawString("Shuriken: " + engine.shuriken, bx + 130, by + 36);

        g.setColor(Palette.INK_SOFT);
        g.drawString("Arrows move   SPACE throw shuriken   ESC withdraw   —   reach the target unseen",
                30, getHeight() - 18);
    }
}

package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.engine.DuelEngine;
import com.whim.samurai.model.Rival;
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
import java.util.HashSet;
import java.util.Set;

/**
 * The one-on-one katana DUEL (design ref §2a). A real-time-ish, side-view bout
 * driven by a Swing {@link Timer}: the player and opponent close, strike on three
 * lines (high / mid / low), hold a directional parry, or throw a slow but
 * unblockable charged over-the-shoulder cut. First to four wounds falls (§2a).
 *
 * <p>All rules live in {@link DuelEngine}; this screen only owns timing, input and
 * rendering. Handles a null {@code duelTarget} by staging a practice sparring bout
 * so the screen is runnable standalone (skeleton hard-rule 4).</p>
 */
public class DuelScreen extends Screen implements ActionListener {

    private static final int FPS_MS = 33;
    private static final double REACH = 96;        // strike distance between centres

    // Fighter states.
    private static final int IDLE = 0, WINDUP = 1, STRIKE = 2, RECOVER = 3, STAGGER = 4;

    private final Timer timer = new Timer(FPS_MS, this);
    private final Set<Integer> held = new HashSet<Integer>();

    private DuelEngine engine;
    private Rival target;
    private boolean toDeath;
    private String reason = "";

    private Fighter player, foe;
    private int foeDecideCd;
    private String banner;          // end-of-duel message
    private int endDelay;           // frames to hold the result before leaving
    private boolean resolved;

    public DuelScreen(Game game) {
        super(game);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e)  { onKey(e.getKeyCode(), true); }
            public void keyReleased(KeyEvent e) { onKey(e.getKeyCode(), false); }
        });
    }

    public String name() { return Game.DUEL; }

    /** A duelling figure with a small state machine (pose is derived for drawing). */
    private static final class Fighter {
        double x;
        int facing;              // +1 faces right
        int state = IDLE, t = 0;
        int line = DuelEngine.LINE_MID;
        boolean charged;
        int parry = -1;          // held parry side, -1 = none
        int lastLine = -1;       // for the opponent's parry read
        Color kimono, trim;
    }

    public void onShow() {
        held.clear();
        target = game.duelTarget;
        toDeath = game.duelToDeath;
        reason = (game.duelReason != null && !game.duelReason.isEmpty())
                ? game.duelReason
                : (target == null ? "A practice bout against your fencing master." : "Honour must be answered.");

        int mySkill = (game.state != null && game.state.player != null) ? game.state.player.swordsmanship : 10;
        // Null target → sparring partner near the player's own level (hard-rule 4).
        int foeSkill = target != null ? target.swordsmanship : Math.max(4, mySkill - 1);
        engine = new DuelEngine(mySkill, foeSkill, game.rng, 0);

        player = new Fighter();
        player.facing = +1;
        player.kimono = Palette.INDIGO;
        player.trim = Palette.GOLD;
        foe = new Fighter();
        foe.facing = -1;
        foe.kimono = Palette.CINNABAR_DK;
        foe.trim = Palette.INK;

        banner = null;
        endDelay = 0;
        resolved = false;
        foeDecideCd = 20;
        timer.start();
        requestFocusInWindow();
    }

    private double arenaLeft()  { return 120; }
    private double arenaRight() { return Math.max(360, getWidth() - 120); }
    private int groundY()       { return (int) (getHeight() * 0.74); }

    private void resetPositions() {
        player.x = arenaLeft() + 60;
        foe.x = arenaRight() - 60;
    }

    // --- input ---------------------------------------------------------------

    private void onKey(int code, boolean down) {
        if (down) held.add(code); else held.remove(code);
        if (!down || resolved) return;

        if (code == KeyEvent.VK_ESCAPE) { concede(); return; }
        if (canAct(player)) {
            switch (code) {
                case KeyEvent.VK_A: beginAttack(player, DuelEngine.LINE_HIGH, false); break;
                case KeyEvent.VK_S: beginAttack(player, DuelEngine.LINE_MID, false); break;
                case KeyEvent.VK_D: beginAttack(player, DuelEngine.LINE_LOW, false); break;
                case KeyEvent.VK_SPACE: beginAttack(player, DuelEngine.OVERHEAD, true); break;
                default: break;
            }
        }
    }

    private boolean canAct(Fighter f) { return f.state == IDLE; }

    private void beginAttack(Fighter f, int line, boolean charged) {
        f.state = WINDUP;
        f.line = line;
        f.charged = charged;
        f.parry = -1;
        // The charged over-shoulder cut has a long, exposed wind-up (design ref §2a).
        f.t = charged ? 14 : 4;
        if (f == player && line != DuelEngine.OVERHEAD) f.lastLine = line;
    }

    // --- main loop -----------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        if (getWidth() > 0 && player.x == 0 && foe.x == 0) resetPositions();
        if (!resolved) {
            updatePlayer();
            updateFoe();
            step(player, foe);
            step(foe, player);
            checkEnd();
        } else if (--endDelay <= 0) {
            leave();
            return;
        }
        repaint();
    }

    private void updatePlayer() {
        // Held-parry (only while idle) — you may parry left/mid/right (design ref §2a).
        if (player.state == IDLE) {
            if (held.contains(KeyEvent.VK_J)) player.parry = DuelEngine.LINE_HIGH;
            else if (held.contains(KeyEvent.VK_K)) player.parry = DuelEngine.LINE_MID;
            else if (held.contains(KeyEvent.VK_L)) player.parry = DuelEngine.LINE_LOW;
            else player.parry = -1;

            double sp = 3.2;
            if (held.contains(KeyEvent.VK_LEFT))  player.x -= sp;   // back away (§2a)
            if (held.contains(KeyEvent.VK_RIGHT)) player.x += sp;   // advance
            player.x = clamp(player.x, arenaLeft(), foe.x - 40);
        }
        player.facing = player.x <= foe.x ? +1 : -1;
    }

    private void updateFoe() {
        foe.facing = foe.x <= player.x ? +1 : -1;
        if (foe.state != IDLE) return;
        if (--foeDecideCd > 0) return;
        // Higher skill = faster reactions (design ref §2a, §4).
        foeDecideCd = Math.max(6, 26 - engine.foeSkill);

        double dist = Math.abs(foe.x - player.x);
        if (dist > REACH - 6) {
            foe.parry = -1;
            foe.x += (player.x > foe.x ? 1 : -1) * (2.4 + engine.foeSkill * 0.05); // close in
            foe.x = clamp(foe.x, player.x + 40, arenaRight());
        } else if (player.state == WINDUP || player.state == STRIKE) {
            foe.parry = engine.chooseFoeParry(player.lastLine); // read the incoming line
        } else if (game.rng.chance(0.6)) {
            int a = engine.chooseFoeAttack();
            beginAttack(foe, a, a == DuelEngine.OVERHEAD);
        } else {
            foe.parry = engine.chooseFoeParry(-1);
        }
    }

    /** Advance a fighter's state machine; resolve a hit at the wind-up→strike edge. */
    private void step(Fighter f, Fighter other) {
        if (f.state == IDLE) return;
        if (--f.t > 0) return;
        switch (f.state) {
            case WINDUP:
                f.state = STRIKE;
                f.t = f.charged ? 5 : 4;
                resolveHit(f, other);
                break;
            case STRIKE:
                f.state = RECOVER;
                f.t = f.charged ? 10 : 6;
                break;
            case STAGGER:
            case RECOVER:
            default:
                f.state = IDLE;
                f.t = 0;
        }
    }

    private void resolveHit(Fighter atk, Fighter def) {
        if (Math.abs(atk.x - def.x) > REACH) return;              // out of range, whiffs
        boolean attackerIsPlayer = (atk == player);
        int atkSkill = attackerIsPlayer ? engine.playerSkill : engine.foeSkill;
        int defSkill = attackerIsPlayer ? engine.foeSkill : engine.playerSkill;
        int defParry = def.state == IDLE ? def.parry : -1;        // can't parry mid-swing

        if (engine.lands(atk.line, atk.charged, defParry, atkSkill, defSkill)) {
            if (attackerIsPlayer) engine.woundFoe(atk.charged); else engine.woundPlayer(atk.charged);
            // A wound "knocks you back a couple of steps" and interrupts (design ref §2a).
            def.state = STAGGER;
            def.t = 12;
            def.parry = -1;
            def.x += (def.x < atk.x ? -1 : 1) * 26;
            def.x = clamp(def.x, arenaLeft(), arenaRight());
        }
    }

    private void checkEnd() {
        if (engine.foeDown()) finish(true);
        else if (engine.playerDown()) finish(false);
    }

    private void concede() {
        // Fleeing a duel is "very dishonorable" (design ref §2a); treat as a loss without a kill.
        toDeath = false;
        banner = "You turned and fled — a coward's dishonour.";
        engine.playerWounds = DuelEngine.MAX_WOUNDS;
        finish(false);
    }

    private void finish(boolean playerWon) {
        if (resolved) return;
        resolved = true;
        endDelay = 60;
        if (banner == null) {
            String who = target != null ? target.name : "your sparring partner";
            banner = playerWon
                    ? (toDeath ? "You cut down " + who + "!" : "First blood — you win the duel!")
                    : (toDeath ? "You have fallen. The Way is found in death." : "You are bested and shamed.");
        }
        engine.applyOutcome(game, target, toDeath, playerWon);
    }

    private void leave() {
        timer.stop();
        if (game.state != null && game.state.gameOver) game.screens.show(Game.GAMEOVER);
        else game.returnFromAction();
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // --- rendering -----------------------------------------------------------

    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        int gy = groundY();
        // dojo floor
        g.setColor(Palette.PANEL);
        g.fillRect(0, gy, getWidth(), getHeight() - gy);
        g.setColor(Palette.PANEL_LINE);
        g.drawLine(0, gy, getWidth(), gy);

        if (player.x == 0 && foe.x == 0) resetPositions();

        drawFighter(g, player, gy);
        drawFighter(g, foe, gy);

        hud(g);
        if (resolved && banner != null) centreBanner(g, banner);
    }

    private void drawFighter(Graphics2D g, Fighter f, int gy) {
        SamuraiSprite.drawDuelist(g, (int) f.x, gy, 1.0, f.facing, f.kimono, f.trim, pose(f));
    }

    private int pose(Fighter f) {
        if (f.state == STAGGER) return SamuraiSprite.POSE_STAGGER;
        if (f.state == WINDUP)  return f.charged ? SamuraiSprite.POSE_WINDUP : lineGuard(f.line);
        if (f.state == STRIKE) {
            if (f.charged || f.line == DuelEngine.LINE_HIGH || f.line == DuelEngine.OVERHEAD) return SamuraiSprite.POSE_STRIKE_HIGH;
            if (f.line == DuelEngine.LINE_LOW) return SamuraiSprite.POSE_STRIKE_LOW;
            return SamuraiSprite.POSE_STRIKE_MID;
        }
        if (f.parry >= 0) return SamuraiSprite.POSE_PARRY;
        return SamuraiSprite.POSE_GUARD_MID;
    }

    private int lineGuard(int line) {
        if (line == DuelEngine.LINE_HIGH) return SamuraiSprite.POSE_GUARD_HIGH;
        if (line == DuelEngine.LINE_LOW) return SamuraiSprite.POSE_GUARD_LOW;
        return SamuraiSprite.POSE_GUARD_MID;
    }

    private void hud(Graphics2D g) {
        g.setColor(Palette.INK);
        g.setFont(UiKit.HEAD);
        g.drawString("You", 24, 34);
        String foeName = target != null ? target.name : "Sparring Master";
        int fw = g.getFontMetrics().stringWidth(foeName);
        g.drawString(foeName, getWidth() - fw - 24, 34);

        drawWounds(g, 24, 44, engine.playerWounds, true);
        drawWounds(g, getWidth() - 24 - 4 * 26, 44, engine.foeWounds, false);

        g.setFont(UiKit.SMALL);
        g.setColor(Palette.INK_SOFT);
        g.drawString(reason, 24, 70);
        if (toDeath) {
            g.setColor(Palette.CINNABAR);
            g.drawString("A DUEL TO THE DEATH", 24, 88);
        }

        // control legend
        g.setColor(Palette.INK_SOFT);
        g.setFont(UiKit.SMALL);
        String legend = "←/→ move   A/S/D strike high/mid/low   SPACE charged (unblockable)   "
                + "J/K/L parry high/mid/low   ESC flee";
        g.drawString(legend, 24, getHeight() - 14);
    }

    private void drawWounds(Graphics2D g, int x, int y, int wounds, boolean player) {
        for (int i = 0; i < DuelEngine.MAX_WOUNDS; i++) {
            boolean hurt = i < wounds;
            g.setColor(hurt ? Palette.CINNABAR : Palette.PANEL);
            g.fillOval(x + i * 26, y, 20, 20);
            g.setColor(Palette.PANEL_LINE);
            g.drawOval(x + i * 26, y, 20, 20);
        }
    }

    private void centreBanner(Graphics2D g, String text) {
        g.setColor(new Color(20, 16, 12, 180));
        int bh = 70;
        g.fillRect(0, getHeight() / 2 - bh / 2, getWidth(), bh);
        g.setColor(Palette.PAPER);
        g.setFont(UiKit.HEAD);
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (getWidth() - tw) / 2, getHeight() / 2 + 6);
    }
}

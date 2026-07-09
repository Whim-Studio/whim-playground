package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.engine.BattleEngine;
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

/**
 * The open-field BATTLE (design ref §2b). Two phases: pick a named formation
 * (attack or defence, per which side you are), then issue a command each round —
 * advance / hold / charge / retreat — until one army routs. A readable tactical
 * mini-game, not a full RTS; all resolution lives in {@link BattleEngine}.
 *
 * <p>Handles a null {@code battleTarget} as a practice skirmish (hard-rule 4).</p>
 */
public class BattleScreen extends Screen implements ActionListener {

    private static final int FORMATION = 0, COMMAND = 1, RESULT = 2;

    private final Timer timer = new Timer(33, this);
    private BattleEngine engine;
    private Province target;
    private boolean attacking;

    private int phase;
    private int[] options;          // the three formation ids available this battle
    private int sel;
    private int animTick;
    private int round;
    private String banner;
    private int endDelay;

    public BattleScreen(Game game) {
        super(game);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { onKey(e.getKeyCode()); }
        });
    }

    public String name() { return Game.BATTLE; }

    public void onShow() {
        target = game.battleTarget;
        Integer pc = (game.state != null && game.state.playerClan() != null) ? game.state.playerClan().id : null;
        // Assaulting a foreign province = attacker; defending your own = defender (design ref §2b).
        attacking = target == null || pc == null || target.ownerClanId != pc;

        engine = new BattleEngine(game.state != null ? game.state.player : null, target, attacking, game.rng);
        options = attacking
                ? new int[]{BattleEngine.HOSHI, BattleEngine.KAKUYOKU, BattleEngine.KATANA}
                : new int[]{BattleEngine.GANKO, BattleEngine.KOYAKU, BattleEngine.ENGETSU};
        sel = 0;
        phase = FORMATION;
        round = 0;
        banner = null;
        endDelay = 0;
        timer.start();
        requestFocusInWindow();
    }

    private void onKey(int code) {
        if (phase == FORMATION) {
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W)   sel = (sel + options.length - 1) % options.length;
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) sel = (sel + 1) % options.length;
            if (code == KeyEvent.VK_ENTER) {
                engine.playerFormation = options[sel];
                phase = COMMAND;
            }
        } else if (phase == COMMAND) {
            int cmd = -1;
            switch (code) {
                case KeyEvent.VK_1: cmd = BattleEngine.ADVANCE; break;
                case KeyEvent.VK_2: cmd = BattleEngine.HOLD; break;
                case KeyEvent.VK_3: cmd = BattleEngine.CHARGE; break;
                case KeyEvent.VK_4: cmd = BattleEngine.RETREAT; break;
                default: break;
            }
            if (cmd >= 0) doRound(cmd);
        } else if (phase == RESULT) {
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_ESCAPE) leave();
        }
    }

    private void doRound(int cmd) {
        round++;
        engine.resolveRound(cmd);
        if (engine.isOver()) endBattle();
    }

    private void endBattle() {
        phase = RESULT;
        endDelay = 90;
        boolean won = engine.playerWon();
        engine.applyOutcome(game, target, won);
        String place = target != null ? target.name : "the field";
        banner = won ? "Victory at " + place + "!" : "Your army is broken at " + place + ".";
    }

    private void leave() {
        timer.stop();
        if (game.state != null && game.state.gameOver) game.screens.show(Game.GAMEOVER);
        else game.returnFromAction();
    }

    public void actionPerformed(ActionEvent e) {
        animTick++;
        if (phase == RESULT && --endDelay <= 0) { leave(); return; }
        repaint();
    }

    // --- rendering -----------------------------------------------------------

    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        // battlefield band
        g.setColor(Palette.MOSS);
        g.fillRect(0, 150, getWidth(), getHeight() - 260);

        g.setColor(Palette.INK);
        g.setFont(UiKit.TITLE);
        g.drawString("Battle", 40, 90);
        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK_SOFT);
        g.drawString(attacking ? "You are the attacker." : "You defend your land.", 40, 120);

        if (phase == FORMATION) drawFormationPick(g);
        else drawField(g);

        if (banner != null && phase == RESULT) {
            g.setColor(new Color(20, 16, 12, 190));
            g.fillRect(0, getHeight() / 2 - 34, getWidth(), 68);
            g.setColor(Palette.PAPER);
            g.setFont(UiKit.HEAD);
            int tw = g.getFontMetrics().stringWidth(banner);
            g.drawString(banner, (getWidth() - tw) / 2, getHeight() / 2 + 6);
            g.setFont(UiKit.SMALL);
            String hint = "Press ENTER to continue";
            g.drawString(hint, (getWidth() - g.getFontMetrics().stringWidth(hint)) / 2, getHeight() / 2 + 26);
        }
    }

    private void drawFormationPick(Graphics2D g) {
        g.setFont(UiKit.HEAD);
        g.setColor(Palette.INK);
        g.drawString("Choose your " + (attacking ? "attack" : "defence") + " formation:", 40, 190);

        for (int i = 0; i < options.length; i++) {
            int y = 220 + i * 40;
            boolean on = i == sel;
            g.setColor(on ? Palette.CINNABAR : Palette.INK_SOFT);
            g.setFont(on ? UiKit.HEAD : UiKit.BODY);
            g.drawString((on ? "▶ " : "   ") + BattleEngine.FORMATION_NAMES[options[i]], 60, y);
        }

        // schematic preview of the selected formation
        drawFormationPreview(g, options[sel], getWidth() - 320, 210, 260, 150);

        g.setColor(Palette.INK_SOFT);
        g.setFont(UiKit.SMALL);
        g.drawString("↑/↓ choose   ENTER commit", 40, getHeight() - 40);
        g.drawString("The enemy has already arrayed his lines; counter them wisely (design ref §2b).",
                40, getHeight() - 22);
    }

    /** A tiny block diagram of where the mass sits for each formation. */
    private void drawFormationPreview(Graphics2D g, int f, int x, int y, int w, int h) {
        g.setColor(Palette.PANEL);
        g.fillRect(x, y, w, h);
        g.setColor(Palette.PANEL_LINE);
        g.drawRect(x, y, w, h);
        int cx = x + w / 2, cy = y + h / 2;
        g.setColor(Palette.INDIGO);
        switch (f) {
            case BattleEngine.HOSHI:                          // arrowhead — concentrated centre point
                g.fillPolygon(new int[]{cx, cx + 40, cx - 40}, new int[]{y + 20, y + h - 30, y + h - 30}, 3);
                break;
            case BattleEngine.KAKUYOKU:                       // crane's wing — heavy flanks
                g.fillRect(x + 20, cy - 30, 40, 60);
                g.fillRect(x + w - 60, cy - 30, 40, 60);
                break;
            case BattleEngine.KATANA:                         // massed on one flank
                g.fillRect(x + 20, y + 20, 50, h - 40);
                break;
            case BattleEngine.GANKO:                          // strong centre, light flanks
                g.fillRect(cx - 25, y + 20, 50, h - 40);
                break;
            case BattleEngine.KOYAKU:                          // yoke — flanks forward
                g.fillRect(x + 20, y + 20, 36, 40);
                g.fillRect(x + w - 56, y + 20, 36, 40);
                g.fillRect(cx - 20, cy, 40, 40);
                break;
            case BattleEngine.ENGETSU:                         // half-moon on one flank
                g.fillArc(x + 20, y + 15, w - 40, h - 30, 90, 180);
                break;
            default: break;
        }
    }

    private void drawField(Graphics2D g) {
        // Two clan colours; player indigo, enemy cinnabar.
        Color pc = Palette.INDIGO, ec = Palette.CINNABAR;
        int march = attacking ? (animTick / 6) % 8 : 0;

        drawArmy(g, pc, 90 + march, 200, engine.playerStrength, SamuraiSprite.UNIT_INFANTRY, true);
        drawArmy(g, ec, getWidth() - 110, 200, engine.enemyStrength, SamuraiSprite.UNIT_INFANTRY, false);

        SamuraiSprite.drawBanner(g, 70, 190, pc, "You");
        SamuraiSprite.drawBanner(g, getWidth() - 90, 190, ec, target != null ? target.name : "Enemy");

        // strength + morale bars
        bar(g, 40, getHeight() - 150, "Your strength", engine.playerStrength, startStrength(engine, true), pc);
        bar(g, 40, getHeight() - 128, "Your morale", engine.playerMorale, 100, Palette.GOLD);
        bar(g, getWidth() - 300, getHeight() - 150, "Enemy strength", engine.enemyStrength, startStrength(engine, false), ec);
        bar(g, getWidth() - 300, getHeight() - 128, "Enemy morale", engine.enemyMorale, 100, Palette.GOLD);

        g.setColor(Palette.INK);
        g.setFont(UiKit.SMALL);
        g.drawString("Formation: " + BattleEngine.FORMATION_NAMES[engine.playerFormation]
                + "   vs   " + BattleEngine.FORMATION_NAMES[engine.enemyFormation]
                + "   (1 figure = " + engine.soldiersPerFigure + " men)", 40, 145);

        if (phase == COMMAND) {
            g.setColor(Palette.INK_SOFT);
            g.drawString("Round " + round + "   —   1 Advance   2 Hold   3 Charge   4 Retreat",
                    40, getHeight() - 40);
            g.drawString("You give destinations, not targets; charge trades hard, retreat risks a rout (design ref §2b).",
                    40, getHeight() - 22);
        }
    }

    /** ~APPROX baseline for the strength bar — recompute the initial figures. */
    private double startStrength(BattleEngine e, boolean player) {
        // The engine mutates strengths; use a generous cap so bars read sensibly.
        return player ? Math.max(e.playerStrength, 30) : Math.max(e.enemyStrength, 30);
    }

    private void drawArmy(Graphics2D g, Color c, int x, int y, double strength, int type, boolean facingRight) {
        int n = Math.max(1, Math.min(12, (int) Math.round(strength / 4.0)));
        for (int i = 0; i < n; i++) {
            int col = i % 4, row = i / 4;
            int gx = x + (facingRight ? col * 22 : -col * 22);
            int gy = y + row * 22;
            SamuraiSprite.drawSoldier(g, gx, gy, 8, c, type);
        }
    }

    private void bar(Graphics2D g, int x, int y, String label, double v, double max, Color c) {
        int w = 240, h = 14;
        g.setColor(Palette.PANEL);
        g.fillRect(x, y, w, h);
        double frac = max <= 0 ? 0 : Math.max(0, Math.min(1, v / max));
        g.setColor(c);
        g.fillRect(x, y, (int) (w * frac), h);
        g.setColor(Palette.PANEL_LINE);
        g.drawRect(x, y, w, h);
        g.setColor(Palette.INK);
        g.setFont(UiKit.SMALL);
        g.drawString(label, x, y - 2);
    }
}

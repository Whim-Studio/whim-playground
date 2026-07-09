package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.model.Rival;
import com.whim.samurai.model.Samurai;
import com.whim.samurai.render.Palette;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * The random-encounter dispatcher on the road (design ref §1.6, §2, §3.2). Short
 * dialogue with keyed choices: a bandit ambush (a bold deed — fight alone for
 * honour), a rival's insult (challenge him → launches a DUEL), or a roadside event
 * (a shrine, a beggar). Light outcomes, then back to the map.
 *
 * <p>Reads world state; degrades gracefully when {@code game.state} is null so the
 * screen runs standalone (hard-rule 4).</p>
 */
public class EncounterScreen extends Screen {

    private static final int BANDITS = 0, INSULT = 1, ROADSIDE = 2;
    private static final int CHOOSING = 0, RESULT = 1;

    private int kind;
    private int phase;
    private String title = "";
    private String[] body = new String[0];
    private String[] choices = new String[0];
    private String result = "";
    private Rival insulter;

    public EncounterScreen(Game game) {
        super(game);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { onKey(e.getKeyCode()); }
        });
    }

    public String name() { return Game.ENCOUNTER; }

    public void onShow() {
        phase = CHOOSING;
        result = "";
        insulter = null;
        kind = game.rng.range(0, 2);
        switch (kind) {
            case BANDITS: setupBandits(); break;
            case INSULT:  setupInsult();  break;
            default:      setupRoadside(); break;
        }
        requestFocusInWindow();
    }

    private void setupBandits() {
        title = "Bandits on the Road";
        body = new String[]{
            "A gang of brigands blocks the mountain path, blades drawn.",
            "To face them single-handed is the samurai's road to honour (design ref §3.2)."
        };
        choices = new String[]{"1  Draw your sword and fight them alone", "2  Spur past and ride on"};
    }

    private void setupInsult() {
        title = "An Insult";
        insulter = pickRival();
        String who = insulter != null ? insulter.name : "A haughty samurai";
        body = new String[]{
            who + " mocks your family before the assembled lords.",
            "Honour must be answered — or swallowed at a cost (design ref §3.2–§3.3)."
        };
        choices = new String[]{"1  Challenge him to a duel", "2  Swallow the insult"};
    }

    private void setupRoadside() {
        title = "A Wayside Shrine";
        body = new String[]{
            "A weathered shrine stands beside the road, its offering box empty.",
            "A small kindness is the mark of an honourable lord (design ref §3.2)."
        };
        choices = new String[]{"1  Leave an offering of rice", "2  Bow and continue"};
    }

    private Rival pickRival() {
        if (game.state == null) return null;
        int clanId = game.state.liegeClanId;
        List<Rival> pool = game.state.livingRivalsInClan(clanId);
        if (pool.isEmpty()) pool = game.state.rivals;
        for (Rival r : pool) if (r.alive) return r;
        return null;
    }

    private void onKey(int code) {
        if (phase == RESULT) {
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_ESCAPE) game.returnFromAction();
            return;
        }
        if (code == KeyEvent.VK_1) choose(0);
        else if (code == KeyEvent.VK_2) choose(1);
    }

    private void choose(int idx) {
        Samurai me = game.state != null ? game.state.player : null;
        switch (kind) {
            case BANDITS:
                if (idx == 0) fightBandits(me); else resultText("You ride on. Nothing is gained or lost.");
                break;
            case INSULT:
                if (idx == 0) launchDuel(); else swallowInsult(me);
                break;
            default:
                if (idx == 0) offer(me); else resultText("You bow respectfully and continue on your way.");
        }
    }

    private void fightBandits(Samurai me) {
        int skill = me != null ? me.swordsmanship : 8;
        // A bold deed resolved as a quick brawl (a full fight would use the melee engine).
        boolean win = game.rng.chance(0.55 + skill * 0.02);
        if (win) {
            if (me != null) { me.honor += game.rng.range(8, 16); game.state.dynastyScore += 15; }
            resultText("You cut the brigands down single-handed — your honour rises.");
        } else {
            if (me != null) me.koku = Math.max(0, me.koku - game.rng.range(10, 30));
            resultText("They overwhelm you and take some rice before scattering.");
        }
    }

    private void swallowInsult(Samurai me) {
        // Backing down from an insult loses honour (design ref §3.3).
        if (me != null) me.honor = Math.max(0, me.honor - game.rng.range(6, 12));
        resultText("You let the insult pass. Your honour suffers for it.");
    }

    private void launchDuel() {
        // Hand off to the DUEL engine via Game fields + card name (design ref §2a, ARCHITECTURE §4).
        game.duelTarget = insulter;
        game.duelToDeath = false;
        game.duelReason = "You were insulted and demanded satisfaction.";
        game.afterAction = Game.MAP;
        game.screens.show(Game.DUEL);
    }

    private void offer(Samurai me) {
        if (me != null && me.koku >= 10) {
            me.koku -= 10;
            me.honor += game.rng.range(3, 7);
            resultText("You leave an offering. The priest blesses your house.");
        } else {
            resultText("Your purse is too light to give. You bow and move on.");
        }
    }

    private void resultText(String msg) {
        result = msg;
        phase = RESULT;
        repaint();
    }

    // --- rendering -----------------------------------------------------------

    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        g.setColor(Palette.INK);
        g.setFont(UiKit.TITLE);
        g.drawString("Encounter", 60, 90);

        g.setColor(Palette.CINNABAR);
        g.setFont(UiKit.HEAD);
        g.drawString(title, 60, 150);

        g.setColor(Palette.INK_SOFT);
        g.setFont(UiKit.BODY);
        int y = 190;
        for (String line : body) { g.drawString(line, 60, y); y += 28; }

        if (phase == CHOOSING) {
            g.setColor(Palette.INK);
            g.setFont(UiKit.HEAD);
            y += 20;
            for (String c : choices) { g.drawString(c, 80, y); y += 36; }
            g.setColor(Palette.DIM);
            g.setFont(UiKit.SMALL);
            g.drawString("Press the number of your choice.", 60, getHeight() - 30);
        } else {
            g.setColor(Palette.INK);
            g.setFont(UiKit.BODY);
            g.drawString(result, 60, y + 30);
            g.setColor(Palette.DIM);
            g.setFont(UiKit.SMALL);
            g.drawString("Press ENTER to return to the map.", 60, getHeight() - 30);
        }
    }
}

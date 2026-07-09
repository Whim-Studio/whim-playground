package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.engine.PoliticsEngine;
import com.whim.samurai.engine.TurnEngine;
import com.whim.samurai.model.Clan;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Province;
import com.whim.samurai.model.Rival;
import com.whim.samurai.model.Samurai;
import com.whim.samurai.render.Palette;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.List;

/**
 * The strategic hub (design ref §1, §6, §8): a stylised procedurally-drawn map of
 * the 48 provinces of Sengoku Japan coloured by owning clan, a status HUD, and
 * the per-season "Home Options" (design ref §1.5) offered as buttons + key hints.
 *
 * All map art is drawn with Java2D — no assets from the original (see README).
 * Rules live in the engines (TurnEngine / PoliticsEngine); this screen only reads
 * {@link GameState} and launches action sequences through the {@link Game}
 * hand-off contract (ARCHITECTURE.md §4).
 */
public class MapScreen extends Screen {

    // Map canvas rectangle — MUST match WorldGen.MAP_* so province coords land here.
    private static final int MAP_X0 = 40, MAP_Y0 = 150, MAP_W = 560, MAP_H = 300;
    // HUD panel on the right.
    private static final int HUD_X = 620, HUD_Y = 110, HUD_W = 246, HUD_H = 360;

    private static final Font TINY = new Font("Serif", Font.PLAIN, 10);

    private String statusLog = "Your spymaster brings the morning's news.";

    public MapScreen(Game game) {
        super(game);
        setLayout(null);
        buildControls();
    }

    public String name() { return Game.MAP; }

    @Override
    public void onShow() {
        if (game.state == null) return;
        if (game.state.gameOver) { game.screens.show(Game.GAMEOVER); return; }
        // A returning action (duel/battle/…) may have ended the game via its engine.
        PoliticsEngine.checkVictory(game.state);
        PoliticsEngine.checkDefeat(game.state);
        if (game.state.gameOver) { game.screens.show(Game.GAMEOVER); return; }
        repaint();
    }

    // ------------------------------------------------------------------ controls
    private void buildControls() {
        // Row 1 — honourable / economic season actions.
        addBtn("Improve Estate (E)", 30, 490, 150, "E", new Runnable() { public void run() { improveEstate(); } });
        addBtn("Raise Troops (R)",  190, 490, 150, "R", new Runnable() { public void run() { raiseTroops(); } });
        addBtn("Patrol / Hunt (P)", 350, 490, 150, "P", new Runnable() { public void run() { patrol(); } });
        addBtn("Attend Daimyo (A)", 510, 490, 170, "A", new Runnable() { public void run() { attendDaimyo(); } });
        // Row 2 — martial actions + the clock.
        addBtn("Duel Rival (D)",     30, 535, 150, "D", new Runnable() { public void run() { duelRival(); } });
        addBtn("Besiege (B)",       190, 535, 150, "B", new Runnable() { public void run() { besiege(); } });
        addBtn("Infiltrate (I)",    350, 535, 150, "I", new Runnable() { public void run() { infiltrate(); } });
        addBtn("End Season (Spc)",  510, 535, 170, "SPACE", new Runnable() { public void run() { endSeason(); } });
        // Row 3 — navigation to the other screens.
        addBtn("Character (C)",      30, 580, 150, "C", new Runnable() { public void run() { game.screens.show(Game.CHARACTER); } });
        addBtn("Family (F)",        190, 580, 150, "F", new Runnable() { public void run() { game.screens.show(Game.FAMILY); } });
        addBtn("Help (H)",          350, 580, 150, "H", new Runnable() { public void run() { game.screens.show(Game.HELP); } });
    }

    private void addBtn(String text, int x, int y, int w, String key, final Runnable action) {
        JButton b = UiKit.button(text);
        b.setFont(UiKit.BODY);
        b.setBounds(x, y, w, 36);
        b.setFocusable(false);
        b.addActionListener(e -> action.run());
        add(b);
        Keys.bind(this, key, action);
    }

    // ------------------------------------------------------------------ actions
    private Samurai you() { return game.state.player; }
    private int playerClanId() { return game.state.playerClan().id; }
    private Province seat() { return game.state.province(game.state.currentProvinceId); }

    /** Improve Estate — raise development/rice of your seat, a small power gain (design ref §1.5). */
    private void improveEstate() {
        Province p = seat();
        if (p == null) return;
        if (p.development < 5) p.development++;
        p.rice += 8;
        you().power += 2;
        statusLog = "You improve " + p.name + " — development " + p.development + ", rice " + p.rice + ".";
        repaint();
    }

    /** Equip Samurai — spend koku to field more retainers; capped by rice wealth (design ref §1.5, §4.2). */
    private void raiseTroops() {
        Samurai you = you();
        if (you.koku >= 50) {
            you.koku -= 50;
            you.power += 6;
            statusLog = "You equip more retainers (+6 power, -50 koku).";
        } else {
            statusLog = "Your granaries are too thin to raise more troops (need 50 koku).";
        }
        repaint();
    }

    /** Patrol / Hunt Bandits — a chance to fall into a melee encounter (design ref §2c, §3.2). */
    private void patrol() {
        if (game.rng.chance(0.6)) {
            game.afterAction = Game.MAP;
            game.screens.show(Game.ENCOUNTER);
        } else {
            you().honor += 2;
            statusLog = "The roads are quiet; grateful peasants speak well of you (+2 honor).";
            repaint();
        }
    }

    /** Attend the Daimyo — volunteer for a bold deed: honour, and sometimes a fight (design ref §6.1). */
    private void attendDaimyo() {
        if (game.rng.chance(0.5)) {
            statusLog = "The daimyo dispatches you against a bandit outrage.";
            game.afterAction = Game.MAP;
            game.screens.show(Game.ENCOUNTER);
        } else {
            you().honor += 4;
            statusLog = "You attend the daimyo's court; your loyalty is noted (+4 honor).";
            repaint();
        }
    }

    /** Duel a Rival — challenge the most hostile living clansman (design ref §2a, §6.2). */
    private void duelRival() {
        List<Rival> rivals = game.state.livingRivalsInClan(playerClanId());
        if (rivals.isEmpty()) {
            statusLog = "No rivals remain within your clan to challenge.";
            repaint();
            return;
        }
        Rival target = rivals.get(0);
        for (Rival r : rivals) if (r.hostility > target.hostility) target = r;
        game.duelTarget = target;
        game.duelReason = "A matter of honor between clansmen — you cross swords with " + target.name + ".";
        game.duelToDeath = false;
        game.afterAction = Game.MAP;
        game.screens.show(Game.DUEL);
    }

    /** Besiege an adjacent province not held by your clan (design ref §2b, §6.4). */
    private void besiege() {
        Province target = adjacentNotOwned(true);
        if (target == null) target = adjacentNotOwned(false);
        if (target == null) {
            statusLog = "No adjacent enemy province borders you here.";
            repaint();
            return;
        }
        game.battleTarget = target;
        game.afterAction = Game.MAP;
        game.screens.show(Game.BATTLE);
    }

    /** Infiltrate a castle in an adjacent rival province (design ref §2c). */
    private void infiltrate() {
        Province target = adjacentNotOwned(true);
        if (target == null) {
            statusLog = "No adjacent daimyo's castle within reach to infiltrate.";
            repaint();
            return;
        }
        game.ninjaTarget = target;
        game.afterAction = Game.MAP;
        game.screens.show(Game.NINJA);
    }

    /**
     * Pick a neighbour of the current province that your clan does not own.
     * When {@code rivalsOnly} is set, only foreign daimyo (not the unaligned
     * belt) qualify — used for infiltration/assault targeting.
     */
    private Province adjacentNotOwned(boolean rivalsOnly) {
        Province cur = seat();
        if (cur == null) return null;
        int pc = playerClanId();
        for (int nid : cur.neighbors) {
            Province np = game.state.province(nid);
            if (np == null || np.ownerClanId == pc) continue;
            Clan owner = game.state.clan(np.ownerClanId);
            boolean neutral = owner != null && "Independent".equals(owner.name);
            if (rivalsOnly && neutral) continue;
            return np;
        }
        return null;
    }

    /** End Season — advance the clock, run AI, then re-check rank and the endgame. */
    private void endSeason() {
        GameState s = game.state;
        String log = TurnEngine.advanceSeason(s, game.rng);
        if (PoliticsEngine.checkPromotion(s)) {
            log += "  You are raised to " + s.player.rank.title + "!";
        }
        PoliticsEngine.checkVictory(s);
        PoliticsEngine.checkDefeat(s);
        statusLog = log;
        if (s.gameOver) { game.screens.show(Game.GAMEOVER); return; }
        repaint();
    }

    // ------------------------------------------------------------------ render
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        if (game.state == null) {
            g.setColor(Palette.DIM);
            g.setFont(UiKit.BODY);
            g.drawString("No game in progress.", 60, 120);
            return;
        }

        drawHeader(g);
        drawMap(g);
        drawHud(g);
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(Palette.INK);
        g.setFont(UiKit.TITLE);
        g.drawString("The Provinces", 30, 66);
        g.setFont(UiKit.HEAD);
        g.setColor(Palette.CINNABAR_DK);
        g.drawString(game.state.calendar.label(), 30, 96);
        // placeholder-art disclaimer (all map art is procedural)
        g.setFont(TINY);
        g.setColor(Palette.DIM);
        g.drawString("[ stylised procedural map — no original assets ]", 30, MAP_Y0 + MAP_H + 24);
    }

    private void drawMap(Graphics2D g) {
        GameState s = game.state;

        // Sea-frame around the map region.
        g.setColor(new Color(198, 206, 200));
        g.fillRect(MAP_X0 - 10, MAP_Y0 - 10, MAP_W + 20, MAP_H + 20);
        g.setColor(Palette.PANEL_LINE);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(MAP_X0 - 10, MAP_Y0 - 10, MAP_W + 20, MAP_H + 20);

        // Adjacency lines (drawn once per undirected pair, beneath the nodes).
        Stroke thin = new BasicStroke(1f);
        g.setStroke(thin);
        g.setColor(new Color(96, 82, 64, 90));
        for (Province p : s.provinces) {
            for (int nid : p.neighbors) {
                if (nid <= p.id) continue;
                Province q = s.province(nid);
                if (q != null) g.drawLine(p.x, p.y, q.x, q.y);
            }
        }

        // Province nodes.
        for (Province p : s.provinces) {
            Color fill = provinceColor(s, p);
            g.setColor(fill);
            g.fillOval(p.x - 8, p.y - 8, 16, 16);
            g.setColor(Palette.INK);
            g.setStroke(new BasicStroke(1.2f));
            g.drawOval(p.x - 8, p.y - 8, 16, 16);
            // name label
            g.setFont(TINY);
            g.setColor(Palette.INK_SOFT);
            int w = g.getFontMetrics().stringWidth(p.name);
            g.drawString(p.name, p.x - w / 2, p.y + 20);
        }

        // Marker on the player's current province.
        Province cur = seat();
        if (cur != null) {
            g.setStroke(new BasicStroke(2.5f));
            g.setColor(Palette.GOLD);
            g.drawOval(cur.x - 13, cur.y - 13, 26, 26);
            g.setColor(Palette.CINNABAR);
            int[] xs = { cur.x, cur.x + 6, cur.x, cur.x - 6 };
            int[] ys = { cur.y - 6, cur.y, cur.y + 6, cur.y };
            g.fillPolygon(xs, ys, 4);
        }
    }

    private Color provinceColor(GameState s, Province p) {
        Clan owner = s.clan(p.ownerClanId);
        if (owner == null) return Palette.DIM;
        if ("Independent".equals(owner.name)) return new Color(150, 142, 128); // gray = unaligned
        int idx = owner.colorIndex % Palette.CLAN.length;
        return Palette.CLAN[idx];
    }

    private void drawHud(Graphics2D g) {
        GameState s = game.state;
        Samurai you = s.player;

        g.setColor(Palette.PANEL);
        g.fillRect(HUD_X, HUD_Y, HUD_W, HUD_H);
        g.setColor(Palette.PANEL_LINE);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(HUD_X, HUD_Y, HUD_W, HUD_H);

        int x = HUD_X + 14, y = HUD_Y + 26;
        g.setColor(Palette.INK);
        g.setFont(UiKit.HEAD);
        g.drawString(you.name, x, y);
        y += 22;

        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK_SOFT);
        Clan pc = s.playerClan();
        Clan liege = s.clan(s.liegeClanId);
        y = line(g, x, y, "Rank:   " + you.rank.title);
        y = line(g, x, y, "Clan:   " + (pc != null ? pc.name : you.clanName));
        y = line(g, x, y, "Liege:  " + (liege != null ? liege.daimyoName : "—"));
        Province cur = seat();
        y = line(g, x, y, "At:     " + (cur != null ? cur.name : "—"));
        y = line(g, x, y, "Age:    " + you.age);
        y += 6;

        y = stat(g, x, y, "Honor", you.honor, Palette.GOLD);
        y = stat(g, x, y, "Power", you.power, Palette.INDIGO);
        y = stat(g, x, y, "Koku", you.koku, Palette.MOSS);
        y += 4;
        g.setColor(Palette.DIM);
        g.setFont(UiKit.SMALL);
        y = line(g, x, y, "Sword " + you.swordsmanship
                + "  Gen " + you.generalship + "  Stealth " + you.stealth);
        int held = s.provinceCountFor(pc != null ? pc.id : -1);
        y = line(g, x, y, "Clan provinces: " + held + " / 48  (Shogun @ 24 + Omi)");
        y += 8;

        // clan colour legend
        g.setFont(UiKit.SMALL);
        g.setColor(Palette.INK_SOFT);
        y = line(g, x, y, "Houses:");
        for (Clan c : s.clans) {
            Color sw = "Independent".equals(c.name)
                    ? new Color(150, 142, 128)
                    : Palette.CLAN[c.colorIndex % Palette.CLAN.length];
            g.setColor(sw);
            g.fillRect(x, y - 10, 12, 12);
            g.setColor(Palette.INK_SOFT);
            g.drawString(c.name + (c.isPlayer ? " (you)" : ""), x + 18, y);
            y += 16;
        }

        // status log, wrapped inside the HUD width
        y += 4;
        g.setColor(Palette.CINNABAR_DK);
        g.setFont(UiKit.SMALL);
        drawWrapped(g, statusLog, x, y, HUD_W - 28);
    }

    private int line(Graphics2D g, int x, int y, String text) {
        g.drawString(text, x, y);
        return y + 20;
    }

    private int stat(Graphics2D g, int x, int y, String label, int value, Color c) {
        g.setColor(c);
        g.setFont(UiKit.BODY);
        g.drawString(label + ":", x, y);
        g.setColor(Palette.INK);
        g.drawString(String.valueOf(value), x + 70, y);
        return y + 20;
    }

    private void drawWrapped(Graphics2D g, String text, int x, int y, int maxW) {
        String[] words = text.split(" ");
        StringBuilder ln = new StringBuilder();
        for (String w : words) {
            String trial = ln.length() == 0 ? w : ln + " " + w;
            if (g.getFontMetrics().stringWidth(trial) > maxW) {
                g.drawString(ln.toString(), x, y);
                y += 16;
                ln = new StringBuilder(w);
            } else {
                ln = new StringBuilder(trial);
            }
        }
        if (ln.length() > 0) g.drawString(ln.toString(), x, y);
    }
}

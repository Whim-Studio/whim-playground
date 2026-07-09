package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.engine.HonorEngine;
import com.whim.samurai.model.Samurai;
import com.whim.samurai.render.Palette;

import javax.swing.JButton;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Character sheet (design ref §1.7 / §3.1) — the player's name, rank, the two
 * reputation axes shown with descriptive bands from {@link HonorEngine}, the three
 * arcade skills, wealth, fiefs held, age and dynasty generation. Back to the map.
 */
public class CharacterScreen extends Screen {

    public CharacterScreen(Game game) {
        super(game);
        setLayout(null);
        JButton back = UiKit.button("Back to Map  (Esc)");
        back.setBounds(60, 470, 220, 44);
        back.addActionListener(e -> game.screens.show(Game.MAP));
        add(back);
        Keys.bind(this, "ESCAPE", () -> game.screens.show(Game.MAP));
    }

    public String name() { return Game.CHARACTER; }

    @Override public void onShow() { repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        g.setColor(Palette.INK);
        g.setFont(UiKit.TITLE);
        g.drawString("The Samurai", 60, 80);

        Samurai p = (game.state != null) ? game.state.player : null;
        if (p == null) {
            g.setFont(UiKit.BODY);
            g.setColor(Palette.DIM);
            g.drawString("No character — begin a new game.", 62, 120);
            return;
        }

        int x = 62, y = 130;
        g.setFont(UiKit.HEAD);
        g.setColor(Palette.INK);
        g.drawString(p.name + "  of the " + p.clanName + " family", x, y);
        y += 28;
        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK_SOFT);
        g.drawString("Rank: " + p.rank.title + "        Age: " + p.age
                + "        Generation: " + game.state.generation, x, y);

        // Two reputation axes with bands (design ref §3).
        y += 44;
        drawAxis(g, x, y, "HONOUR", p.honor, Palette.GOLD,
                HonorEngine.honorBand(p.honor), HonorEngine.honorConsequence(p.honor));
        y += 92;
        drawAxis(g, x, y, "POWER", p.power, Palette.INDIGO,
                HonorEngine.powerBand(p.power), HonorEngine.powerConsequence(p.power));

        // Skills + holdings block (right column).
        int cx = getWidth() - 340, cy = 130;
        g.setColor(Palette.PANEL);
        g.fillRect(cx, cy, 290, 260);
        g.setColor(Palette.PANEL_LINE);
        g.drawRect(cx, cy, 290, 260);
        g.setFont(UiKit.HEAD);
        g.setColor(Palette.CINNABAR_DK);
        g.drawString("Skills & Holdings", cx + 16, cy + 30);

        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK);
        int ry = cy + 62;
        g.drawString("Swordsmanship:  " + p.swordsmanship + " / 20", cx + 16, ry); ry += 26;
        g.drawString("Generalship:    " + p.generalship + " / 20", cx + 16, ry); ry += 26;
        g.drawString("Stealth:        " + p.stealth + " / 20", cx + 16, ry); ry += 34;
        g.drawString("Koku (rice):    " + p.koku, cx + 16, ry); ry += 26;
        g.drawString("Fiefs held:     " + p.fiefs.size(), cx + 16, ry); ry += 26;
        g.drawString("Married:        " + (p.isMarried() ? p.wife.name : "no"), cx + 16, ry); ry += 26;
        g.drawString("Heir:           " + (p.hasHeir() ? p.heir().name : "none yet"), cx + 16, ry);
    }

    /** Draw one reputation axis: a labelled bar with value, band and consequence line. */
    private void drawAxis(Graphics2D g, int x, int y, String label, int value,
                          java.awt.Color color, String bandName, String consequence) {
        g.setFont(UiKit.HEAD);
        g.setColor(Palette.INK);
        g.drawString(label + ": " + bandName + "  (" + value + ")", x, y);

        int barY = y + 12, barW = 420, barH = 16;
        g.setColor(Palette.PAPER_DK);
        g.fillRect(x, barY, barW, barH);
        // ~0..800 working range (matches HonorEngine banding).
        int fill = (int) (barW * Math.min(1.0, value / 800.0));
        g.setColor(color);
        g.fillRect(x, barY, fill, barH);
        g.setColor(Palette.PANEL_LINE);
        g.drawRect(x, barY, barW, barH);

        g.setFont(UiKit.SMALL);
        g.setColor(Palette.INK_SOFT);
        g.drawString(consequence, x, barY + barH + 20);
    }
}

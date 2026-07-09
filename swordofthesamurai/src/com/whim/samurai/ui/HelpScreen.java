package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.render.Palette;

import javax.swing.JButton;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * "Bushido — How to Play": a concise rules primer drawn from the design reference
 * (goal, the Honor vs Power spine, the action sub-games, and succession). Back to
 * the main menu with the button or Esc.
 */
public class HelpScreen extends Screen {

    private static final String[] LINES = {
        "GOAL — Rise from gokenin (vassal samurai) to Shogun of all Japan. Climb",
        "Samurai → Hatamoto → Daimyo → Shogun by balancing two things at once:  (design ref §1)",
        "",
        "HONOUR (the Way of bushido) — won in duels, bold deeds, defending your fief,",
        "marrying up and donating to temples. Lost by cowardice, over-taxing, or being",
        "caught in treachery. High honour wins the daimyo's favour and promotion.  (§3.2/§3.3)",
        "",
        "POWER (land, armies, koku) — grown by campaigns, taxes, and — as daimyo —",
        "conquering provinces. Treachery and assassination raise power but risk your",
        "honour and, if caught, an ordered seppuku.  (§3.4)",
        "",
        "THE SWORD — Three action sub-games decide your fate in the moment:  (§2)",
        "  • Duel  — one-on-one katana combat, to four wounds.",
        "  • Melee — one hero against many (raids, rescues, stealth), to two wounds.",
        "  • Battle — command units of foot, horse, archers and muskets in the field.",
        "",
        "FAMILY & DYNASTY — Take a wife and raise sons; your first-born is your heir.",
        "When you die, an heir carries on the quest — dying heir-less ends the line.  (§5)",
        "",
        "CONTROLS — Buttons drive the strategic hub; Esc backs out of a screen. Save and",
        "Continue use a single scroll (save slot) from the main menu.",
    };

    public HelpScreen(Game game) {
        super(game);
        setLayout(null);
        JButton back = UiKit.button("Back  (Esc)");
        back.setBounds(60, 520, 180, 44);
        back.addActionListener(e -> game.screens.show(Game.MENU));
        add(back);
        Keys.bind(this, "ESCAPE", () -> game.screens.show(Game.MENU));
    }

    public String name() { return Game.HELP; }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        g.setColor(Palette.INK);
        g.setFont(UiKit.TITLE);
        g.drawString("Bushido — How to Play", 60, 76);

        g.setFont(UiKit.BODY);
        int y = 112;
        for (String line : LINES) {
            if (line.startsWith("GOAL") || line.startsWith("HONOUR") || line.startsWith("POWER")
                    || line.startsWith("THE SWORD") || line.startsWith("FAMILY") || line.startsWith("CONTROLS")) {
                g.setColor(Palette.CINNABAR_DK);
            } else {
                g.setColor(Palette.INK_SOFT);
            }
            g.drawString(line, 62, y);
            y += 22;
        }
    }
}

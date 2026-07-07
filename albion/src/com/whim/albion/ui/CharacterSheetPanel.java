package com.whim.albion.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import com.whim.albion.api.GameController;
import com.whim.albion.api.Views.CharacterView;
import com.whim.albion.api.Views.PartyView;
import com.whim.albion.api.Views.SpellView;
import com.whim.albion.api.Enums.SkillType;
import com.whim.albion.api.Enums.StatType;

/** Character sheet overlay: portrait, vitals, the 8 stats, 4 skills, and known spells. */
final class CharacterSheetPanel extends JPanel {

    private final GameController controller;

    CharacterSheetPanel(GameController controller) {
        this.controller = controller;
        setBackground(new Color(22, 24, 30));
    }

    private CharacterView active() {
        PartyView pv = controller.state().party();
        if (pv == null || pv.members().isEmpty()) return null;
        int i = Math.max(0, Math.min(pv.activeIndex(), pv.members().size() - 1));
        return pv.members().get(i);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        CharacterView c = active();
        g.setColor(new Color(232, 220, 160));
        g.setFont(UiUtil.UI_BOLD.deriveFont(18f));
        g.drawString("Character Sheet    (Esc / C to close · click portraits to switch)", 20, 30);
        if (c == null) return;

        SpriteFactory.drawPortrait(g, c.portraitKey(), 20, 46, 110, 132);
        g.setColor(UiUtil.INK);
        g.setFont(UiUtil.UI_BOLD.deriveFont(16f));
        g.drawString(c.name(), 150, 62);
        g.setFont(UiUtil.UI_FONT);
        g.drawString(c.profession() + "   Level " + c.level(), 150, 82);
        g.drawString("XP " + c.xp() + " / " + c.xpToNext(), 150, 100);
        UiUtil.bar(g, 150, 108, 200, 12, c.lp(), c.maxLp(), UiUtil.LP_COLOR, "LP");
        UiUtil.bar(g, 150, 124, 200, 12, c.sp(), c.maxSp(), UiUtil.SP_COLOR, "SP");

        // stats
        int sy = 210;
        g.setColor(new Color(210, 200, 170));
        g.setFont(UiUtil.UI_BOLD);
        g.drawString("Attributes", 20, sy - 6);
        g.setFont(UiUtil.UI_FONT);
        StatType[] stats = StatType.values();
        for (int i = 0; i < stats.length; i++) {
            int col = i / 4, row = i % 4;
            int x = 20 + col * 200, y = sy + 16 + row * 20;
            g.setColor(new Color(180, 172, 150));
            g.drawString(pretty(stats[i].name()), x, y);
            g.setColor(UiUtil.INK);
            g.drawString(String.valueOf(c.stat(stats[i])), x + 150, y);
        }

        // skills
        int ky = sy + 120;
        g.setColor(new Color(210, 200, 170));
        g.setFont(UiUtil.UI_BOLD);
        g.drawString("Skills", 20, ky - 6);
        g.setFont(UiUtil.UI_FONT);
        SkillType[] skills = SkillType.values();
        for (int i = 0; i < skills.length; i++) {
            int y = ky + 16 + i * 20;
            g.setColor(new Color(180, 172, 150));
            g.drawString(pretty(skills[i].name()), 20, y);
            g.setColor(UiUtil.INK);
            g.drawString(c.skill(skills[i]) + "%", 170, y);
        }

        // spells
        int spx = 320, spy = ky;
        g.setColor(new Color(210, 200, 170));
        g.setFont(UiUtil.UI_BOLD);
        g.drawString("Spells", spx, spy - 6);
        g.setFont(UiUtil.UI_FONT);
        List<SpellView> spells = c.spells();
        if (spells.isEmpty()) {
            g.setColor(new Color(150, 140, 120));
            g.drawString("(no spells known)", spx, spy + 16);
        }
        for (int i = 0; i < spells.size(); i++) {
            SpellView s = spells.get(i);
            int y = spy + 16 + i * 20;
            g.setColor(s.castable() ? UiUtil.INK : new Color(140, 130, 120));
            g.drawString(s.name() + " [" + s.school() + "]  " + s.spCost() + " SP", spx, y);
        }
    }

    private static String pretty(String enumName) {
        String s = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(640, 560); }
}

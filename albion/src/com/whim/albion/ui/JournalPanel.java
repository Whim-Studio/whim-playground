package com.whim.albion.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import com.whim.albion.api.GameController;
import com.whim.albion.api.Views.JournalView;
import com.whim.albion.api.Views.QuestEntryView;
import com.whim.albion.api.Enums.QuestStatus;

/** Quest journal overlay: each quest with its status and objective checklist. */
final class JournalPanel extends JPanel {

    private final GameController controller;

    JournalPanel(GameController controller) {
        this.controller = controller;
        setBackground(new Color(26, 24, 22));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(232, 220, 160));
        g.setFont(UiUtil.UI_BOLD.deriveFont(18f));
        g.drawString("Journal    (Esc / J to close)", 20, 34);

        JournalView jv = controller.state().journal();
        if (jv == null) return;
        List<QuestEntryView> quests = jv.quests();
        int y = 70;
        for (QuestEntryView q : quests) {
            g.setColor(statusColor(q.status()));
            g.setFont(UiUtil.UI_BOLD.deriveFont(15f));
            g.drawString("• " + q.title() + "  [" + q.status() + "]", 24, y);
            y += 20;
            g.setColor(UiUtil.INK);
            g.setFont(UiUtil.UI_FONT);
            for (String obj : q.objectives()) {
                g.drawString("    " + obj, 40, y);
                y += 18;
            }
            y += 12;
        }
        if (quests.isEmpty()) {
            g.setColor(new Color(150, 140, 120));
            g.drawString("Your journal is empty.", 24, 70);
        }
    }

    private static Color statusColor(QuestStatus s) {
        switch (s) {
            case COMPLETED: return new Color(120, 200, 120);
            case FAILED:    return new Color(200, 100, 100);
            default:        return new Color(230, 210, 140);
        }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(640, 560); }
}

package com.whim.albion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.whim.albion.api.GameController;
import com.whim.albion.api.Views.CombatView;
import com.whim.albion.api.Views.CombatantView;
import com.whim.albion.api.Enums.CombatActionType;

/**
 * Turn-based tactical combat screen: a battlefield grid with combatants (from {@link CombatView}),
 * a row of action buttons for {@link CombatView#availableActions()}, click-to-target, and a
 * scrolling combat log. Actions route through {@link GameController#combatAction}.
 */
final class CombatPanel extends JPanel {

    private final GameController controller;
    private final Field field;
    private final JPanel actionBar;
    private int selectedTarget = -1;
    private CombatActionType pendingAction = CombatActionType.ATTACK;

    CombatPanel(GameController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        field = new Field();
        actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        actionBar.setBackground(new Color(20, 18, 22));
        add(field, BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);
    }

    /** Rebuild the action buttons from the current combat view (called on state change). */
    void refresh() {
        actionBar.removeAll();
        CombatView cv = controller.state().combat();
        if (cv == null || cv.finished()) { actionBar.revalidate(); actionBar.repaint(); return; }
        for (final CombatActionType a : cv.availableActions()) {
            JButton b = new JButton(label(a));
            b.setFocusable(false);
            b.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    pendingAction = a;
                    if (a == CombatActionType.DEFEND || a == CombatActionType.FLEE) {
                        controller.combatAction(a, -1, null);
                    } else if (a == CombatActionType.CAST) {
                        controller.combatAction(a, selectedTarget, "spell.spark");
                    } else if (a == CombatActionType.ITEM) {
                        controller.combatAction(a, selectedTarget, "potion.heal");
                    } else {
                        controller.combatAction(a, selectedTarget, null);
                    }
                    selectedTarget = -1;
                }
            });
            actionBar.add(b);
        }
        actionBar.revalidate();
        actionBar.repaint();
        field.repaint();
    }

    private static String label(CombatActionType a) {
        switch (a) {
            case ATTACK: return "Attack";
            case CAST:   return "Cast";
            case ITEM:   return "Item";
            case MOVE:   return "Move";
            case DEFEND: return "Defend";
            case FLEE:   return "Flee";
            default:     return a.name();
        }
    }

    private final class Field extends JPanel {
        Field() {
            setBackground(new Color(18, 16, 20));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { pickTarget(e); }
            });
        }

        private void pickTarget(MouseEvent e) {
            CombatView cv = controller.state().combat();
            if (cv == null) return;
            int[] geo = geometry(cv);
            int cell = geo[0], ox = geo[1], oy = geo[2];
            List<CombatantView> cs = cv.combatants();
            for (int i = 0; i < cs.size(); i++) {
                CombatantView c = cs.get(i);
                int px = ox + c.gridX() * cell, py = oy + c.gridY() * cell;
                if (e.getX() >= px && e.getX() < px + cell && e.getY() >= py && e.getY() < py + cell) {
                    selectedTarget = i;
                    repaint();
                    return;
                }
            }
        }

        private int[] geometry(CombatView cv) {
            int logH = 90;
            int usableH = getHeight() - logH;
            int cw = getWidth() / Math.max(1, cv.cols());
            int ch = usableH / Math.max(1, cv.rows());
            int cell = Math.max(24, Math.min(cw, ch));
            int ox = (getWidth() - cell * cv.cols()) / 2;
            int oy = 10;
            return new int[]{cell, ox, oy};
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            CombatView cv = controller.state().combat();
            if (cv == null) return;
            int[] geo = geometry(cv);
            int cell = geo[0], ox = geo[1], oy = geo[2];

            // grid
            for (int y = 0; y < cv.rows(); y++) {
                for (int x = 0; x < cv.cols(); x++) {
                    boolean enemyRows = y < cv.rows() / 2;
                    g.setColor(enemyRows ? new Color(46, 34, 34) : new Color(34, 40, 46));
                    g.fillRect(ox + x * cell, oy + y * cell, cell, cell);
                    g.setColor(new Color(0, 0, 0, 60));
                    g.drawRect(ox + x * cell, oy + y * cell, cell, cell);
                }
            }

            // combatants
            List<CombatantView> cs = cv.combatants();
            for (int i = 0; i < cs.size(); i++) {
                CombatantView c = cs.get(i);
                if (!c.alive()) continue;
                int px = ox + c.gridX() * cell, py = oy + c.gridY() * cell;
                SpriteFactory.drawActor(g, c.spriteKey(), px, py, cell, cell, !c.playerSide());
                if (c.current()) {
                    g.setColor(new Color(240, 220, 100));
                    g.drawRect(px + 1, py + 1, cell - 3, cell - 3);
                    g.drawRect(px + 2, py + 2, cell - 5, cell - 5);
                }
                if (i == selectedTarget) {
                    g.setColor(new Color(230, 90, 90));
                    g.drawOval(px + 2, py + 2, cell - 4, cell - 4);
                }
                UiUtil.bar(g, px + 2, py + cell - 8, cell - 4, 5, c.lp(), c.maxLp(), UiUtil.LP_COLOR, null);
                g.setColor(UiUtil.INK);
                g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 9));
                g.drawString(c.name(), px, py - 1);
            }

            // combat log
            int logTop = getHeight() - 88;
            g.setColor(new Color(0, 0, 0, 170));
            g.fillRect(0, logTop, getWidth(), 88);
            g.setColor(UiUtil.PANEL_EDGE);
            g.drawLine(0, logTop, getWidth(), logTop);
            g.setColor(UiUtil.INK);
            g.setFont(UiUtil.UI_FONT);
            List<String> log = cv.log();
            int ly = logTop + 14;
            for (String line : log) { g.drawString(line, 8, ly); ly += 14; }

            if (cv.finished()) {
                g.setColor(new Color(240, 220, 120));
                g.setFont(UiUtil.TITLE_FONT);
                g.drawString(cv.victory() ? "Victory!" : "Defeat", getWidth() / 2 - 90, getHeight() / 2);
            }
        }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(640, 560); }
}

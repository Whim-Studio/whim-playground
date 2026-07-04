package com.whim.swd6.ui;

import com.whim.swd6.api.RollResult;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Paints a strip of six-sided dice faces for a {@link RollResult}: the normal dice
 * plus the Wild Die chain (highlighted; exploded 6s and complication 1s colored).
 * All drawn with {@link Graphics2D} — no images.
 *
 * Owned by Task 3 (ui).
 */
public final class DiceStripView extends JPanel {

    private RollResult result;
    private static final int DIE = 40;
    private static final int GAP = 10;

    public DiceStripView() {
        setBackground(Palette.SPACE_DEEP);
        setPreferredSize(new Dimension(400, DIE + 24));
    }

    public void setResult(RollResult r) {
        this.result = r;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int count = 1;
        if (result != null) {
            count = Math.max(1, result.getNormalDice().size() + result.getWildDieRolls().size());
        }
        int w = count * (DIE + GAP) + GAP + 60;
        return new Dimension(w, DIE + 24);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = GAP;
        int y = 8;
        if (result == null) {
            g2.setColor(Palette.TEXT_FAINT);
            g2.setFont(Palette.BODY);
            g2.drawString("Roll the dice to see the faces here.", x, y + DIE / 2);
            g2.dispose();
            return;
        }
        for (Integer v : result.getNormalDice()) {
            drawDie(g2, x, y, v, Palette.SPACE_RAISED, Palette.TEXT, false);
            x += DIE + GAP;
        }
        List<Integer> wild = result.getWildDieRolls();
        for (int i = 0; i < wild.size(); i++) {
            int v = wild.get(i);
            Color face = Palette.blend(Palette.AMBER_DIM, Palette.SPACE_RAISED, 0.35f);
            Color pip = Palette.SPACE_DEEP;
            if (v == 1) {
                face = Palette.DANGER;
            } else if (v == 6) {
                face = Palette.AMBER;
            }
            drawDie(g2, x, y, v, face, pip, true);
            x += DIE + GAP;
        }
        g2.dispose();
    }

    private void drawDie(Graphics2D g2, int x, int y, int value, Color face, Color pip, boolean wild) {
        g2.setColor(face);
        g2.fillRoundRect(x, y, DIE, DIE, 10, 10);
        g2.setColor(wild ? Palette.AMBER : Palette.GRID_LINE);
        g2.setStroke(wild ? Palette.FRAME : Palette.HAIRLINE);
        g2.drawRoundRect(x, y, DIE, DIE, 10, 10);
        g2.setColor(pip);
        drawPips(g2, x, y, value);
        if (wild) {
            g2.setColor(Palette.alpha(Palette.SPACE_DEEP, 200));
            g2.setFont(Palette.SMALL);
            g2.drawString("W", x + 3, y + 12);
        }
    }

    private void drawPips(Graphics2D g2, int x, int y, int value) {
        int r = 5;
        int cx = x + DIE / 2;
        int cy = y + DIE / 2;
        int off = DIE / 4;
        int lx = x + off, mx = cx, rx = x + DIE - off;
        int ty = y + off, my = cy, by = y + DIE - off;
        boolean[] p = pipMap(value);
        int[][] pos = {
                {lx, ty}, {rx, ty},
                {lx, my}, {mx, my}, {rx, my},
                {lx, by}, {rx, by}
        };
        for (int i = 0; i < 7; i++) {
            if (p[i]) {
                g2.fillOval(pos[i][0] - r, pos[i][1] - r, r * 2, r * 2);
            }
        }
    }

    private boolean[] pipMap(int v) {
        // slots: TL, TR, ML, MM, MR, BL, BR
        boolean[] a = new boolean[7];
        switch (v) {
            case 1: a[3] = true; break;
            case 2: a[0] = true; a[6] = true; break;
            case 3: a[0] = true; a[3] = true; a[6] = true; break;
            case 4: a[0] = true; a[1] = true; a[5] = true; a[6] = true; break;
            case 5: a[0] = true; a[1] = true; a[3] = true; a[5] = true; a[6] = true; break;
            case 6: a[0] = true; a[1] = true; a[2] = true; a[4] = true; a[5] = true; a[6] = true; break;
            default: break;
        }
        return a;
    }
}

package com.whim.alganon.ui.screens;

import com.whim.alganon.api.GameController;
import com.whim.alganon.api.Views;
import com.whim.alganon.ui.SoundHooks;
import com.whim.alganon.ui.UiTheme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

/** The title menu: a painted banner + New Game / Load / Settings / Quit driven by menuOptions(). */
public final class TitleScreen extends JPanel {

    private final GameController controller;
    private int hover = -1;
    private Rectangle[] hits = new Rectangle[0];

    public TitleScreen(GameController controller) {
        this.controller = controller;
        setBackground(UiTheme.BG);
        setPreferredSize(new Dimension(1100, 720));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { click(e); }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int h = index(e); if (h != hover) { hover = h; repaint(); }
            }
        });
    }

    private int index(MouseEvent e) {
        for (int i = 0; i < hits.length; i++) if (hits[i] != null && hits[i].contains(e.getPoint())) return i;
        return -1;
    }

    private void click(MouseEvent e) {
        int i = index(e);
        if (i >= 0) { SoundHooks.get().play(SoundHooks.Cue.UI_CLICK); controller.selectMenuOption(i); }
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiTheme.aa(g);
        int w = getWidth(), h = getHeight();

        g.setPaint(new GradientPaint(0, 0, new Color(0x1A, 0x14, 0x24), 0, h, new Color(0x0C, 0x0A, 0x12)));
        g.fillRect(0, 0, w, h);
        drawStars(g, w, h);

        // title
        g.setFont(UiTheme.FONT_TITLE);
        String title = "ALGANON";
        int tw = g.getFontMetrics().stringWidth(title);
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(title, (w - tw) / 2 + 3, 173);
        g.setColor(UiTheme.ACCENT);
        g.drawString(title, (w - tw) / 2, 170);
        g.setFont(UiTheme.FONT_H2);
        String sub = "A single-player recreation";
        g.setColor(UiTheme.TEXT_DIM);
        g.drawString(sub, (w - g.getFontMetrics().stringWidth(sub)) / 2, 202);

        // menu
        Views.GameStateView v = controller.state();
        List<String> opts = v.menuOptions();
        hits = new Rectangle[opts.size()];
        int bw = 320, bh = 52, x = (w - bw) / 2, y = 290;
        for (int i = 0; i < opts.size(); i++) {
            Rectangle r = new Rectangle(x, y + i * (bh + 14), bw, bh);
            hits[i] = r;
            boolean hv = i == hover;
            g.setColor(hv ? UiTheme.PANEL_LIGHT : UiTheme.PANEL);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            g.setColor(hv ? UiTheme.ACCENT : UiTheme.BORDER);
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 12, 12);
            g.setFont(UiTheme.FONT_H1);
            g.setColor(hv ? UiTheme.ACCENT_HOT : UiTheme.TEXT);
            String s = opts.get(i);
            g.drawString(s, r.x + (bw - g.getFontMetrics().stringWidth(s)) / 2, r.y + 34);
        }

        String toast = v.toastMessage();
        if (toast != null && !toast.isEmpty()) {
            g.setFont(UiTheme.FONT_BODY);
            g.setColor(UiTheme.TEXT_DIM);
            g.drawString(toast, (w - g.getFontMetrics().stringWidth(toast)) / 2, y + opts.size() * (bh + 14) + 30);
        }

        g.setFont(UiTheme.FONT_SMALL);
        g.setColor(UiTheme.TEXT_FAINT);
        String foot = "Clean-room fan recreation • procedural graphics • Java 8 / Swing";
        g.drawString(foot, (w - g.getFontMetrics().stringWidth(foot)) / 2, h - 24);
    }

    private void drawStars(Graphics2D g, int w, int h) {
        java.util.Random rng = new java.util.Random(1234);
        for (int i = 0; i < 140; i++) {
            int sx = rng.nextInt(w), sy = rng.nextInt(h);
            int a = 40 + rng.nextInt(120);
            g.setColor(new Color(200, 190, 230, a));
            int sz = rng.nextInt(2) + 1;
            g.fillOval(sx, sy, sz, sz);
        }
    }
}

package com.whim.populous.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.whim.populous.api.ActionResult;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.GameController;
import com.whim.populous.api.Views.GameStateView;

/**
 * Bottom HUD: a mana meter, Good vs Evil population, the tick/status line, and a
 * row of procedurally-drawn Divine Power icons. Clicking an icon arms a targeted
 * power ({@link GameController#selectPower}) or fires a global one immediately
 * ({@link GameController#castGlobal}). Unaffordable powers are greyed; the armed
 * power is ringed.
 */
public class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GameController controller;
    private final GodPower[] powers = GodPower.values();
    private final List<Rectangle> iconBounds = new ArrayList<Rectangle>();
    private int hoverIcon = -1;

    private static final int PAD = 12;
    private static final int ICON = 44;
    private static final int ICON_GAP = 8;
    private static final int BAR_H = 18;

    public DashboardPanel(GameController controller) {
        this.controller = controller;
        setBackground(UiColors.HUD_BG);
        setPreferredSize(new Dimension(640, 150));
        MouseHandler h = new MouseHandler();
        addMouseListener(h);
        addMouseMotionListener(h);
    }

    @Override
    protected void paintComponent(Graphics gRaw) {
        super.paintComponent(gRaw);
        Graphics2D g = (Graphics2D) gRaw;
        Renderer.hints(g);
        GameStateView st = controller.state();

        int w = getWidth();
        int y = PAD;

        // --- title / status line ---
        g.setFont(getFont().deriveFont(Font.BOLD, 13f));
        g.setColor(UiColors.HUD_TEXT);
        String status = st.statusLine() != null ? st.statusLine() : "";
        g.drawString("POPULOUS   tick " + st.tick() + "   " + status, PAD, y + 12);
        y += 24;

        // --- mana bar (good mana vs maxMana) ---
        int barW = w - PAD * 2;
        drawMeter(g, PAD, y, barW, BAR_H, st.goodMana(), st.maxMana(),
                UiColors.MANA, UiColors.MANA_BG, "MANA " + st.goodMana() + " / " + st.maxMana());
        y += BAR_H + 8;

        // --- population counts (Good blue vs Evil red) ---
        drawPopBar(g, PAD, y, barW, 14, st.goodPopulation(), st.evilPopulation(),
                st.populationCap());
        y += 14 + 10;

        // --- divine power icon row ---
        iconBounds.clear();
        int x = PAD;
        GodPower armed = st.selectedPower();
        for (int i = 0; i < powers.length; i++) {
            GodPower p = powers[i];
            Rectangle r = new Rectangle(x, y, ICON, ICON);
            iconBounds.add(r);
            boolean affordable = st.powerAffordable(p);
            boolean isArmed = (armed == p);
            drawPowerIcon(g, p, r, affordable, isArmed, i == hoverIcon);
            x += ICON + ICON_GAP;
            if (x + ICON > w - PAD) { // wrap to a second row if narrow
                x = PAD;
                y += ICON + ICON_GAP;
            }
        }
    }

    private void drawMeter(Graphics2D g, int x, int y, int w, int h,
                           int val, int max, Color fill, Color bg, String label) {
        g.setColor(bg);
        g.fillRect(x, y, w, h);
        double f = max > 0 ? Math.max(0.0, Math.min(1.0, val / (double) max)) : 0.0;
        g.setColor(fill);
        g.fillRect(x, y, (int) Math.round(w * f), h);
        g.setColor(UiColors.HUD_BORDER);
        g.drawRect(x, y, w, h);
        g.setColor(UiColors.HUD_TEXT);
        g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        g.drawString(label, x + 6, y + h - 5);
    }

    /** A diverging bar: Good grows from the left, Evil from the right. */
    private void drawPopBar(Graphics2D g, int x, int y, int w, int h,
                            int good, int evil, int cap) {
        g.setColor(UiColors.HUD_PANEL);
        g.fillRect(x, y, w, h);
        int gwFull = (int) Math.round((w / 2.0) * Math.min(1.0, good / (double) Math.max(1, cap)));
        int ewFull = (int) Math.round((w / 2.0) * Math.min(1.0, evil / (double) Math.max(1, cap)));
        int mid = x + w / 2;
        g.setColor(UiColors.GOOD);
        g.fillRect(mid - gwFull, y, gwFull, h);
        g.setColor(UiColors.EVIL);
        g.fillRect(mid, y, ewFull, h);
        g.setColor(UiColors.HUD_BORDER);
        g.drawRect(x, y, w, h);
        g.drawLine(mid, y, mid, y + h);
        g.setColor(UiColors.HUD_TEXT);
        g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        g.drawString("GOOD " + good, x + 6, y + h - 3);
        String es = "EVIL " + evil;
        g.drawString(es, x + w - 6 - g.getFontMetrics().stringWidth(es), y + h - 3);
    }

    private void drawPowerIcon(Graphics2D g, GodPower p, Rectangle r,
                               boolean affordable, boolean armed, boolean hover) {
        // background tile
        g.setColor(hover ? UiColors.HUD_BORDER : UiColors.HUD_PANEL);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(armed ? UiColors.HIGHLIGHT : UiColors.HUD_BORDER);
        g.setStroke(new BasicStroke(armed ? 2.5f : 1f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setStroke(new BasicStroke(1f));

        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2 - 2;
        Color ink = affordable ? UiColors.HUD_TEXT : UiColors.HUD_TEXT_DIM;
        drawGlyph(g, p, cx, cy, ink);

        // label + cost
        g.setFont(getFont().deriveFont(Font.PLAIN, 9f));
        g.setColor(affordable ? UiColors.HUD_TEXT : UiColors.HUD_TEXT_DIM);
        String cost = p.manaCost() == 0 ? "free" : String.valueOf(p.manaCost());
        int cw = g.getFontMetrics().stringWidth(cost);
        g.drawString(cost, cx - cw / 2, r.y + r.height - 3);

        if (!affordable) {
            g.setColor(UiColors.withAlpha(UiColors.HUD_BG, 120));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }
    }

    /** Distinct procedural glyph per power so the row is readable without text. */
    private void drawGlyph(Graphics2D g, GodPower p, int cx, int cy, Color ink) {
        g.setColor(ink);
        g.setStroke(new BasicStroke(2f));
        switch (p) {
            case RAISE_LAND: {
                Polygon up = new Polygon();
                up.addPoint(cx, cy - 9); up.addPoint(cx - 8, cy + 6); up.addPoint(cx + 8, cy + 6);
                g.drawPolygon(up);
                g.drawLine(cx, cy - 9, cx, cy + 2);
                break;
            }
            case LOWER_LAND: {
                Polygon dn = new Polygon();
                dn.addPoint(cx, cy + 9); dn.addPoint(cx - 8, cy - 6); dn.addPoint(cx + 8, cy - 6);
                g.drawPolygon(dn);
                break;
            }
            case PAPAL_MAGNET: {
                g.drawOval(cx - 7, cy - 7, 14, 14);
                g.drawLine(cx, cy - 9, cx, cy + 9);
                g.drawLine(cx - 9, cy, cx + 9, cy);
                break;
            }
            case EARTHQUAKE: {
                int[] xs = {cx - 9, cx - 3, cx + 2, cx + 9};
                int[] ys = {cy - 6, cy + 6, cy - 6, cy + 6};
                g.drawPolyline(xs, ys, 4);
                break;
            }
            case SWAMP: {
                g.setColor(UiColors.SWAMP.brighter());
                g.fillOval(cx - 8, cy - 3, 16, 9);
                g.setColor(ink);
                g.drawOval(cx - 8, cy - 3, 16, 9);
                g.drawLine(cx - 3, cy - 6, cx - 3, cy + 1);
                g.drawLine(cx + 3, cy - 7, cx + 3, cy);
                break;
            }
            case VOLCANO: {
                Polygon v = new Polygon();
                v.addPoint(cx - 9, cy + 7); v.addPoint(cx - 3, cy - 7);
                v.addPoint(cx + 3, cy - 7); v.addPoint(cx + 9, cy + 7);
                g.setColor(UiColors.ROCK);
                g.fillPolygon(v);
                g.setColor(UiColors.LAVA);
                g.fillRect(cx - 3, cy - 9, 6, 5);
                g.setColor(ink);
                g.drawPolygon(v);
                break;
            }
            case FLOOD: {
                g.setColor(UiColors.SHALLOW);
                for (int i = -1; i <= 1; i++) {
                    int yy = cy + i * 5;
                    g.drawArc(cx - 9, yy - 3, 9, 6, 0, 180);
                    g.drawArc(cx, yy - 3, 9, 6, 180, 180);
                }
                g.setColor(ink);
                break;
            }
            case ARMAGEDDON: {
                g.setColor(UiColors.LAVA);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4.0;
                    g.drawLine(cx, cy, cx + (int) (Math.cos(a) * 9), cy + (int) (Math.sin(a) * 9));
                }
                g.setColor(UiColors.LAVA_HOT);
                g.fillOval(cx - 3, cy - 3, 6, 6);
                g.setColor(ink);
                break;
            }
            default:
                g.drawRect(cx - 6, cy - 6, 12, 12);
                break;
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void activate(int idx) {
        if (idx < 0 || idx >= powers.length) {
            return;
        }
        GodPower p = powers[idx];
        if (!controller.state().powerAffordable(p)) {
            return;
        }
        if (p.targeted()) {
            controller.selectPower(p);
        } else {
            ActionResult r = controller.castGlobal(p);
            // result surfaced through the map flash / status line on next repaint
            if (r != null) { /* no-op: state change triggers repaint */ }
        }
        repaint();
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            for (int i = 0; i < iconBounds.size(); i++) {
                if (iconBounds.get(i).contains(e.getPoint())) {
                    activate(i);
                    return;
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            int prev = hoverIcon;
            hoverIcon = -1;
            for (int i = 0; i < iconBounds.size(); i++) {
                if (iconBounds.get(i).contains(e.getPoint())) {
                    hoverIcon = i;
                    break;
                }
            }
            if (prev != hoverIcon) {
                repaint();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverIcon != -1) {
                hoverIcon = -1;
                repaint();
            }
        }
    }
}

package com.whim.kenshi.ui;

import com.whim.kenshi.api.Enums;
import com.whim.kenshi.api.GameController;
import com.whim.kenshi.api.Views;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The heads-up display: a top bar (pause / speed controls, world clock and the
 * selected unit's faction reputation), a bottom-centre strip of clickable squad
 * portrait chips, and a scrolling event log. All three are plain custom-painted
 * {@link JPanel}s that read {@link Views} snapshots and drive the
 * {@link GameController}.
 */
public final class Hud {

    private final GameController controller;
    private final TopBar topBar;
    private final Portraits portraits;
    private final EventLog eventLog;

    public Hud(GameController controller) {
        this.controller = controller;
        this.topBar = new TopBar();
        this.portraits = new Portraits();
        this.eventLog = new EventLog();
    }

    public JPanel topBar()   { return topBar; }
    public JPanel portraits() { return portraits; }
    public JPanel eventLog() { return eventLog; }

    public void setState(Views.GameStateView s) {
        topBar.state = s;      topBar.repaint();
        portraits.state = s;   portraits.repaint();
        eventLog.state = s;    eventLog.repaint();
    }

    // ===== Top bar =========================================================
    private final class TopBar extends JPanel {
        private final Font clockFont = new Font("SansSerif", Font.BOLD, 15);
        private final Font small = new Font("SansSerif", Font.PLAIN, 12);
        volatile Views.GameStateView state;
        // hit rectangles: [0]=pause, [1]=1x, [2]=2x, [3]=4x
        private final Rectangle[] buttons = new Rectangle[4];

        TopBar() {
            setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
            setBackground(Palette.HUD_BG);
            for (int i = 0; i < buttons.length; i++) buttons[i] = new Rectangle();
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { onClick(e.getPoint().x, e.getPoint().y); }
            });
        }

        private void onClick(int x, int y) {
            if (buttons[0].contains(x, y)) { controller.togglePause(); return; }
            if (buttons[1].contains(x, y)) { controller.setGameSpeed(1); return; }
            if (buttons[2].contains(x, y)) { controller.setGameSpeed(2); return; }
            if (buttons[3].contains(x, y)) { controller.setGameSpeed(4); return; }
        }

        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Views.GameStateView s = state;

            boolean paused = s != null && s.phase() == Enums.Phase.PAUSED;
            int speed = s == null ? 1 : s.gameSpeed();

            int x = 8, y = 6, h = 28;
            x = drawButton(g, buttons[0], x, y, h, paused ? "▶ Play" : "|| Pause", paused);
            x += 8;
            x = drawButton(g, buttons[1], x, y, h, "1x", !paused && speed == 1);
            x = drawButton(g, buttons[2], x, y, h, "2x", !paused && speed == 2);
            x = drawButton(g, buttons[3], x, y, h, "4x", !paused && speed == 4);

            // Clock (centre-left).
            g.setFont(clockFont);
            g.setColor(Palette.HUD_ACCENT);
            String clock = s == null ? "--" : formatClock(s.worldSeconds());
            g.drawString(clock, x + 24, 25);

            // Selected unit faction reputation (right side).
            g.setFont(small);
            String rep = reputationText(s);
            if (rep != null) {
                int tw = g.getFontMetrics().stringWidth(rep);
                g.setColor(Palette.HUD_TEXT);
                g.drawString(rep, getWidth() - tw - 12, 25);
            }
        }

        private int drawButton(Graphics2D g, Rectangle bounds, int x, int y, int h,
                               String text, boolean active) {
            g.setFont(small);
            int w = g.getFontMetrics().stringWidth(text) + 18;
            bounds.setBounds(x, y, w, h);
            g.setColor(active ? Palette.HUD_ACCENT : Palette.HUD_PANEL_LIGHT);
            g.fillRect(x, y, w, h);
            g.setColor(Palette.HUD_BORDER);
            g.drawRect(x, y, w, h);
            g.setColor(active ? new Color(30, 26, 20) : Palette.HUD_TEXT);
            g.drawString(text, x + 9, y + 19);
            return x + w + 4;
        }

        private String reputationText(Views.GameStateView s) {
            if (s == null) return null;
            List<String> ids = s.selectedIds();
            Enums.FactionId f = null;
            if (ids != null && !ids.isEmpty()) {
                Views.CharacterView ch = findChar(s, ids.get(0));
                if (ch != null) f = ch.faction();
            }
            if (f == null) return "Reputation: — (select a unit)";
            List<Views.FactionView> facs = s.factions();
            for (int i = 0; i < facs.size(); i++) {
                if (facs.get(i).id() == f) {
                    return facs.get(i).label() + " reputation: " + facs.get(i).reputationWithPlayer();
                }
            }
            return f.label();
        }
    }

    // ===== Portrait chips ==================================================
    private final class Portraits extends JPanel {
        private final Font small = new Font("SansSerif", Font.BOLD, 12);
        volatile Views.GameStateView state;
        private static final int CHIP_W = 92;
        private static final int CHIP_H = 46;
        private static final int GAP = 8;
        private List<String> chipIds = new ArrayList<String>();

        Portraits() {
            setPreferredSize(new Dimension(Integer.MAX_VALUE, 60));
            setBackground(Palette.HUD_BG);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { onClick(e.getPoint().x, e.getPoint().y); }
            });
        }

        private void onClick(int mx, int my) {
            List<String> ids = chipIds;
            int n = ids.size();
            if (n == 0) return;
            int totalW = n * CHIP_W + (n - 1) * GAP;
            int startX = (getWidth() - totalW) / 2;
            int y = (getHeight() - CHIP_H) / 2;
            if (my < y || my > y + CHIP_H) return;
            for (int i = 0; i < n; i++) {
                int x = startX + i * (CHIP_W + GAP);
                if (mx >= x && mx <= x + CHIP_W) {
                    List<String> sel = new ArrayList<String>();
                    sel.add(ids.get(i));
                    controller.setSelection(sel);
                    return;
                }
            }
        }

        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Views.GameStateView s = state;
            List<Views.CharacterView> squad = playerUnits(s);

            List<String> ids = new ArrayList<String>();
            for (int i = 0; i < squad.size(); i++) ids.add(squad.get(i).id());
            chipIds = ids;

            int n = squad.size();
            if (n == 0) return;
            int totalW = n * CHIP_W + (n - 1) * GAP;
            int startX = (getWidth() - totalW) / 2;
            int y = (getHeight() - CHIP_H) / 2;

            for (int i = 0; i < n; i++) {
                Views.CharacterView ch = squad.get(i);
                int x = startX + i * (CHIP_W + GAP);
                drawChip(g, ch, x, y);
            }
        }

        private void drawChip(Graphics2D g, Views.CharacterView ch, int x, int y) {
            boolean sel = ch.selected();
            g.setColor(sel ? Palette.HUD_PANEL_LIGHT : Palette.HUD_PANEL);
            g.fillRect(x, y, CHIP_W, CHIP_H);
            g.setColor(sel ? Palette.SELECT_RING : Palette.HUD_BORDER);
            g.drawRect(x, y, CHIP_W, CHIP_H);

            // Worst-part swatch as a health dot.
            double frac = Renderer.worstPartFraction(ch);
            g.setColor(Palette.grade(frac));
            g.fillOval(x + 8, y + 8, 14, 14);
            g.setColor(Palette.darker(Palette.faction(ch.faction()), 0.6));
            g.drawOval(x + 8, y + 8, 14, 14);

            g.setFont(small);
            g.setColor(Palette.HUD_TEXT);
            g.drawString(shorten(ch.name(), 9), x + 28, y + 18);

            g.setColor(Palette.HUD_TEXT_DIM);
            String st = stateBadge(ch);
            g.drawString(st, x + 28, y + 34);

            // health mini-bar along the bottom
            g.setColor(new Color(20, 18, 16));
            g.fillRect(x + 6, y + CHIP_H - 8, CHIP_W - 12, 4);
            g.setColor(Palette.grade(frac));
            g.fillRect(x + 6, y + CHIP_H - 8, (int) ((CHIP_W - 12) * frac), 4);
        }

        private String stateBadge(Views.CharacterView ch) {
            Enums.MoveState ms = ch.moveState();
            if (ms == Enums.MoveState.DEAD) return "DEAD";
            if (ms == Enums.MoveState.DOWNED) return "DOWNED";
            if (ms == Enums.MoveState.CRAWLING) return "CRAWL";
            if (ch.bleedRate() > 0.01) return "BLEEDING";
            return ms.name();
        }
    }

    // ===== Event log =======================================================
    private final class EventLog extends JPanel {
        private final Font small = new Font("Monospaced", Font.PLAIN, 11);
        volatile Views.GameStateView state;

        EventLog() {
            setPreferredSize(new Dimension(300, 60));
            setBackground(Palette.HUD_BG);
        }

        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setColor(Palette.HUD_BORDER);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.setFont(small);
            Views.GameStateView s = state;
            if (s == null) return;
            List<Views.LogView> log = s.log();
            if (log == null || log.isEmpty()) {
                g.setColor(Palette.HUD_TEXT_DIM);
                g.drawString("— no events yet —", 8, 18);
                return;
            }
            int lineH = 13;
            int maxLines = Math.max(1, (getHeight() - 8) / lineH);
            int from = Math.max(0, log.size() - maxLines);
            int y = 14;
            for (int i = from; i < log.size(); i++) {
                float a = (i - from + 1) / (float) (log.size() - from);
                g.setColor(Palette.lerp(Palette.HUD_TEXT_DIM, Palette.HUD_TEXT, a));
                g.drawString(clip(log.get(i).text(), g, getWidth() - 12), 8, y);
                y += lineH;
            }
        }

        private String clip(String txt, Graphics2D g, int maxW) {
            if (g.getFontMetrics().stringWidth(txt) <= maxW) return txt;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < txt.length(); i++) {
                sb.append(txt.charAt(i));
                if (g.getFontMetrics().stringWidth(sb.toString() + "…") > maxW) {
                    sb.append("…");
                    return sb.toString();
                }
            }
            return txt;
        }
    }

    // ===== helpers =========================================================
    private static List<Views.CharacterView> playerUnits(Views.GameStateView s) {
        List<Views.CharacterView> out = new ArrayList<Views.CharacterView>();
        if (s == null) return out;
        List<Views.CharacterView> chars = s.characters();
        for (int i = 0; i < chars.size(); i++) {
            if (chars.get(i).playerControlled()) out.add(chars.get(i));
        }
        return out;
    }

    private static Views.CharacterView findChar(Views.GameStateView s, String id) {
        List<Views.CharacterView> chars = s.characters();
        for (int i = 0; i < chars.size(); i++) {
            if (chars.get(i).id().equals(id)) return chars.get(i);
        }
        return null;
    }

    private static String formatClock(double worldSeconds) {
        long totalMin = (long) (worldSeconds / 60.0);
        long day = totalMin / (60 * 24) + 1;
        long hour = (totalMin / 60) % 24;
        long min = totalMin % 60;
        return String.format("Day %d  %02d:%02d", day, hour, min);
    }

    private static String shorten(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}

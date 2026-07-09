package com.whim.alganon.ui.screens;

import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.ControlState;
import com.whim.alganon.api.GameController;
import com.whim.alganon.api.Views;
import com.whim.alganon.ui.SoundHooks;
import com.whim.alganon.ui.UiTheme;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * The main in-world HUD: a central {@link WorldPanel} framed by docked panels — character
 * (left-top), minimap (left-bottom), quest tracker (right-top), inventory (right-bottom),
 * and along the bottom the action bar + a tabbed chat/log. Every panel reads only
 * {@link Views} from the controller.
 */
public final class HudScreen extends JPanel {

    private final GameController controller;
    private final WorldPanel world;
    private final ActionBar actionBar;
    private final ChatPanel chat;

    public HudScreen(GameController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(UiTheme.BG);

        world = new WorldPanel(controller);

        JPanel left = new JPanel();
        left.setLayout(new BorderLayout());
        left.setBackground(UiTheme.BG);
        left.setPreferredSize(new Dimension(250, 10));
        left.add(new CharacterPanel(controller), BorderLayout.NORTH);
        left.add(new Minimap(controller), BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setLayout(new BorderLayout());
        right.setBackground(UiTheme.BG);
        right.setPreferredSize(new Dimension(260, 10));
        right.add(new QuestTracker(controller), BorderLayout.NORTH);
        right.add(new Inventory(controller), BorderLayout.CENTER);

        actionBar = new ActionBar(controller);
        chat = new ChatPanel(controller);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(UiTheme.BG);
        bottom.add(actionBar, BorderLayout.NORTH);
        bottom.add(chat, BorderLayout.CENTER);
        bottom.setPreferredSize(new Dimension(10, 250));

        add(world, BorderLayout.CENTER);
        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    public int selectedTargetIndex() { return world.selectedMobIndex(); }

    public void refresh() { repaint(); }

    // ============================================================
    // Character panel: name/level/class + HP + resource + XP + stats
    // ============================================================
    static final class CharacterPanel extends JPanel {
        private final GameController controller;
        CharacterPanel(GameController c) { this.controller = c; setBackground(UiTheme.BG); setPreferredSize(new Dimension(250, 220)); }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0; UiTheme.aa(g);
            Views.CharacterView p = controller.state().player();
            UiTheme.panel(g, 6, 6, getWidth() - 12, getHeight() - 12);
            if (p == null) return;
            int x = 18, y = 30;
            g.setFont(UiTheme.FONT_H2); g.setColor(UiTheme.ACCENT);
            g.drawString(p.name(), x, y);
            g.setFont(UiTheme.FONT_SMALL); g.setColor(UiTheme.factionColor(p.faction()));
            g.drawString(p.faction().name(), getWidth() - 18 - g.getFontMetrics().stringWidth(p.faction().name()), y);
            g.setFont(UiTheme.FONT_BODY); g.setColor(UiTheme.TEXT_DIM);
            g.drawString("Lv " + p.level() + " " + p.raceName() + " " + p.className(), x, y + 18);

            int bw = getWidth() - 36, by = y + 34;
            g.setFont(UiTheme.FONT_SMALL);
            g.setColor(UiTheme.TEXT); g.drawString("HP", x, by - 2);
            UiTheme.bar(g, x, by, bw, 12, p.hp() / (double) Math.max(1, p.maxHp()), UiTheme.HP, UiTheme.HP_BG);
            g.setColor(UiTheme.TEXT); g.drawString(p.hp() + " / " + p.maxHp(), x + bw - 60, by + 11);

            by += 26;
            Color rc = UiTheme.resourceColor(p.resourceType());
            g.setColor(UiTheme.TEXT); g.drawString(p.resourceType().name(), x, by - 2);
            UiTheme.bar(g, x, by, bw, 12, p.resource() / (double) Math.max(1, p.maxResource()), rc, UiTheme.PANEL_DARK);
            g.setColor(UiTheme.TEXT); g.drawString(p.resource() + " / " + p.maxResource(), x + bw - 60, by + 11);

            by += 26;
            g.setColor(UiTheme.TEXT); g.drawString("XP", x, by - 2);
            UiTheme.bar(g, x, by, bw, 8, p.xp() / (double) Math.max(1, p.xpToNext()), UiTheme.XP, UiTheme.PANEL_DARK);

            by += 22;
            g.setColor(UiTheme.TEXT_DIM); g.setFont(UiTheme.FONT_SMALL);
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<com.whim.alganon.api.Enums.StatType, Integer> e : p.stats().entrySet())
                sb.append(e.getKey().name().substring(0, 3)).append(" ").append(e.getValue()).append("   ");
            g.drawString(sb.toString().trim(), x, by);
            g.setColor(UiTheme.ACCENT);
            g.drawString("Gold: " + p.gold(), x, by + 16);
        }
    }

    // ============================================================
    // Minimap placeholder: tiny top-down blips + faction war summary
    // ============================================================
    static final class Minimap extends JPanel {
        private final GameController controller;
        Minimap(GameController c) { this.controller = c; setBackground(UiTheme.BG); }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0; UiTheme.aa(g);
            UiTheme.panel(g, 6, 6, getWidth() - 12, getHeight() - 12);
            g.setFont(UiTheme.FONT_SMALL); g.setColor(UiTheme.TEXT_DIM);
            g.drawString("Minimap", 18, 24);
            Views.GameStateView v = controller.state();
            Views.WorldView w = v.world();
            if (w == null) return;
            int mx = 16, my = 32, mw = getWidth() - 32, mh = getHeight() - 96;
            g.setColor(UiTheme.PANEL_DARK); g.fillRect(mx, my, mw, mh);
            g.setColor(UiTheme.BORDER); g.drawRect(mx, my, mw, mh);
            double sx = mw / (double) w.width(), sy = mh / (double) w.height();
            for (Views.NpcView n : w.npcs()) blip(g, mx, my, sx, sy, n.pos(), n.questGiver() ? UiTheme.ACCENT : UiTheme.TEXT_DIM);
            for (Views.MobView m : w.mobs()) blip(g, mx, my, sx, sy, m.pos(), UiTheme.BAD);
            for (Views.PortalView pt : w.portals()) blip(g, mx, my, sx, sy, pt.pos(), UiTheme.XP);
            Views.CharacterView p = v.player();
            if (p != null && p.pos() != null) blip(g, mx, my, sx, sy, p.pos(), UiTheme.ACCENT_HOT);

            // faction war summary
            Views.FactionWarView war = w.factionWar();
            if (war != null) {
                int wy = my + mh + 16;
                g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_SMALL);
                g.drawString("Faction War  A:" + war.asharrScore() + "  K:" + war.kujixScore(), 18, wy);
                int ox = 18, oyy = wy + 8;
                for (Views.FactionWarView.ObjectiveView o : war.objectives()) {
                    g.setColor(UiTheme.controlColor(o.control()));
                    g.fillOval(ox, oyy, 8, 8);
                    ox += 14;
                }
            }
        }
        private void blip(Graphics2D g, int mx, int my, double sx, double sy, com.whim.alganon.api.GridPos p, Color c) {
            if (p == null) return;
            g.setColor(c);
            g.fillOval(mx + (int) (p.x * sx) - 2, my + (int) (p.y * sy) - 2, 5, 5);
        }
    }

    // ============================================================
    // Quest tracker
    // ============================================================
    static final class QuestTracker extends JPanel {
        private final GameController controller;
        QuestTracker(GameController c) { this.controller = c; setBackground(UiTheme.BG); setPreferredSize(new Dimension(260, 230)); }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0; UiTheme.aa(g);
            UiTheme.panel(g, 6, 6, getWidth() - 12, getHeight() - 12);
            g.setFont(UiTheme.FONT_H2); g.setColor(UiTheme.ACCENT);
            g.drawString("Quests", 18, 28);
            List<Views.QuestView> quests = controller.state().quests();
            int y = 50;
            g.setFont(UiTheme.FONT_BODY);
            for (Views.QuestView q : quests) {
                boolean ready = q.status() == com.whim.alganon.api.Enums.QuestStatus.READY_TO_TURN_IN;
                g.setColor(ready ? UiTheme.GOOD : UiTheme.TEXT);
                g.drawString((q.procedural() ? "◆ " : "• ") + q.name(), 18, y);
                y += 17;
                g.setFont(UiTheme.FONT_SMALL); g.setColor(UiTheme.TEXT_DIM);
                for (Views.QuestView.ObjectiveProgressView o : q.objectives()) {
                    String s = "   " + o.text() + "  " + o.current() + "/" + o.required();
                    g.setColor(o.done() ? UiTheme.GOOD : UiTheme.TEXT_DIM);
                    g.drawString(s, 18, y); y += 15;
                }
                g.setFont(UiTheme.FONT_BODY);
                y += 6;
                if (y > getHeight() - 20) break;
            }
            if (quests.isEmpty()) { g.setColor(UiTheme.TEXT_DIM); g.drawString("No active quests.", 18, y); }
        }
    }

    // ============================================================
    // Inventory grid
    // ============================================================
    static final class Inventory extends JPanel {
        private final GameController controller;
        Inventory(GameController c) { this.controller = c; setBackground(UiTheme.BG); }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0; UiTheme.aa(g);
            UiTheme.panel(g, 6, 6, getWidth() - 12, getHeight() - 12);
            g.setFont(UiTheme.FONT_H2); g.setColor(UiTheme.ACCENT);
            g.drawString("Inventory", 18, 28);
            Views.CharacterView p = controller.state().player();
            if (p == null) return;
            List<Views.ItemView> inv = p.inventory();
            int cols = 5, cell = 40, gap = 6, x0 = 16, y0 = 40, slots = 20;
            for (int i = 0; i < slots; i++) {
                int cx = x0 + (i % cols) * (cell + gap), cy = y0 + (i / cols) * (cell + gap);
                g.setColor(UiTheme.PANEL_DARK); g.fillRoundRect(cx, cy, cell, cell, 6, 6);
                g.setColor(UiTheme.BORDER); g.drawRoundRect(cx, cy, cell, cell, 6, 6);
                if (i < inv.size()) {
                    Views.ItemView it = inv.get(i);
                    g.setColor(itemColor(it));
                    g.fillRoundRect(cx + 8, cy + 8, cell - 16, cell - 16, 4, 4);
                    if (it.quantity() > 1) {
                        g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_SMALL);
                        String q = String.valueOf(it.quantity());
                        g.drawString(q, cx + cell - 4 - g.getFontMetrics().stringWidth(q), cy + cell - 4);
                    }
                }
            }
        }
        private Color itemColor(Views.ItemView it) {
            switch (it.type()) {
                case WEAPON: return new Color(0xC0, 0x6A, 0x5A);
                case ARMOR: return new Color(0x6A, 0x8A, 0xC0);
                case CONSUMABLE: return new Color(0x6A, 0xC0, 0x7A);
                case MATERIAL: return new Color(0xB0, 0x9A, 0x6A);
                default: return UiTheme.TEXT_DIM;
            }
        }
    }

    // ============================================================
    // Action bar: abilities with cooldown sweep + resource cost + hotkey
    // ============================================================
    static final class ActionBar extends JPanel {
        private final GameController controller;
        ActionBar(GameController c) {
            this.controller = c; setBackground(UiTheme.BG); setPreferredSize(new Dimension(10, 78));
            addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { click(e); } });
        }
        private Rectangle slotRect(int i, int n) {
            int cell = 58, gap = 8, total = n * cell + (n - 1) * gap;
            int x0 = (getWidth() - total) / 2, y0 = 12;
            return new Rectangle(x0 + i * (cell + gap), y0, cell, cell);
        }
        private void click(MouseEvent e) {
            Views.CharacterView p = controller.state().player();
            if (p == null) return;
            List<Views.AbilityView> abils = p.abilities();
            for (int i = 0; i < abils.size(); i++) {
                if (slotRect(i, abils.size()).contains(e.getPoint())) {
                    SoundHooks.get().play(SoundHooks.Cue.ABILITY_CAST);
                    controller.useAbility(abils.get(i).id(), -1);
                    return;
                }
            }
        }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0; UiTheme.aa(g);
            Views.CharacterView p = controller.state().player();
            if (p == null) return;
            List<Views.AbilityView> abils = p.abilities();
            Color rc = UiTheme.resourceColor(p.resourceType());
            for (int i = 0; i < abils.size(); i++) {
                Views.AbilityView a = abils.get(i);
                Rectangle r = slotRect(i, abils.size());
                boolean afford = p.resource() >= a.resourceCost();
                g.setColor(UiTheme.PANEL); g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                g.setColor(a.usable() && afford ? UiTheme.ACCENT : UiTheme.BORDER);
                g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
                // ability glyph
                g.setColor(kindColor(a));
                g.fillRoundRect(r.x + 8, r.y + 8, r.width - 16, r.height - 24, 6, 6);
                g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_SMALL);
                String nm = a.name().length() > 8 ? a.name().substring(0, 8) : a.name();
                g.drawString(nm, r.x + 5, r.y + r.height - 6);
                // hotkey
                g.setColor(UiTheme.ACCENT_HOT); g.setFont(UiTheme.FONT_SMALL);
                g.drawString(String.valueOf(i + 1), r.x + 4, r.y + 13);
                // resource cost
                g.setColor(afford ? rc : UiTheme.BAD);
                String cost = String.valueOf(a.resourceCost());
                g.drawString(cost, r.x + r.width - 4 - g.getFontMetrics().stringWidth(cost), r.y + 13);
                // cooldown sweep
                if (a.cooldownRemaining() > 0 && a.cooldownSec() > 0) {
                    double frac = a.cooldownRemaining() / a.cooldownSec();
                    g.setColor(new Color(0, 0, 0, 150));
                    int ang = (int) Math.round(360 * frac);
                    g.fillArc(r.x + 6, r.y + 6, r.width - 12, r.height - 12, 90, ang);
                    g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_SMALL);
                    String cd = String.format("%.0f", Math.ceil(a.cooldownRemaining()));
                    g.drawString(cd, r.x + r.width / 2 - 4, r.y + r.height / 2 + 2);
                }
            }
        }
        private Color kindColor(Views.AbilityView a) {
            switch (a.kind()) {
                case HEAL: case HOT: return new Color(0x4A, 0x9A, 0x5A);
                case DAMAGE: return new Color(0x9A, 0x4A, 0x4A);
                case DOT: return new Color(0x7A, 0x4A, 0x7A);
                case BUFF: return new Color(0x4A, 0x6A, 0x9A);
                case DEBUFF: return new Color(0x6A, 0x5A, 0x3A);
                case PET_SUMMON: return new Color(0x5A, 0x7A, 0x4A);
                case TRAP: return new Color(0x8A, 0x7A, 0x3A);
                case STANCE: return new Color(0x7A, 0x6A, 0x9A);
                default: return UiTheme.PANEL_LIGHT;
            }
        }
    }

    // ============================================================
    // Chat / log with channel tabs
    // ============================================================
    static final class ChatPanel extends JPanel {
        private final GameController controller;
        private ChatChannel filter = null; // null = All
        private final ChatChannel[] tabs = {null, ChatChannel.SYSTEM, ChatChannel.SAY, ChatChannel.FAMILY,
                ChatChannel.FACTION, ChatChannel.COMBAT, ChatChannel.LOOT};
        private Rectangle[] tabHits = new Rectangle[0];
        ChatPanel(GameController c) {
            this.controller = c; setBackground(UiTheme.BG);
            addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { clickTab(e); } });
        }
        private void clickTab(MouseEvent e) {
            for (int i = 0; i < tabHits.length; i++)
                if (tabHits[i] != null && tabHits[i].contains(e.getPoint())) { filter = tabs[i]; repaint(); return; }
        }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0; UiTheme.aa(g);
            UiTheme.panel(g, 6, 2, getWidth() - 12, getHeight() - 8);
            // tabs
            tabHits = new Rectangle[tabs.length];
            int tx = 12, ty = 8;
            g.setFont(UiTheme.FONT_SMALL);
            for (int i = 0; i < tabs.length; i++) {
                String label = tabs[i] == null ? "All" : cap(tabs[i].name());
                int tw = g.getFontMetrics().stringWidth(label) + 14;
                Rectangle r = new Rectangle(tx, ty, tw, 18);
                tabHits[i] = r;
                boolean sel = filter == tabs[i];
                g.setColor(sel ? UiTheme.PANEL_LIGHT : UiTheme.PANEL_DARK);
                g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
                g.setColor(sel ? UiTheme.ACCENT : UiTheme.channelColor(tabs[i]));
                g.drawString(label, r.x + 7, r.y + 13);
                tx += tw + 5;
            }
            // lines
            List<Views.ChatLineView> lines = controller.state().chat();
            int y = getHeight() - 12;
            g.setFont(UiTheme.FONT_BODY);
            for (int i = lines.size() - 1; i >= 0 && y > 34; i--) {
                Views.ChatLineView ln = lines.get(i);
                if (filter != null && ln.channel() != filter) continue;
                g.setColor(UiTheme.channelColor(ln.channel()));
                String s = "[" + cap(ln.channel().name()) + "] " + ln.text();
                g.drawString(clip(g, s, getWidth() - 30), 14, y);
                y -= 17;
            }
        }
        private String clip(Graphics2D g, String s, int maxW) {
            if (g.getFontMetrics().stringWidth(s) <= maxW) return s;
            while (s.length() > 4 && g.getFontMetrics().stringWidth(s + "…") > maxW) s = s.substring(0, s.length() - 1);
            return s + "…";
        }
        private static String cap(String s) { return s.charAt(0) + s.substring(1).toLowerCase(); }
    }
}

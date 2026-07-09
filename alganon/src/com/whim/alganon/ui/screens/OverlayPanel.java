package com.whim.alganon.ui.screens;

import com.whim.alganon.api.Defs;
import com.whim.alganon.api.Enums.GameStateType;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.GameController;
import com.whim.alganon.api.Views;
import com.whim.alganon.ui.SoundHooks;
import com.whim.alganon.ui.UiTheme;
import com.whim.alganon.ui.render.Sprites;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A single full-window overlay that renders whichever overlay state is active — Study,
 * Crafting, Auction, Family, Library/Codex, Settings, Character Sheet, Inventory, or Quest
 * Log — atop a dimmed HUD. Interactive hotspots (assign study, craft, buy, tabs) call the
 * controller. The whole panel is only added to the frame while an overlay state is active.
 */
public final class OverlayPanel extends JPanel {

    private final GameController controller;
    private final List<Hotspot> hotspots = new ArrayList<Hotspot>();
    private int libraryTab = 0; // 0 races,1 classes,2 families,3 systems

    public OverlayPanel(GameController controller) {
        this.controller = controller;
        setOpaque(false);
        addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { click(e); } });
    }

    private interface Action { void run(); }
    private static final class Hotspot { final Rectangle r; final Action a; Hotspot(Rectangle r, Action a) { this.r = r; this.a = a; } }

    private void click(MouseEvent e) {
        for (Hotspot h : new ArrayList<Hotspot>(hotspots))
            if (h.r.contains(e.getPoint())) { SoundHooks.get().play(SoundHooks.Cue.UI_CLICK); h.a.run(); return; }
    }

    private Rectangle add(int x, int y, int w, int h, Action a) {
        Rectangle r = new Rectangle(x, y, w, h);
        hotspots.add(new Hotspot(r, a));
        return r;
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0; UiTheme.aa(g);
        hotspots.clear();
        int w = getWidth(), h = getHeight();
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, w, h);

        int pad = 60;
        int px = pad, py = pad, pw = w - pad * 2, ph = h - pad * 2;
        UiTheme.panel(g, px, py, pw, ph, UiTheme.PANEL);

        Views.GameStateView v = controller.state();
        GameStateType st = v.state();
        // header
        g.setFont(UiTheme.FONT_H1); g.setColor(UiTheme.ACCENT);
        g.drawString(title(st), px + 24, py + 34);
        // close button
        Rectangle close = add(px + pw - 44, py + 14, 30, 30, new Action() { public void run() { controller.closeOverlay(); } });
        g.setColor(UiTheme.PANEL_DARK); g.fillRoundRect(close.x, close.y, close.width, close.height, 6, 6);
        g.setColor(UiTheme.BORDER); g.drawRoundRect(close.x, close.y, close.width, close.height, 6, 6);
        g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_H2); g.drawString("✕", close.x + 9, close.y + 20);

        int cx = px + 24, cy = py + 64, cw = pw - 48, chh = ph - 88;
        switch (st) {
            case STUDY: drawStudy(g, v, cx, cy, cw); break;
            case CRAFTING: drawCrafting(g, v, cx, cy, cw); break;
            case AUCTION: drawAuction(g, v, cx, cy, cw); break;
            case FAMILY: drawFamily(g, v, cx, cy, cw); break;
            case LIBRARY: drawLibrary(g, v, cx, cy, cw, chh); break;
            case SETTINGS: drawSettings(g, v, cx, cy, cw); break;
            case CHARACTER_SHEET: drawSheet(g, v, cx, cy, cw); break;
            case INVENTORY: drawInventory(g, v, cx, cy, cw); break;
            case QUEST_LOG: drawQuestLog(g, v, cx, cy, cw, chh); break;
            default: g.setColor(UiTheme.TEXT_DIM); g.drawString("(nothing here)", cx, cy); break;
        }

        g.setFont(UiTheme.FONT_SMALL); g.setColor(UiTheme.TEXT_FAINT);
        g.drawString("Esc or ✕ to close", px + 24, py + ph - 16);
    }

    private String title(GameStateType st) {
        switch (st) {
            case STUDY: return "Study — Offline Progression";
            case CRAFTING: return "Tradeskills — Crafting";
            case AUCTION: return "Requisition House";
            case FAMILY: return "Family";
            case LIBRARY: return "Library / Codex";
            case SETTINGS: return "Settings";
            case CHARACTER_SHEET: return "Character Sheet";
            case INVENTORY: return "Inventory";
            case QUEST_LOG: return "Quest Log";
            default: return "";
        }
    }

    // -------------------- STUDY --------------------
    private void drawStudy(Graphics2D g, Views.GameStateView v, int x, int y, int w) {
        Views.StudyView s = v.study();
        if (s == null) return;
        g.setFont(UiTheme.FONT_BODY); g.setColor(UiTheme.TEXT_DIM);
        CreationScreen.drawWrapped(g,
                "Assign one skill to Study. While the game is closed it banks offline progress at a fixed "
                        + "rate, capped so idle time can't outpace active play. [single-player substitution for "
                        + "Alganon's offline study]", x, y, w, 19);
        int by = y + 60;
        g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_H2);
        g.drawString("Assigned: " + (s.assignedSkill() == null ? "none" : s.assignedSkill().name())
                + "   (" + s.studySlots() + " slot)", x, by);

        by += 12;
        g.setColor(UiTheme.TEXT_DIM); g.setFont(UiTheme.FONT_SMALL);
        g.drawString("Banked " + String.format("%.1f", s.bankedHours()) + "h / cap "
                + String.format("%.0f", s.capHours()) + "h", x, by + 18);
        UiTheme.bar(g, x, by + 24, 280, 10, s.bankedHours() / Math.max(0.1, s.capHours()), UiTheme.XP, UiTheme.PANEL_DARK);
        g.setColor(UiTheme.TEXT_DIM);
        g.drawString("Progress to next point of " + (s.assignedSkill() == null ? "—" : s.assignedSkill().name()), x, by + 54);
        UiTheme.bar(g, x, by + 60, 280, 10, s.progressToNextPoint(), UiTheme.GOOD, UiTheme.PANEL_DARK);

        int sy = by + 100;
        g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_H2); g.drawString("Skills", x, sy);
        sy += 12;
        for (final SkillType sk : s.studyableSkills()) {
            Integer lvl = s.skillLevels() == null ? null : s.skillLevels().get(sk);
            final boolean assigned = sk == s.assignedSkill();
            Rectangle r = add(x, sy, 260, 26, new Action() { public void run() {
                if (assigned) controller.clearStudy(); else controller.assignStudy(sk);
            }});
            g.setColor(assigned ? UiTheme.PANEL_LIGHT : UiTheme.PANEL_DARK);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            g.setColor(assigned ? UiTheme.ACCENT : UiTheme.BORDER);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_BODY);
            g.drawString(sk.name() + (lvl != null ? "  (Lv " + lvl + ")" : "") + (assigned ? "  ✓ studying" : ""), r.x + 10, r.y + 18);
            sy += 32;
        }
    }

    // -------------------- CRAFTING --------------------
    private void drawCrafting(Graphics2D g, Views.GameStateView v, int x, int y, int w) {
        Views.CraftingView c = v.crafting();
        if (c == null) return;
        int col2 = x + w / 2 + 20;
        g.setFont(UiTheme.FONT_H2); g.setColor(UiTheme.TEXT); g.drawString("Recipes", x, y);
        int ry = y + 16;
        for (final Views.CraftingView.RecipeProgressView rp : c.recipes()) {
            Rectangle r = add(x, ry, w / 2 - 20, 54, new Action() { public void run() {
                if (rp.craftable()) controller.craft(rp.id()); } });
            g.setColor(UiTheme.PANEL_DARK); g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(rp.craftable() ? UiTheme.GOOD : UiTheme.BORDER);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_BODY);
            g.drawString(rp.name(), r.x + 10, r.y + 20);
            g.setColor(UiTheme.TEXT_DIM); g.setFont(UiTheme.FONT_SMALL);
            StringBuilder in = new StringBuilder("needs: ");
            for (Map.Entry<String, Integer> e : rp.inputs().entrySet()) in.append(e.getValue()).append("x ").append(e.getKey()).append("  ");
            g.drawString(in.toString(), r.x + 10, r.y + 37);
            g.setColor(rp.craftable() ? UiTheme.GOOD : UiTheme.BAD);
            g.drawString(rp.craftable() ? "→ craft " + rp.outputQty() + "x " + rp.outputName() : "missing materials", r.x + 10, r.y + 50);
            ry += 62;
        }
        // materials column
        g.setFont(UiTheme.FONT_H2); g.setColor(UiTheme.TEXT); g.drawString("Materials", col2, y);
        int my = y + 20;
        g.setFont(UiTheme.FONT_BODY);
        for (Map.Entry<String, Integer> e : c.materials().entrySet()) {
            g.setColor(UiTheme.TEXT_DIM);
            g.drawString(e.getValue() + "x  " + e.getKey(), col2, my);
            my += 20;
        }
    }

    // -------------------- AUCTION --------------------
    private void drawAuction(Graphics2D g, Views.GameStateView v, int x, int y, int w) {
        Views.AuctionView a = v.auction();
        if (a == null) return;
        g.setFont(UiTheme.FONT_H2); g.setColor(UiTheme.ACCENT);
        g.drawString("Your gold: " + a.playerGold(), x, y);
        int ry = y + 24;
        g.setFont(UiTheme.FONT_BODY); g.setColor(UiTheme.TEXT_DIM);
        g.drawString("Item", x + 10, ry); g.drawString("Qty", x + 260, ry);
        g.drawString("Price", x + 320, ry); g.drawString("", x + 420, ry);
        ry += 10;
        for (final Views.AuctionView.ListingView l : a.listings()) {
            ry += 8;
            g.setColor(UiTheme.PANEL_DARK); g.fillRoundRect(x, ry, w - 20, 34, 6, 6);
            g.setColor(UiTheme.BORDER); g.drawRoundRect(x, ry, w - 20, 34, 6, 6);
            g.setColor(l.sellerIsPlayer() ? UiTheme.ACCENT : UiTheme.TEXT); g.setFont(UiTheme.FONT_BODY);
            g.drawString(l.itemName() + (l.sellerIsPlayer() ? "  (yours)" : ""), x + 10, ry + 22);
            g.setColor(UiTheme.TEXT_DIM);
            g.drawString(String.valueOf(l.quantity()), x + 260, ry + 22);
            g.setColor(UiTheme.ACCENT);
            g.drawString(l.price() + "g", x + 320, ry + 22);
            if (!l.sellerIsPlayer()) {
                Rectangle b = add(x + w - 100, ry + 6, 72, 22, new Action() { public void run() { controller.auctionBuy(l.listingId()); } });
                g.setColor(UiTheme.PANEL_LIGHT); g.fillRoundRect(b.x, b.y, b.width, b.height, 6, 6);
                g.setColor(UiTheme.GOOD); g.drawRoundRect(b.x, b.y, b.width, b.height, 6, 6);
                g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_SMALL); g.drawString("Buy", b.x + 26, b.y + 15);
            }
            ry += 42;
        }
        g.setColor(UiTheme.TEXT_FAINT); g.setFont(UiTheme.FONT_SMALL);
        g.drawString("NPC-populated listings (single-player economy substitute). Posting via inventory.", x, ry + 16);
    }

    // -------------------- FAMILY --------------------
    private void drawFamily(Graphics2D g, Views.GameStateView v, int x, int y, int w) {
        Views.FamilyView f = v.family();
        if (f == null) return;
        g.setFont(UiTheme.FONT_H1); g.setColor(UiTheme.ACCENT);
        g.drawString(f.familyName(), x, y);
        g.setFont(UiTheme.FONT_H2); g.setColor(UiTheme.TEXT_DIM);
        g.drawString("Archetype: " + f.archetype().name(), x, y + 26);
        g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_BODY);
        CreationScreen.drawWrapped(g, "Bonus: " + f.bonusDescription(), x, y + 52, w, 19);
        int my = y + 110;
        g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_H2); g.drawString("Members", x, my);
        my += 12;
        g.setFont(UiTheme.FONT_BODY);
        for (String m : f.memberNames()) {
            Sprites.draw(g, "npc", x + 12, my + 8, 8);
            g.setColor(UiTheme.TEXT); g.drawString(m, x + 28, my + 12); my += 26;
        }
        g.setColor(UiTheme.TEXT_FAINT); g.setFont(UiTheme.FONT_SMALL);
        g.drawString("Family channel + merchant are NPC-driven single-player substitutions.", x, my + 14);
    }

    // -------------------- LIBRARY / CODEX --------------------
    private void drawLibrary(Graphics2D g, Views.GameStateView v, int x, int y, int w, int hh) {
        final String[] tabs = {"Races", "Classes", "Families", "Systems"};
        int tx = x;
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            Rectangle r = add(tx, y, 100, 24, new Action() { public void run() { libraryTab = idx; } });
            boolean sel = libraryTab == i;
            g.setColor(sel ? UiTheme.PANEL_LIGHT : UiTheme.PANEL_DARK); g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            g.setColor(sel ? UiTheme.ACCENT : UiTheme.BORDER); g.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            g.setColor(sel ? UiTheme.ACCENT_HOT : UiTheme.TEXT); g.setFont(UiTheme.FONT_BODY);
            g.drawString(tabs[i], r.x + 14, r.y + 16);
            tx += 108;
        }
        int cy = y + 44;
        Views.CreationView cr = codexSource(v);
        g.setFont(UiTheme.FONT_BODY);
        if (cr == null) { g.setColor(UiTheme.TEXT_DIM); g.drawString("Reference unavailable.", x, cy); return; }
        switch (libraryTab) {
            case 0:
                for (Defs.RaceDef r : cr.races()) cy = entry(g, x, cy, w, r.name + "  (" + r.faction + ")", r.description);
                break;
            case 1:
                for (Defs.ClassDef c : cr.classes()) cy = entry(g, x, cy, w, c.name + "  [" + c.resource + "]", c.description);
                break;
            case 2:
                for (Defs.FamilyDef f : allFamilies(cr)) cy = entry(g, x, cy, w, f.name + "  (" + f.archetype + ")", f.description);
                break;
            default:
                cy = entry(g, x, cy, w, "Faction War", "Alganon's Towers & Keeps PvP is replaced by a background simulated faction war whose control drifts over time. Watch it on the minimap and in the Faction chat channel.");
                cy = entry(g, x, cy, w, "Study", "Offline progression: assign a skill, bank hours up to an 8h cap, and collect on load.");
                cy = entry(g, x, cy, w, "Grouping", "Legions and raids become flavor structures; a tiny NPC companion party stands in for live grouping.");
                break;
        }
    }

    private int entry(Graphics2D g, int x, int y, int w, String head, String body) {
        g.setColor(UiTheme.ACCENT); g.setFont(UiTheme.FONT_H2);
        g.drawString(head, x, y);
        g.setColor(UiTheme.TEXT_DIM); g.setFont(UiTheme.FONT_BODY);
        int lines = wrapCount(g, body, w - 20);
        CreationScreen.drawWrapped(g, body, x, y + 18, w - 20, 17);
        return y + 26 + lines * 17;
    }

    private int wrapCount(Graphics2D g, String text, int maxW) {
        String[] words = text.split(" ");
        int count = 1, lineW = 0;
        for (String word : words) {
            int ww = g.getFontMetrics().stringWidth(word + " ");
            if (lineW + ww > maxW) { count++; lineW = ww; } else lineW += ww;
        }
        return count;
    }

    private Views.CreationView codexSource(Views.GameStateView v) {
        // The library reads the same content projection the wizard uses; the stub always
        // exposes it, and a real engine can surface a creation view for the codex too.
        return v.creation();
    }

    private List<Defs.FamilyDef> allFamilies(Views.CreationView cr) {
        List<Defs.FamilyDef> out = new ArrayList<Defs.FamilyDef>();
        for (Defs.RaceDef r : cr.races()) out.addAll(cr.familiesFor(r.id));
        return out;
    }

    // -------------------- SETTINGS --------------------
    private void drawSettings(Graphics2D g, Views.GameStateView v, int x, int y, int w) {
        g.setFont(UiTheme.FONT_BODY);
        Rectangle mute = add(x, y, 220, 30, new Action() { public void run() {
            SoundHooks.get().setMuted(!SoundHooks.get().isMuted()); repaint(); } });
        g.setColor(UiTheme.PANEL_DARK); g.fillRoundRect(mute.x, mute.y, mute.width, mute.height, 6, 6);
        g.setColor(UiTheme.BORDER); g.drawRoundRect(mute.x, mute.y, mute.width, mute.height, 6, 6);
        g.setColor(UiTheme.TEXT); g.drawString("Sound: " + (SoundHooks.get().isMuted() ? "Muted" : "On (stub)"), mute.x + 12, mute.y + 20);

        int sy = y + 46;
        g.setColor(UiTheme.TEXT_DIM);
        String[] info = {
                "Movement: WASD or Arrow keys", "Interact: E    Target: click a creature",
                "Abilities: number keys 1–6 or click the action bar",
                "Panels: I inventory · C sheet · K study · B crafting · U auction · F family · L codex · J quests",
                "Close overlay: Esc"};
        for (String s : info) { g.drawString(s, x, sy); sy += 22; }
    }

    // -------------------- CHARACTER SHEET --------------------
    private void drawSheet(Graphics2D g, Views.GameStateView v, int x, int y, int w) {
        Views.CharacterView p = v.player();
        if (p == null) return;
        Sprites.draw(g, "player." + p.className(), x + 40, y + 40, 34);
        g.setColor(UiTheme.ACCENT); g.setFont(UiTheme.FONT_H1);
        g.drawString(p.name(), x + 100, y + 30);
        g.setColor(UiTheme.TEXT_DIM); g.setFont(UiTheme.FONT_H2);
        g.drawString("Lv " + p.level() + " " + p.raceName() + " " + p.className() + " · " + p.familyName(), x + 100, y + 54);
        g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_BODY);
        int sy = y + 110;
        g.setFont(UiTheme.FONT_H2); g.setColor(UiTheme.TEXT); g.drawString("Attributes", x, sy); sy += 22;
        g.setFont(UiTheme.FONT_BODY);
        for (Map.Entry<com.whim.alganon.api.Enums.StatType, Integer> e : p.stats().entrySet()) {
            g.setColor(UiTheme.TEXT_DIM); g.drawString(e.getKey().name(), x, sy);
            g.setColor(UiTheme.TEXT); g.drawString(String.valueOf(e.getValue()), x + 120, sy); sy += 20;
        }
        int col2 = x + w / 2;
        int ey = y + 110;
        g.setFont(UiTheme.FONT_H2); g.setColor(UiTheme.TEXT); g.drawString("Equipment", col2, ey); ey += 22;
        g.setFont(UiTheme.FONT_BODY);
        for (Map.Entry<com.whim.alganon.api.Enums.EquipSlot, Views.ItemView> e : p.equipped().entrySet()) {
            g.setColor(UiTheme.TEXT_DIM); g.drawString(e.getKey().name(), col2, ey);
            g.setColor(UiTheme.TEXT); g.drawString(e.getValue().name(), col2 + 90, ey); ey += 20;
        }
        if (p.equipped().isEmpty()) { g.setColor(UiTheme.TEXT_FAINT); g.drawString("(nothing equipped)", col2, ey); }
    }

    // -------------------- INVENTORY (full) --------------------
    private void drawInventory(Graphics2D g, Views.GameStateView v, int x, int y, int w) {
        Views.CharacterView p = v.player();
        if (p == null) return;
        List<Views.ItemView> inv = p.inventory();
        int cols = 8, cell = 54, gap = 8;
        for (int i = 0; i < 40; i++) {
            int gx = x + (i % cols) * (cell + gap), gy = y + (i / cols) * (cell + gap);
            g.setColor(UiTheme.PANEL_DARK); g.fillRoundRect(gx, gy, cell, cell, 6, 6);
            g.setColor(UiTheme.BORDER); g.drawRoundRect(gx, gy, cell, cell, 6, 6);
            if (i < inv.size()) {
                final Views.ItemView it = inv.get(i);
                add(gx, gy, cell, cell, new Action() { public void run() { controller.useItem(it.id()); } });
                g.setColor(new Color(0x6A, 0x8A, 0xB0)); g.fillRoundRect(gx + 10, gy + 10, cell - 20, cell - 20, 4, 4);
                g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_SMALL);
                if (it.quantity() > 1) g.drawString("x" + it.quantity(), gx + cell - 22, gy + cell - 6);
            }
        }
        g.setColor(UiTheme.TEXT_DIM); g.setFont(UiTheme.FONT_SMALL);
        g.drawString("Click an item to use/equip.", x, y + 5 * (cell + gap) + 10);
    }

    // -------------------- QUEST LOG --------------------
    private void drawQuestLog(Graphics2D g, Views.GameStateView v, int x, int y, int w, int hh) {
        List<Views.QuestView> quests = v.quests();
        int qy = y;
        for (Views.QuestView q : quests) {
            g.setColor(UiTheme.ACCENT); g.setFont(UiTheme.FONT_H2);
            g.drawString(q.name() + "  [" + q.status() + "]" + (q.procedural() ? "  (dynamic)" : ""), x, qy);
            qy += 20;
            g.setColor(UiTheme.TEXT_DIM); g.setFont(UiTheme.FONT_BODY);
            CreationScreen.drawWrapped(g, q.description(), x, qy, w - 20, 17);
            qy += 24;
            for (Views.QuestView.ObjectiveProgressView o : q.objectives()) {
                g.setColor(o.done() ? UiTheme.GOOD : UiTheme.TEXT);
                g.drawString("  • " + o.text() + "  " + o.current() + "/" + o.required(), x, qy); qy += 18;
            }
            qy += 14;
            if (qy > y + hh - 20) break;
        }
        if (quests.isEmpty()) { g.setColor(UiTheme.TEXT_DIM); g.drawString("No quests yet.", x, qy); }
    }
}

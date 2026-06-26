package com.whim.starcraft8.ui;

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

import javax.swing.JPanel;

import com.whim.starcraft8.data.TechTree;
import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.Race;
import com.whim.starcraft8.domain.UnitType;
import com.whim.starcraft8.engine.Commands;

/**
 * Bottom command console: minimap, selection portrait, resource readout
 * (minerals / gas / supply) and contextual action buttons. Buttons are painted
 * clickable regions (rebuilt each frame from the selection) that translate to
 * {@code Commands.*} pushed through {@link UiContext}. Pure presentation.
 */
final class Hud extends JPanel {

    static final int HEIGHT = 150;

    private static final Color BG = new Color(18, 20, 28);
    private static final Color PANEL = new Color(30, 34, 46);
    private static final Color BORDER = new Color(70, 80, 100);
    private static final Color BTN = new Color(46, 54, 72);
    private static final Color BTN_HI = new Color(70, 86, 116);
    private static final Color TEXT = new Color(220, 228, 240);
    private static final Color MIN_C = new Color(90, 180, 240);
    private static final Color GAS_C = new Color(90, 220, 150);
    private static final Color OWN_C = new Color(90, 200, 255);
    private static final Color ENEMY_C = new Color(235, 70, 70);

    private final UiContext ctx;
    private final GamePanel panel; // for keyboard-equivalent actions / repaint coupling

    private final Font fBig = new Font("Monospaced", Font.BOLD, 16);
    private final Font fSmall = new Font("Monospaced", Font.BOLD, 11);

    private boolean buildMenuOpen = false;

    // layout rects (recomputed in paint)
    private Rectangle minimapRect = new Rectangle();
    private Rectangle portraitRect = new Rectangle();

    private static final class Btn {
        Rectangle rect;
        String label;
        Runnable action;
        boolean enabled = true;
        Btn(Rectangle r, String l, Runnable a) { rect = r; label = l; action = a; }
    }

    private final List<Btn> buttons = new ArrayList<Btn>();

    Hud(UiContext ctx, GamePanel panel) {
        this.ctx = ctx;
        this.panel = panel;
        setPreferredSize(new Dimension(100, HEIGHT));
        setBackground(BG);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onClick(e); }
        });
    }

    // ---- click handling ----------------------------------------------------

    private void onClick(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        int x = e.getX();
        int y = e.getY();
        if (minimapRect.contains(x, y)) {
            RenderState rs = ctx.render;
            if (rs != null) {
                double fx = (x - minimapRect.x) / (double) minimapRect.width;
                double fy = (y - minimapRect.y) / (double) minimapRect.height;
                ctx.camera.centerOnTile(fx * rs.mapW, fy * rs.mapH);
            }
            return;
        }
        for (int i = 0; i < buttons.size(); i++) {
            Btn b = buttons.get(i);
            if (b.enabled && b.rect.contains(x, y)) {
                b.action.run();
                repaint();
                if (panel != null) panel.repaint();
                return;
            }
        }
    }

    // ---- painting ----------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        RenderState rs = ctx.render;

        int h = getHeight();
        int pad = 6;

        // minimap (left square)
        int mmSize = h - pad * 2;
        minimapRect = new Rectangle(pad, pad, mmSize, mmSize);
        drawPanel(g, minimapRect);
        if (rs != null) drawMinimap(g, rs, minimapRect);

        // resource readout (top center) + supply
        drawResources(g, rs, minimapRect.x + minimapRect.width + pad, pad);

        // portrait
        int portX = minimapRect.x + minimapRect.width + pad;
        int portY = pad + 26;
        int portSize = h - portY - pad;
        portraitRect = new Rectangle(portX, portY, portSize, portSize);
        drawPanel(g, portraitRect);
        drawPortrait(g, rs, portraitRect);

        // action buttons (right)
        int btnAreaX = portraitRect.x + portraitRect.width + pad;
        layoutButtons(rs, btnAreaX, pad, getWidth() - btnAreaX - pad, h - pad * 2);
        drawButtons(g);
    }

    private void drawPanel(Graphics2D g, Rectangle r) {
        g.setColor(PANEL);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(BORDER);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
    }

    private void drawResources(Graphics2D g, RenderState rs, int x, int y) {
        g.setFont(fBig);
        if (rs == null || !rs.humanFound) {
            g.setColor(TEXT);
            g.drawString("- / - / -", x, y + 16);
            return;
        }
        int cx = x;
        g.setColor(MIN_C);
        g.fillRect(cx, y + 4, 10, 10);
        g.setColor(TEXT);
        g.drawString(String.valueOf(rs.minerals), cx + 14, y + 14);
        cx += 14 + 60;

        g.setColor(GAS_C);
        g.fillRect(cx, y + 4, 10, 10);
        g.setColor(TEXT);
        g.drawString(String.valueOf(rs.gas), cx + 14, y + 14);
        cx += 14 + 60;

        g.setColor(rs.supplyUsed >= rs.supplyCap ? ENEMY_C : TEXT);
        g.drawString("SUP " + rs.supplyUsed + "/" + rs.supplyCap, cx, y + 14);
    }

    private void drawMinimap(Graphics2D g, RenderState rs, Rectangle r) {
        double sx = r.width / (double) rs.mapW;
        double sy = r.height / (double) rs.mapH;
        // terrain
        for (int tx = 0; tx < rs.mapW; tx++) {
            for (int ty = 0; ty < rs.mapH; ty++) {
                Color c;
                switch (rs.terrain[tx][ty]) {
                    case MINERAL_FIELD: c = MIN_C; break;
                    case GEYSER: c = GAS_C; break;
                    case UNBUILDABLE: c = new Color(20, 22, 28); break;
                    default: c = new Color(44, 58, 44); break;
                }
                g.setColor(c);
                g.fillRect(r.x + (int) (tx * sx), r.y + (int) (ty * sy),
                        Math.max(1, (int) Math.ceil(sx)), Math.max(1, (int) Math.ceil(sy)));
            }
        }
        // buildings
        for (int i = 0; i < rs.buildings.size(); i++) {
            RenderState.RBuilding b = rs.buildings.get(i);
            g.setColor(b.ownerId == rs.humanId ? OWN_C : ENEMY_C);
            g.fillRect(r.x + (int) (b.tileX * sx), r.y + (int) (b.tileY * sy),
                    Math.max(2, (int) (b.w * sx)), Math.max(2, (int) (b.h * sy)));
        }
        // units
        for (int i = 0; i < rs.units.size(); i++) {
            RenderState.RUnit u = rs.units.get(i);
            g.setColor(u.ownerId == rs.humanId ? OWN_C : ENEMY_C);
            g.fillRect(r.x + (int) (u.x * sx), r.y + (int) (u.y * sy), 2, 2);
        }
        // viewport rectangle
        int vx0 = (int) (ctx.camera.screenToWorldX(0) * sx);
        int vy0 = (int) (ctx.camera.screenToWorldY(0) * sy);
        int vx1 = (int) (ctx.camera.screenToWorldX(panel.getWidth()) * sx);
        int vy1 = (int) (ctx.camera.screenToWorldY(panel.getHeight()) * sy);
        g.setColor(Color.WHITE);
        g.drawRect(r.x + vx0, r.y + vy0, Math.max(1, vx1 - vx0), Math.max(1, vy1 - vy0));
    }

    private void drawPortrait(Graphics2D g, RenderState rs, Rectangle r) {
        g.setFont(fSmall);
        if (rs == null) return;
        // building selected?
        if (ctx.selectedBuilding >= 0) {
            RenderState.RBuilding b = findBuilding(rs, ctx.selectedBuilding);
            if (b != null) {
                Sprites.draw(g, Sprites.forBuilding(b.type), b.type.baseColor(),
                        r.x + 6, r.y + 6, r.width - 28);
                g.setColor(TEXT);
                g.drawString(b.type.displayName(), r.x + 4, r.y + r.height - 14);
                g.drawString("HP " + b.hp + "/" + b.maxHp, r.x + 4, r.y + r.height - 3);
                return;
            }
        }
        if (!ctx.selectedUnits.isEmpty()) {
            RenderState.RUnit u = findUnit(rs, ctx.selectedUnits.iterator().next().longValue());
            if (u != null) {
                Sprites.draw(g, Sprites.forUnit(u.type), u.type.baseColor(),
                        r.x + 6, r.y + 6, r.width - 28);
                g.setColor(TEXT);
                String name = ctx.selectedUnits.size() > 1
                        ? u.type.displayName() + " x" + ctx.selectedUnits.size()
                        : u.type.displayName();
                g.drawString(name, r.x + 4, r.y + r.height - 14);
                g.drawString("HP " + u.hp + "/" + u.maxHp, r.x + 4, r.y + r.height - 3);
                return;
            }
        }
        g.setColor(new Color(120, 130, 150));
        g.drawString("No selection", r.x + 6, r.y + r.height / 2);
    }

    // ---- contextual buttons ------------------------------------------------

    private void layoutButtons(RenderState rs, int areaX, int areaY, int areaW, int areaH) {
        buttons.clear();
        if (rs == null) return;
        int cols = 4;
        int rows = 3;
        int gap = 4;
        int bw = (areaW - gap * (cols - 1)) / cols;
        int bh = (areaH - gap * (rows - 1)) / rows;
        int idx = 0;

        // production building selected -> train menu
        if (ctx.selectedBuilding >= 0) {
            RenderState.RBuilding b = findBuilding(rs, ctx.selectedBuilding);
            if (b != null) {
                List<UnitType> trainable = TechTree.producedBy(b.type);
                for (int i = 0; i < trainable.size(); i++) {
                    final UnitType ut = trainable.get(i);
                    final long bid = b.id;
                    addBtn(idx++, areaX, areaY, bw, bh, gap, cols,
                            shortName(ut.displayName()), new Runnable() {
                        public void run() { ctx.enqueue(Commands.train(bid, ut)); }
                    });
                }
                return;
            }
        }

        if (ctx.selectedUnits.isEmpty()) return;

        long workerId = firstWorker(rs);
        Race race = rs.humanRace;

        if (workerId >= 0 && buildMenuOpen && race != null) {
            List<BuildingType> opts = TechTree.buildableBy(race);
            for (int i = 0; i < opts.size(); i++) {
                final BuildingType bt = opts.get(i);
                final long wid = workerId;
                addBtn(idx++, areaX, areaY, bw, bh, gap, cols,
                        shortName(bt.displayName()), new Runnable() {
                    public void run() {
                        ctx.beginPlacement(bt, wid);
                        buildMenuOpen = false;
                    }
                });
            }
            addBtn(idx++, areaX, areaY, bw, bh, gap, cols, "Cancel", new Runnable() {
                public void run() { buildMenuOpen = false; ctx.cancelMode(); }
            });
            return;
        }

        // default unit command card
        addBtn(idx++, areaX, areaY, bw, bh, gap, cols, "Move", new Runnable() {
            public void run() { ctx.cancelMode(); }
        });
        addBtn(idx++, areaX, areaY, bw, bh, gap, cols, "Attack(A)", new Runnable() {
            public void run() { panel.hotkeyAttackMove(); }
        });
        addBtn(idx++, areaX, areaY, bw, bh, gap, cols, "Stop(S)", new Runnable() {
            public void run() { panel.hotkeyStop(); }
        });
        if (workerId >= 0) {
            addBtn(idx++, areaX, areaY, bw, bh, gap, cols, "Build(B)", new Runnable() {
                public void run() { buildMenuOpen = true; }
            });
        }
    }

    private void addBtn(int idx, int areaX, int areaY, int bw, int bh, int gap, int cols,
                        String label, Runnable action) {
        int col = idx % cols;
        int row = idx / cols;
        Rectangle r = new Rectangle(areaX + col * (bw + gap), areaY + row * (bh + gap), bw, bh);
        buttons.add(new Btn(r, label, action));
    }

    private void drawButtons(Graphics2D g) {
        g.setFont(fSmall);
        for (int i = 0; i < buttons.size(); i++) {
            Btn b = buttons.get(i);
            g.setColor(b.enabled ? BTN : new Color(36, 38, 46));
            g.fillRect(b.rect.x, b.rect.y, b.rect.width, b.rect.height);
            g.setColor(BORDER);
            g.drawRect(b.rect.x, b.rect.y, b.rect.width - 1, b.rect.height - 1);
            g.setColor(b.enabled ? TEXT : new Color(110, 110, 120));
            int tw = g.getFontMetrics().stringWidth(b.label);
            g.drawString(b.label, b.rect.x + Math.max(2, (b.rect.width - tw) / 2),
                    b.rect.y + b.rect.height / 2 + 4);
        }
    }

    // ---- public hooks ------------------------------------------------------

    /** Called by the B hotkey: open the build menu when a worker is selected. */
    void openBuildMenu() {
        RenderState rs = ctx.render;
        if (rs != null && firstWorker(rs) >= 0) {
            buildMenuOpen = true;
            repaint();
        }
    }

    void resetMenus() {
        buildMenuOpen = false;
    }

    // ---- lookups -----------------------------------------------------------

    private long firstWorker(RenderState rs) {
        for (java.util.Iterator<Long> it = ctx.selectedUnits.iterator(); it.hasNext();) {
            long id = it.next().longValue();
            RenderState.RUnit u = findUnit(rs, id);
            if (u != null && u.type.isWorker()) return id;
        }
        return -1;
    }

    private RenderState.RUnit findUnit(RenderState rs, long id) {
        for (int i = 0; i < rs.units.size(); i++) {
            if (rs.units.get(i).id == id) return rs.units.get(i);
        }
        return null;
    }

    private RenderState.RBuilding findBuilding(RenderState rs, long id) {
        for (int i = 0; i < rs.buildings.size(); i++) {
            if (rs.buildings.get(i).id == id) return rs.buildings.get(i);
        }
        return null;
    }

    private static String shortName(String s) {
        return s.length() <= 9 ? s : s.substring(0, 9);
    }
}

package com.whim.starcraft8.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.Terrain;
import com.whim.starcraft8.engine.Commands;

/**
 * The world viewport. Pure presentation: it paints the latest {@link RenderState}
 * snapshot with {@code Graphics2D} in a chunky 8-bit style and translates mouse input
 * into {@code Commands.*} pushed through {@link UiContext#enqueue}. It never mutates domain.
 */
final class GamePanel extends JPanel {

    private static final Color C_GROUND = new Color(40, 56, 40);
    private static final Color C_GROUND_ALT = new Color(48, 64, 46);
    private static final Color C_UNBUILD = new Color(28, 30, 36);
    private static final Color C_MINERAL = new Color(90, 170, 235);
    private static final Color C_MINERAL_D = new Color(40, 90, 150);
    private static final Color C_GEYSER = new Color(80, 220, 140);
    private static final Color C_GEYSER_D = new Color(30, 110, 70);
    private static final Color C_SELECT = new Color(80, 255, 120);
    private static final Color C_ENEMY = new Color(235, 70, 70);
    private static final Color C_OWN = new Color(90, 200, 255);

    private final UiContext ctx;

    // drag-select state (screen px)
    private boolean dragging = false;
    private int dragStartX, dragStartY, dragCurX, dragCurY;

    GamePanel(UiContext ctx) {
        this.ctx = ctx;
        setBackground(Color.BLACK);
        setFocusable(true);
        MouseHandler mh = new MouseHandler();
        addMouseListener(mh);
        addMouseMotionListener(mh);
    }

    // ---- Painting ----------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        // hard 8-bit edges: no anti-aliasing, nearest-neighbour feel
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        ctx.camera.setViewport(getWidth(), getHeight());

        RenderState rs = ctx.render;
        if (rs == null) return;
        ctx.camera.setMap(rs.mapW, rs.mapH);

        drawTerrain(g, rs);
        drawBuildings(g, rs);
        drawRallyLines(g, rs);
        drawUnits(g, rs);
        drawProjectiles(g, rs);
        drawPlacementPreview(g, rs);
        drawSelectionBox(g);
        drawWinner(g, rs);
    }

    private void drawTerrain(Graphics2D g, RenderState rs) {
        int tile = ctx.camera.tilePx();
        Camera cam = ctx.camera;
        // only iterate visible tiles
        int x0 = (int) Math.floor(cam.screenToWorldX(0));
        int y0 = (int) Math.floor(cam.screenToWorldY(0));
        int x1 = (int) Math.ceil(cam.screenToWorldX(getWidth()));
        int y1 = (int) Math.ceil(cam.screenToWorldY(getHeight()));
        if (x0 < 0) x0 = 0;
        if (y0 < 0) y0 = 0;
        if (x1 > rs.mapW) x1 = rs.mapW;
        if (y1 > rs.mapH) y1 = rs.mapH;

        for (int tx = x0; tx < x1; tx++) {
            for (int ty = y0; ty < y1; ty++) {
                int sx = cam.worldToScreenX(tx);
                int sy = cam.worldToScreenY(ty);
                Terrain t = rs.terrain[tx][ty];
                if (t == Terrain.MINERAL_FIELD) {
                    drawResourceTile(g, sx, sy, tile, C_MINERAL, C_MINERAL_D,
                            rs.resource[tx][ty]);
                } else if (t == Terrain.GEYSER) {
                    drawResourceTile(g, sx, sy, tile, C_GEYSER, C_GEYSER_D,
                            rs.resource[tx][ty]);
                } else if (t == Terrain.UNBUILDABLE) {
                    g.setColor(C_UNBUILD);
                    g.fillRect(sx, sy, tile, tile);
                } else {
                    g.setColor(((tx + ty) & 1) == 0 ? C_GROUND : C_GROUND_ALT);
                    g.fillRect(sx, sy, tile, tile);
                }
            }
        }
    }

    private void drawResourceTile(Graphics2D g, int sx, int sy, int tile,
                                  Color hi, Color lo, int amount) {
        g.setColor(lo);
        g.fillRect(sx, sy, tile, tile);
        // chunky crystal block whose height encodes remaining amount
        int inset = Math.max(1, tile / 8);
        int full = tile - inset * 2;
        int hgt = amount <= 0 ? full / 4 : Math.max(full / 4, Math.min(full, full * amount / 1500));
        g.setColor(hi);
        g.fillRect(sx + inset, sy + (tile - inset - hgt), full, hgt);
        g.setColor(Sprites.lighter(hi));
        g.fillRect(sx + inset, sy + (tile - inset - hgt), Math.max(1, full / 3), Math.max(1, hgt / 3));
    }

    private void drawBuildings(Graphics2D g, RenderState rs) {
        int tile = ctx.camera.tilePx();
        for (int i = 0; i < rs.buildings.size(); i++) {
            RenderState.RBuilding b = rs.buildings.get(i);
            int sx = ctx.camera.worldToScreenX(b.tileX);
            int sy = ctx.camera.worldToScreenY(b.tileY);
            int w = b.w * tile;
            int h = b.h * tile;
            Color base = b.type.baseColor();
            if (b.underConstruction) base = base.darker().darker();
            Sprites.draw(g, Sprites.forBuilding(b.type), base, sx, sy, Math.min(w, h));
            // fill footprint frame so multi-tile buildings read clearly
            g.setColor(Sprites.darker(base));
            g.drawRect(sx, sy, w - 1, h - 1);

            boolean own = b.ownerId == rs.humanId;
            if (b.id == ctx.selectedBuilding) {
                g.setColor(C_SELECT);
                g.setStroke(new BasicStroke(2f));
                g.drawRect(sx - 2, sy - 2, w + 3, h + 3);
                g.setStroke(new BasicStroke(1f));
            }
            drawBar(g, sx, sy - 5, w, b.hp, b.maxHp, 0, 0,
                    own ? C_OWN : C_ENEMY);
            if (b.producing) {
                g.setColor(Color.YELLOW);
                g.fillRect(sx + w - 5, sy + 2, 3, 3);
            }
        }
    }

    private void drawRallyLines(Graphics2D g, RenderState rs) {
        int tile = ctx.camera.tilePx();
        for (int i = 0; i < rs.buildings.size(); i++) {
            RenderState.RBuilding b = rs.buildings.get(i);
            if (b.ownerId != rs.humanId || !b.hasRally) continue;
            if (b.id != ctx.selectedBuilding) continue;
            int sx = ctx.camera.worldToScreenX(b.tileX + b.w / 2.0);
            int sy = ctx.camera.worldToScreenY(b.tileY + b.h / 2.0);
            int rx = ctx.camera.worldToScreenX(b.rallyX);
            int ry = ctx.camera.worldToScreenY(b.rallyY);
            g.setColor(new Color(255, 235, 90, 180));
            g.drawLine(sx, sy, rx, ry);
            g.fillRect(rx - 2, ry - 2, 5, 5);
        }
    }

    private void drawUnits(Graphics2D g, RenderState rs) {
        int tile = ctx.camera.tilePx();
        int size = tile; // one tile per unit sprite
        for (int i = 0; i < rs.units.size(); i++) {
            RenderState.RUnit u = rs.units.get(i);
            int sx = ctx.camera.worldToScreenX(u.x) - size / 2;
            int sy = ctx.camera.worldToScreenY(u.y) - size / 2;

            boolean own = u.ownerId == rs.humanId;
            boolean selected = own && ctx.selectedUnits.contains(Long.valueOf(u.id));
            if (selected) {
                g.setColor(C_SELECT);
                g.drawOval(sx - 1, sy + size - 5, size + 1, 6);
            }
            if (u.flyer) {
                // soft shadow under flyers
                g.setColor(new Color(0, 0, 0, 90));
                g.fillOval(sx + 2, sy + size - 3, size - 4, 4);
            }
            Sprites.draw(g, Sprites.forUnit(u.type), u.type.baseColor(), sx, sy, size);
            // owner pip
            g.setColor(own ? C_OWN : C_ENEMY);
            g.fillRect(sx, sy, 3, 3);

            drawBar(g, sx, sy - 4, size, u.hp, u.maxHp, u.shield, u.maxShield,
                    own ? C_OWN : C_ENEMY);
        }
    }

    /** HP bar (with optional shield segment above it). */
    private void drawBar(Graphics2D g, int x, int y, int w, int hp, int maxHp,
                         int shield, int maxShield, Color ownerTint) {
        if (maxHp <= 0) return;
        int barW = Math.max(8, w);
        g.setColor(Color.BLACK);
        g.fillRect(x, y, barW, 3);
        int hpW = (int) Math.round(barW * Math.max(0, Math.min(1.0, hp / (double) maxHp)));
        Color hpColor = hp > maxHp * 0.5 ? new Color(70, 220, 70)
                : hp > maxHp * 0.25 ? new Color(230, 210, 60) : new Color(230, 70, 60);
        g.setColor(hpColor);
        g.fillRect(x, y, hpW, 3);
        if (maxShield > 0) {
            g.setColor(Color.BLACK);
            g.fillRect(x, y - 3, barW, 2);
            int shW = (int) Math.round(barW * Math.max(0, Math.min(1.0, shield / (double) maxShield)));
            g.setColor(new Color(120, 170, 255));
            g.fillRect(x, y - 3, shW, 2);
        }
    }

    private void drawProjectiles(Graphics2D g, RenderState rs) {
        for (int i = 0; i < rs.projectiles.size(); i++) {
            RenderState.RProjectile p = rs.projectiles.get(i);
            int sx = ctx.camera.worldToScreenX(p.x);
            int sy = ctx.camera.worldToScreenY(p.y);
            g.setColor(p.color != null ? p.color : Color.WHITE);
            g.fillRect(sx - 2, sy - 2, 4, 4);
            g.setColor(Color.WHITE);
            g.fillRect(sx - 1, sy - 1, 2, 2);
        }
    }

    private void drawPlacementPreview(Graphics2D g, RenderState rs) {
        if (ctx.mode != UiContext.MODE_PLACING || ctx.placingType == null) return;
        if (ctx.hoverTileX < 0) return;
        BuildingType t = ctx.placingType;
        int tile = ctx.camera.tilePx();
        int sx = ctx.camera.worldToScreenX(ctx.hoverTileX);
        int sy = ctx.camera.worldToScreenY(ctx.hoverTileY);
        int w = t.widthTiles() * tile;
        int h = t.heightTiles() * tile;
        boolean ok = footprintBuildable(rs, ctx.hoverTileX, ctx.hoverTileY,
                t.widthTiles(), t.heightTiles());
        g.setColor(ok ? new Color(80, 255, 120, 90) : new Color(255, 70, 70, 90));
        g.fillRect(sx, sy, w, h);
        g.setColor(ok ? C_SELECT : C_ENEMY);
        g.drawRect(sx, sy, w - 1, h - 1);
    }

    private boolean footprintBuildable(RenderState rs, int tx, int ty, int w, int h) {
        for (int x = tx; x < tx + w; x++) {
            for (int y = ty; y < ty + h; y++) {
                if (x < 0 || y < 0 || x >= rs.mapW || y >= rs.mapH) return false;
                if (rs.terrain[x][y] != Terrain.GROUND) return false;
            }
        }
        return true;
    }

    private void drawSelectionBox(Graphics2D g) {
        if (!dragging) return;
        int x = Math.min(dragStartX, dragCurX);
        int y = Math.min(dragStartY, dragCurY);
        int w = Math.abs(dragCurX - dragStartX);
        int h = Math.abs(dragCurY - dragStartY);
        g.setColor(new Color(80, 255, 120, 40));
        g.fillRect(x, y, w, h);
        g.setColor(C_SELECT);
        g.drawRect(x, y, w, h);
    }

    private void drawWinner(Graphics2D g, RenderState rs) {
        if (rs.winnerId < 0) return;
        String msg = rs.winnerId == rs.humanId ? "VICTORY" : "DEFEAT";
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, getHeight() / 2 - 30, getWidth(), 60);
        g.setColor(rs.winnerId == rs.humanId ? C_SELECT : C_ENEMY);
        g.setFont(getFont().deriveFont(40f));
        int sw = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, (getWidth() - sw) / 2, getHeight() / 2 + 12);
    }

    // ---- Input -------------------------------------------------------------

    private final class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            RenderState rs = ctx.render;
            if (rs == null) return;
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (ctx.mode == UiContext.MODE_PLACING) {
                    placeBuilding(rs, e.getX(), e.getY());
                    return;
                }
                if (ctx.mode == UiContext.MODE_ATTACK_MOVE) {
                    issueAttackMove(rs, e.getX(), e.getY());
                    ctx.cancelMode();
                    return;
                }
                dragging = true;
                dragStartX = dragCurX = e.getX();
                dragStartY = dragCurY = e.getY();
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                rightClickOrder(rs, e.getX(), e.getY());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (dragging) {
                dragCurX = e.getX();
                dragCurY = e.getY();
                repaint();
            }
            updateHover(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            updateHover(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!dragging || e.getButton() != MouseEvent.BUTTON1) return;
            dragging = false;
            RenderState rs = ctx.render;
            if (rs == null) return;
            int dx = Math.abs(dragCurX - dragStartX);
            int dy = Math.abs(dragCurY - dragStartY);
            if (dx < 4 && dy < 4) {
                clickSelect(rs, e.getX(), e.getY());
            } else {
                boxSelect(rs);
            }
            repaint();
        }
    }

    private void updateHover(MouseEvent e) {
        RenderState rs = ctx.render;
        if (rs == null) return;
        ctx.hoverTileX = (int) Math.floor(ctx.camera.screenToWorldX(e.getX()));
        ctx.hoverTileY = (int) Math.floor(ctx.camera.screenToWorldY(e.getY()));
        if (ctx.mode == UiContext.MODE_PLACING) repaint();
    }

    private void clickSelect(RenderState rs, int px, int py) {
        double wx = ctx.camera.screenToWorldX(px);
        double wy = ctx.camera.screenToWorldY(py);
        // prefer a unit
        RenderState.RUnit best = null;
        double bestD = 0.8 * 0.8;
        for (int i = 0; i < rs.units.size(); i++) {
            RenderState.RUnit u = rs.units.get(i);
            double d = (u.x - wx) * (u.x - wx) + (u.y - wy) * (u.y - wy);
            if (d <= bestD) {
                bestD = d;
                best = u;
            }
        }
        ctx.clearSelection();
        if (best != null && best.ownerId == rs.humanId) {
            ctx.selectedUnits.add(Long.valueOf(best.id));
            return;
        }
        // else try a building (tile hit)
        int tx = (int) Math.floor(wx);
        int ty = (int) Math.floor(wy);
        for (int i = 0; i < rs.buildings.size(); i++) {
            RenderState.RBuilding b = rs.buildings.get(i);
            if (tx >= b.tileX && tx < b.tileX + b.w && ty >= b.tileY && ty < b.tileY + b.h) {
                if (b.ownerId == rs.humanId) ctx.selectedBuilding = b.id;
                return;
            }
        }
    }

    private void boxSelect(RenderState rs) {
        double x0 = ctx.camera.screenToWorldX(Math.min(dragStartX, dragCurX));
        double y0 = ctx.camera.screenToWorldY(Math.min(dragStartY, dragCurY));
        double x1 = ctx.camera.screenToWorldX(Math.max(dragStartX, dragCurX));
        double y1 = ctx.camera.screenToWorldY(Math.max(dragStartY, dragCurY));
        ctx.clearSelection();
        for (int i = 0; i < rs.units.size(); i++) {
            RenderState.RUnit u = rs.units.get(i);
            if (u.ownerId != rs.humanId) continue;
            if (u.x >= x0 && u.x <= x1 && u.y >= y0 && u.y <= y1) {
                ctx.selectedUnits.add(Long.valueOf(u.id));
            }
        }
    }

    private List<Long> selectionList() {
        return new ArrayList<Long>(ctx.selectedUnits);
    }

    private void rightClickOrder(RenderState rs, int px, int py) {
        double wx = ctx.camera.screenToWorldX(px);
        double wy = ctx.camera.screenToWorldY(py);

        // a selected building with no units -> set rally point
        if (ctx.selectedUnits.isEmpty() && ctx.selectedBuilding >= 0) {
            ctx.enqueue(Commands.setRally(ctx.selectedBuilding, wx, wy));
            return;
        }
        if (ctx.selectedUnits.isEmpty()) return;

        // enemy under cursor -> attack
        long enemyId = enemyAt(rs, wx, wy);
        if (enemyId >= 0) {
            ctx.enqueue(Commands.attackTarget(selectionList(), enemyId));
            return;
        }
        // resource field under cursor -> gather
        int tx = (int) Math.floor(wx);
        int ty = (int) Math.floor(wy);
        if (tx >= 0 && ty >= 0 && tx < rs.mapW && ty < rs.mapH) {
            Terrain t = rs.terrain[tx][ty];
            if (t == Terrain.MINERAL_FIELD || t == Terrain.GEYSER) {
                long fieldId = -(((long) ty) * rs.mapW + tx) - 2;
                ctx.enqueue(Commands.gather(selectionList(), fieldId));
                return;
            }
        }
        // otherwise move
        ctx.enqueue(Commands.move(selectionList(), wx, wy));
    }

    private void issueAttackMove(RenderState rs, int px, int py) {
        if (ctx.selectedUnits.isEmpty()) return;
        double wx = ctx.camera.screenToWorldX(px);
        double wy = ctx.camera.screenToWorldY(py);
        ctx.enqueue(Commands.attackMove(selectionList(), wx, wy));
    }

    private long enemyAt(RenderState rs, double wx, double wy) {
        double bestD = 0.8 * 0.8;
        long best = -1;
        for (int i = 0; i < rs.units.size(); i++) {
            RenderState.RUnit u = rs.units.get(i);
            if (u.ownerId == rs.humanId) continue;
            double d = (u.x - wx) * (u.x - wx) + (u.y - wy) * (u.y - wy);
            if (d <= bestD) {
                bestD = d;
                best = u.id;
            }
        }
        if (best >= 0) return best;
        int tx = (int) Math.floor(wx);
        int ty = (int) Math.floor(wy);
        for (int i = 0; i < rs.buildings.size(); i++) {
            RenderState.RBuilding b = rs.buildings.get(i);
            if (b.ownerId == rs.humanId) continue;
            if (tx >= b.tileX && tx < b.tileX + b.w && ty >= b.tileY && ty < b.tileY + b.h) {
                return b.id;
            }
        }
        return -1;
    }

    private void placeBuilding(RenderState rs, int px, int py) {
        int tx = (int) Math.floor(ctx.camera.screenToWorldX(px));
        int ty = (int) Math.floor(ctx.camera.screenToWorldY(py));
        BuildingType t = ctx.placingType;
        long worker = ctx.placingWorker;
        if (t != null && worker >= 0
                && footprintBuildable(rs, tx, ty, t.widthTiles(), t.heightTiles())) {
            ctx.enqueue(Commands.build(worker, t, tx, ty));
            ctx.cancelMode();
            repaint();
        }
        // invalid placement: stay in placing mode so the player can retry
    }

    // ---- keyboard hooks (invoked by GameFrame) -----------------------------

    void hotkeyStop() {
        if (!ctx.selectedUnits.isEmpty()) {
            ctx.enqueue(Commands.stop(selectionList()));
        }
    }

    void hotkeyAttackMove() {
        if (!ctx.selectedUnits.isEmpty()) {
            ctx.mode = UiContext.MODE_ATTACK_MOVE;
        }
    }

    void hotkeyCancel() {
        ctx.cancelMode();
        repaint();
    }
}

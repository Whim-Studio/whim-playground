package com.whim.alganon.ui.render;

import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.Views.CharacterView;
import com.whim.alganon.api.Views.GatherView;
import com.whim.alganon.api.Views.MobView;
import com.whim.alganon.api.Views.NpcView;
import com.whim.alganon.api.Views.PortalView;
import com.whim.alganon.api.Views.WorldView;
import com.whim.alganon.ui.UiTheme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Top-down tile renderer. Draws the visible slice of the zone centered on the player, then the
 * placed entities (nodes, portals, npcs, mobs, player) and lightweight combat feedback. Pure
 * Graphics2D; every entity is drawn by {@link Sprites} from its {@code spriteKey}.
 */
public final class WorldRenderer {

    /** Camera-follow tile size in px. Chosen so a HUD-sized world panel shows ~15-20 tiles. */
    public static final int TILE = 40;

    private final int tile;

    /** Screen-space rect of the last-rendered player-selectable tiles, for click hit-testing. */
    private int originX, originY, camX, camY;

    public WorldRenderer() { this(TILE); }
    public WorldRenderer(int tile) { this.tile = tile; }

    public int tile() { return tile; }

    /** Convert a screen pixel to a world tile using the last render's camera. Null if off-map. */
    public GridPos screenToTile(WorldView world, int px, int py, int panelW, int panelH) {
        if (world == null) return null;
        computeCamera(world, panelW, panelH);
        int tx = camX + (px - originX) / tile;
        int ty = camY + (py - originY) / tile;
        if (tx < 0 || ty < 0 || tx >= world.width() || ty >= world.height()) return null;
        return new GridPos(tx, ty);
    }

    private void computeCamera(WorldView world, int panelW, int panelH) {
        int cols = Math.max(1, panelW / tile);
        int rows = Math.max(1, panelH / tile);
        GridPos c = playerAnchor(world);
        camX = clamp(c.x - cols / 2, 0, Math.max(0, world.width() - cols));
        camY = clamp(c.y - rows / 2, 0, Math.max(0, world.height() - rows));
        originX = (panelW - cols * tile) / 2;
        originY = (panelH - rows * tile) / 2;
    }

    private GridPos playerAnchor(WorldView world) {
        return new GridPos(world.width() / 2, world.height() / 2);
    }

    /**
     * Render the world. {@code selected} is the tile the player last clicked (may be null) and
     * {@code player} supplies the player's own position/sprite (world views don't carry it).
     */
    public void render(Graphics2D g, WorldView world, CharacterView player, GridPos selected,
                       int panelW, int panelH, long animMs) {
        UiTheme.aa(g);
        g.setColor(UiTheme.tileColor(null));
        g.fillRect(0, 0, panelW, panelH);
        if (world == null) return;

        int cols = Math.max(1, panelW / tile);
        int rows = Math.max(1, panelH / tile);
        // Player position drives the camera when available.
        GridPos ppos = player != null && player.pos() != null ? player.pos()
                : new GridPos(world.width() / 2, world.height() / 2);
        camX = clamp(ppos.x - cols / 2, 0, Math.max(0, world.width() - cols));
        camY = clamp(ppos.y - rows / 2, 0, Math.max(0, world.height() - rows));
        originX = (panelW - cols * tile) / 2;
        originY = (panelH - rows * tile) / 2;

        // --- tiles ---
        for (int ry = 0; ry <= rows; ry++) {
            for (int rx = 0; rx <= cols; rx++) {
                int wx = camX + rx, wy = camY + ry;
                if (wx < 0 || wy < 0 || wx >= world.width() || wy >= world.height()) continue;
                int sx = originX + rx * tile, sy = originY + ry * tile;
                Color base = UiTheme.tileColor(world.tileAt(wx, wy));
                g.setColor(((wx + wy) & 1) == 0 ? base : base.darker());
                g.fillRect(sx, sy, tile, tile);
                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(sx, sy, tile, tile);
            }
        }

        // --- selection highlight ---
        if (selected != null) {
            Rectangle r = tileRect(selected);
            if (r != null) {
                g.setColor(new Color(0xC9, 0xA8, 0x5A, 90));
                g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(UiTheme.ACCENT_HOT);
                g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            }
        }

        int r = tile / 2 - 4;

        // --- gather nodes ---
        for (GatherView n : world.gatherNodes()) {
            Rectangle rc = tileRect(n.pos());
            if (rc == null) continue;
            if (n.depleted()) {
                Graphics2D gg = (Graphics2D) g.create();
                gg.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.35f));
                Sprites.draw(gg, "node." + n.name(), rc.x + tile / 2, rc.y + tile / 2, r);
                gg.dispose();
            } else {
                Sprites.draw(g, "node." + n.name(), rc.x + tile / 2, rc.y + tile / 2, r);
            }
        }

        // --- portals ---
        for (PortalView p : world.portals()) {
            Rectangle rc = tileRect(p.pos());
            if (rc == null) continue;
            Sprites.draw(g, "portal", rc.x + tile / 2, rc.y + tile / 2, r);
            label(g, p.label(), rc.x + tile / 2, rc.y - 2, UiTheme.XP);
        }

        // --- npcs ---
        for (NpcView n : world.npcs()) {
            Rectangle rc = tileRect(n.pos());
            if (rc == null) continue;
            String key = n.spriteKey() != null ? n.spriteKey() : (n.questGiver() ? "npc.giver" : "npc");
            Sprites.draw(g, key, rc.x + tile / 2, rc.y + tile / 2, r);
            label(g, n.name(), rc.x + tile / 2, rc.y - 2, n.vendor() ? UiTheme.ACCENT : UiTheme.TEXT_DIM);
        }

        // --- mobs ---
        for (MobView m : world.mobs()) {
            Rectangle rc = tileRect(m.pos());
            if (rc == null) continue;
            String key = m.spriteKey() != null ? m.spriteKey() : "mob";
            Sprites.draw(g, key, rc.x + tile / 2, rc.y + tile / 2, r);
            // mob hp pip
            if (m.maxHp() > 0 && m.hp() < m.maxHp()) {
                int bw = tile - 12;
                UiTheme.bar(g, rc.x + 6, rc.y + 2, bw, 4, m.hp() / (double) m.maxHp(), UiTheme.HP, UiTheme.HP_BG);
            }
            if (m.inCombat()) {
                g.setColor(new Color(0xE0, 0x50, 0x50, (int) (120 + 100 * Math.abs(Math.sin(animMs / 200.0)))));
                g.drawRect(rc.x + 2, rc.y + 2, tile - 4, tile - 4);
            }
            label(g, "Lv" + m.level() + " " + m.name(), rc.x + tile / 2, rc.y + tile + 11, UiTheme.BAD);
        }

        // --- player ---
        if (player != null && player.pos() != null) {
            Rectangle rc = tileRect(player.pos());
            if (rc != null) {
                Sprites.draw(g, "player." + player.className(), rc.x + tile / 2, rc.y + tile / 2, r);
                g.setColor(UiTheme.ACCENT);
                g.drawOval(rc.x + 4, rc.y + 4, tile - 8, tile - 8);
            }
        }

        // --- edge vignette so the finite map feels framed ---
        g.setColor(new Color(0, 0, 0, 60));
        g.drawRect(originX, originY, cols * tile - 1, rows * tile - 1);
    }

    private Rectangle tileRect(GridPos p) {
        if (p == null) return null;
        int rx = p.x - camX, ry = p.y - camY;
        return new Rectangle(originX + rx * tile, originY + ry * tile, tile, tile);
    }

    private void label(Graphics2D g, String s, int cx, int y, Color c) {
        if (s == null || s.isEmpty()) return;
        g.setFont(UiTheme.FONT_SMALL);
        int w = g.getFontMetrics().stringWidth(s);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(cx - w / 2 - 3, y - 11, w + 6, 14, 6, 6);
        g.setColor(c);
        g.drawString(s, cx - w / 2, y);
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
}

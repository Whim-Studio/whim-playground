package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Ids;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Tiny component that paints a single ship or defense or resource glyph. */
public final class GlyphIcon extends JComponent {

    private Ids.ShipType ship;
    private Ids.DefenseType defense;
    private Ids.ResourceType resource;
    private Color color = Palette.ACCENT;

    public GlyphIcon(int size) {
        setPreferredSize(new Dimension(size, size));
        setOpaque(false);
    }

    public GlyphIcon ship(Ids.ShipType t, Color c) {
        this.ship = t; this.defense = null; this.resource = null; this.color = c; repaint(); return this;
    }

    public GlyphIcon defense(Ids.DefenseType t, Color c) {
        this.defense = t; this.ship = null; this.resource = null; this.color = c; repaint(); return this;
    }

    public GlyphIcon resource(Ids.ResourceType t) {
        this.resource = t; this.ship = null; this.defense = null; repaint(); return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int s = Math.min(getWidth(), getHeight());
        int x = (getWidth() - s) / 2, y = (getHeight() - s) / 2;
        if (ship != null) SpaceArt.shipGlyph(g2, ship, x, y, s, color);
        else if (defense != null) SpaceArt.defenseGlyph(g2, defense, x, y, s, color);
        else if (resource != null) SpaceArt.resourceIcon(g2, resource, x, y, s);
        g2.dispose();
    }
}

package com.whim.oggalaxy.ui;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * A JPanel that paints a seeded starfield behind its children. Subclasses / users set
 * it opaque=false on child components so the field shows through. The seed is fixed per
 * panel instance so the field doesn't shimmer on repaint.
 */
public class StarPanel extends JPanel {

    private final long seed;

    public StarPanel(long seed) {
        this.seed = seed;
        setOpaque(true);
        setBackground(Palette.BG_DEEP);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        SpaceArt.starfield(g2, getWidth(), getHeight(), seed);
        g2.dispose();
    }
}

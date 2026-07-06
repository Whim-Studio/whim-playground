package com.whim.starcommand.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * A cheap, deterministic parallax starfield drawn entirely with Java2D — no
 * external image assets. Reused as the background of every screen.
 */
public class Starfield {

    private final int[] xs;
    private final int[] ys;
    private final int[] size;
    private final int width;
    private final int height;

    public Starfield(int width, int height, int count, long seed) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        Random r = new Random(seed);
        xs = new int[count];
        ys = new int[count];
        size = new int[count];
        for (int i = 0; i < count; i++) {
            xs[i] = r.nextInt(this.width);
            ys[i] = r.nextInt(this.height);
            size[i] = 1 + r.nextInt(3);
        }
    }

    public void paint(Graphics2D g, int w, int h) {
        g.setColor(Palette.SPACE);
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < xs.length; i++) {
            int x = (int) (((long) xs[i] * w) / width);
            int y = (int) (((long) ys[i] * h) / height);
            int s = size[i];
            int bright = 90 + s * 45;
            g.setColor(new Color(bright, bright, Math.min(255, bright + 30)));
            g.fillRect(x, y, s, s);
        }
    }
}

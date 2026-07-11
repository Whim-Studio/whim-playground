package com.whim.bc3k.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/** Cheap parallax starfield backdrop shared by bridge consoles. Deterministic. */
public final class Starfield {
    private final int[] sx, sy, sz;
    private double t;

    public Starfield(int count, long seed) {
        sx = new int[count]; sy = new int[count]; sz = new int[count];
        Random r = new Random(seed);
        for (int i = 0; i < count; i++) {
            sx[i] = r.nextInt(2000);
            sy[i] = r.nextInt(1200);
            sz[i] = 1 + r.nextInt(3);
        }
    }

    public void update(double dt) { t += dt * 12; }

    public void render(Graphics2D g, int w, int h) {
        g.setColor(Palette.BG);
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < sx.length; i++) {
            int x = (int) ((sx[i] - t * sz[i]) % (w + 4) + w + 4) % (w + 4);
            int y = sy[i] % (h + 1);
            int b = 90 + sz[i] * 45;
            g.setColor(new Color(b, b, Math.min(255, b + 30)));
            g.fillRect(x, y, sz[i], sz[i]);
        }
    }
}

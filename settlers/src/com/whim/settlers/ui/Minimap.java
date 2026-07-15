package com.whim.settlers.ui;

import com.whim.settlers.engine.Camera;
import com.whim.settlers.engine.World;
import com.whim.settlers.map.TileMap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Overview minimap drawn in the bottom-right corner. Shows the whole map at a
 * fixed pixel size with the current camera viewport outlined, and supports
 * click-to-recentre. The map image is cached and only rebuilt when the map
 * instance changes, so per-frame cost is a single {@code drawImage}.
 */
public final class Minimap {

    private static final int MAX_SIZE = 200;   // longest edge in pixels
    private static final int MARGIN   = 12;
    private static final int PAD      = 4;

    private TileMap cachedFor;
    private java.awt.image.BufferedImage image;
    private int imgW, imgH;

    /** Screen rectangle occupied by the minimap panel (including its padding). */
    public Rectangle panelBounds(int viewportW, int viewportH) {
        ensureImage(null); // sizes only depend on the map once cached
        int w = imgW + PAD * 2;
        int h = imgH + PAD * 2;
        return new Rectangle(viewportW - w - MARGIN, viewportH - h - MARGIN, w, h);
    }

    public void render(Graphics2D g, World world) {
        ensureImage(world.map());
        int vw = world.camera().viewportW();
        int vh = world.camera().viewportH();
        Rectangle panel = panelBounds(vw, vh);

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(panel.x, panel.y, panel.width, panel.height);
        int ix = panel.x + PAD, iy = panel.y + PAD;
        g.drawImage(image, ix, iy, null);

        // Outline the visible viewport on the minimap.
        Camera cam = world.camera();
        TileMap map = world.map();
        double sx = imgW / (double) map.width();
        double sy = imgH / (double) map.height();
        Point2D.Double tl = cam.screenToWorld(0, 0);
        Point2D.Double br = cam.screenToWorld(vw, vh);
        int rx = ix + (int) (clamp(tl.x, 0, map.width()) * sx);
        int ry = iy + (int) (clamp(tl.y, 0, map.height()) * sy);
        int rw = (int) ((clamp(br.x, 0, map.width()) - clamp(tl.x, 0, map.width())) * sx);
        int rh = (int) ((clamp(br.y, 0, map.height()) - clamp(tl.y, 0, map.height())) * sy);
        g.setColor(Color.WHITE);
        g.drawRect(rx, ry, Math.max(1, rw), Math.max(1, rh));
    }

    /**
     * If {@code screen} lies inside the minimap image, return the world tile it
     * maps to (for click-to-recentre); otherwise {@code null}.
     */
    public Point2D.Double worldAt(int screenX, int screenY, World world) {
        ensureImage(world.map());
        int vw = world.camera().viewportW();
        int vh = world.camera().viewportH();
        Rectangle panel = panelBounds(vw, vh);
        int ix = panel.x + PAD, iy = panel.y + PAD;
        if (screenX < ix || screenY < iy || screenX >= ix + imgW || screenY >= iy + imgH) {
            return null;
        }
        TileMap map = world.map();
        double wx = (screenX - ix) / (double) imgW * map.width();
        double wy = (screenY - iy) / (double) imgH * map.height();
        return new Point2D.Double(wx, wy);
    }

    private void ensureImage(TileMap map) {
        if (map == null || map == cachedFor) return;
        double scale = MAX_SIZE / (double) Math.max(map.width(), map.height());
        imgW = Math.max(1, (int) (map.width() * scale));
        imgH = Math.max(1, (int) (map.height() * scale));
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(imgW, imgH,
                        java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int py = 0; py < imgH; py++) {
            int my = (int) (py / (double) imgH * map.height());
            for (int px = 0; px < imgW; px++) {
                int mx = (int) (px / (double) imgW * map.width());
                img.setRGB(px, py, map.get(mx, my).color().getRGB());
            }
        }
        this.image = img;
        this.cachedFor = map;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}

package com.whim.settlers.engine;

import java.awt.geom.Point2D;

/**
 * 2D camera for the tile world. Holds a world-space position (the point that
 * appears at the centre of the viewport) and a zoom factor, and converts
 * between world and screen coordinates.
 *
 * <p>World coordinates are measured in tiles; screen coordinates in device
 * pixels. One tile is {@link #TILE_SIZE} pixels wide at zoom 1.0.
 */
public final class Camera {

    /** Base edge length of a tile in pixels, before zoom. */
    public static final int TILE_SIZE = 32;

    private static final double MIN_ZOOM = 0.35;
    private static final double MAX_ZOOM = 3.0;

    /** Camera centre, in tile units. */
    private double centreX;
    private double centreY;
    private double zoom = 1.0;

    /** Current viewport size in pixels; updated each render. */
    private int viewportW = 1;
    private int viewportH = 1;

    public Camera(double centreX, double centreY) {
        this.centreX = centreX;
        this.centreY = centreY;
    }

    public void setViewport(int width, int height) {
        this.viewportW = Math.max(1, width);
        this.viewportH = Math.max(1, height);
    }

    /** Pixels per tile at the current zoom. */
    public double scale() {
        return TILE_SIZE * zoom;
    }

    /** Pan the camera by a delta expressed in screen pixels. */
    public void panPixels(double dxPixels, double dyPixels) {
        double s = scale();
        centreX -= dxPixels / s;
        centreY -= dyPixels / s;
    }

    /**
     * Zoom towards a fixed screen anchor (typically the mouse cursor) so the
     * world point under the cursor stays put — the standard "zoom to cursor"
     * behaviour players expect.
     *
     * @param steps  wheel notches; positive zooms in, negative zooms out
     * @param anchorScreenX cursor x in pixels
     * @param anchorScreenY cursor y in pixels
     */
    public void zoomAt(int steps, int anchorScreenX, int anchorScreenY) {
        Point2D.Double before = screenToWorld(anchorScreenX, anchorScreenY);
        double factor = Math.pow(1.1, steps);
        zoom = clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
        Point2D.Double after = screenToWorld(anchorScreenX, anchorScreenY);
        // Shift the centre so the anchor world-point maps back to the cursor.
        centreX += before.x - after.x;
        centreY += before.y - after.y;
    }

    /** Convert a world (tile) coordinate to a screen pixel coordinate. */
    public Point2D.Double worldToScreen(double worldX, double worldY) {
        double s = scale();
        double sx = (worldX - centreX) * s + viewportW / 2.0;
        double sy = (worldY - centreY) * s + viewportH / 2.0;
        return new Point2D.Double(sx, sy);
    }

    /** Convert a screen pixel coordinate to a world (tile) coordinate. */
    public Point2D.Double screenToWorld(double screenX, double screenY) {
        double s = scale();
        double wx = (screenX - viewportW / 2.0) / s + centreX;
        double wy = (screenY - viewportH / 2.0) / s + centreY;
        return new Point2D.Double(wx, wy);
    }

    /** Clamp the camera centre so it cannot drift far off a map of the given size. */
    public void clampTo(int mapWidth, int mapHeight) {
        centreX = clamp(centreX, -2, mapWidth + 2);
        centreY = clamp(centreY, -2, mapHeight + 2);
    }

    public double centreX() { return centreX; }
    public double centreY() { return centreY; }
    public double zoom()    { return zoom; }
    public int viewportW()  { return viewportW; }
    public int viewportH()  { return viewportH; }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
